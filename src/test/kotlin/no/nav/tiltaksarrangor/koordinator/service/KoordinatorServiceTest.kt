package no.nav.tiltaksarrangor.koordinator.service

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import no.nav.tiltaksarrangor.client.amtarrangor.AmtArrangorClient
import no.nav.tiltaksarrangor.client.amtarrangor.dto.VeilederAnsatt
import no.nav.tiltaksarrangor.ingest.model.AnsattRolle
import no.nav.tiltaksarrangor.koordinator.model.LeggTilVeiledereRequest
import no.nav.tiltaksarrangor.koordinator.model.VeilederRequest
import no.nav.tiltaksarrangor.model.DeltakerlisteStatus
import no.nav.tiltaksarrangor.model.Endringsmelding
import no.nav.tiltaksarrangor.model.StatusType
import no.nav.tiltaksarrangor.model.Veiledertype
import no.nav.tiltaksarrangor.model.exceptions.UnauthorizedException
import no.nav.tiltaksarrangor.model.exceptions.ValidationException
import no.nav.tiltaksarrangor.repositories.AnsattRepository
import no.nav.tiltaksarrangor.repositories.ArrangorRepository
import no.nav.tiltaksarrangor.repositories.DeltakerRepository
import no.nav.tiltaksarrangor.repositories.DeltakerlisteRepository
import no.nav.tiltaksarrangor.repositories.EndringsmeldingRepository
import no.nav.tiltaksarrangor.repositories.model.AnsattDbo
import no.nav.tiltaksarrangor.repositories.model.AnsattRolleDbo
import no.nav.tiltaksarrangor.repositories.model.ArrangorDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerlisteDbo
import no.nav.tiltaksarrangor.repositories.model.KoordinatorDeltakerlisteDbo
import no.nav.tiltaksarrangor.repositories.model.VeilederDeltakerDbo
import no.nav.tiltaksarrangor.service.AnsattService
import no.nav.tiltaksarrangor.service.MetricsService
import no.nav.tiltaksarrangor.testutils.DbTestDataUtils
import no.nav.tiltaksarrangor.testutils.SingletonPostgresContainer
import no.nav.tiltaksarrangor.testutils.getDeltaker
import no.nav.tiltaksarrangor.testutils.getDeltakerliste
import no.nav.tiltaksarrangor.testutils.getEndringsmelding
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class KoordinatorServiceTest {
	private val amtArrangorClient = mockk<AmtArrangorClient>()
	private val metricsService = mockk<MetricsService>(relaxed = true)
	private val dataSource = SingletonPostgresContainer.getDataSource()
	private val template = NamedParameterJdbcTemplate(dataSource)
	private val ansattRepository = AnsattRepository(template)
	private val ansattService = AnsattService(amtArrangorClient, ansattRepository)
	private val deltakerRepository = DeltakerRepository(template)
	private val deltakerlisteRepository = DeltakerlisteRepository(template, deltakerRepository)
	private val arrangorRepository = ArrangorRepository(template)
	private val endringsmeldingRepository = EndringsmeldingRepository(template)
	private val koordinatorService =
		KoordinatorService(
			ansattService,
			deltakerlisteRepository,
			arrangorRepository,
			deltakerRepository,
			endringsmeldingRepository,
			metricsService,
		)

	@AfterEach
	internal fun tearDown() {
		DbTestDataUtils.cleanDatabase(dataSource)
		clearMocks(amtArrangorClient)
	}

	@Test
	fun `getMineDeltakerlister - ansatt har ingen roller - returnerer unauthorized`() {
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.randomUUID()
		val deltakerId2 = UUID.randomUUID()
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId))
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId2))
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.randomUUID(),
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller = emptyList(),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id)),
				veilederDeltakere =
					listOf(
						VeilederDeltakerDbo(deltakerId, Veiledertype.VEILEDER),
						VeilederDeltakerDbo(deltakerId2, Veiledertype.MEDVEILEDER),
					),
			),
		)

		assertThrows<UnauthorizedException> {
			koordinatorService.getMineDeltakerlister(personIdent)
		}
	}

	@Test
	fun `getMineDeltakerlister - ansatt er veileder - returnerer riktig respons`() {
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId1 = UUID.randomUUID()
		val deltakerId2 = UUID.randomUUID()
		val deltakerId3 = UUID.randomUUID()
		val deltakerId4 = UUID.randomUUID()
		val deltakerId5 = UUID.randomUUID()
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId1))
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId2))
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId3))
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId4))
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId5))
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.randomUUID(),
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller = listOf(AnsattRolleDbo(arrangorId, AnsattRolle.VEILEDER)),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id)),
				veilederDeltakere =
					listOf(
						VeilederDeltakerDbo(deltakerId1, Veiledertype.VEILEDER),
						VeilederDeltakerDbo(deltakerId2, Veiledertype.MEDVEILEDER),
						VeilederDeltakerDbo(deltakerId3, Veiledertype.VEILEDER),
						VeilederDeltakerDbo(deltakerId4, Veiledertype.MEDVEILEDER),
						VeilederDeltakerDbo(deltakerId5, Veiledertype.VEILEDER),
					),
			),
		)

		val mineDeltakerlister = koordinatorService.getMineDeltakerlister(personIdent)

		mineDeltakerlister.veilederFor?.veilederFor shouldBe 3
		mineDeltakerlister.veilederFor?.medveilederFor shouldBe 2
		mineDeltakerlister.koordinatorFor shouldBe null
	}

	@Test
	fun `getMineDeltakerlister - ansatt er koordinator - returnerer riktig respons`() {
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.randomUUID()
		val deltakerId2 = UUID.randomUUID()
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId))
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId2))
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.randomUUID(),
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller = listOf(AnsattRolleDbo(arrangorId, AnsattRolle.KOORDINATOR)),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id)),
				veilederDeltakere =
					listOf(
						VeilederDeltakerDbo(deltakerId, Veiledertype.VEILEDER),
						VeilederDeltakerDbo(deltakerId2, Veiledertype.MEDVEILEDER),
					),
			),
		)

		val mineDeltakerlister = koordinatorService.getMineDeltakerlister(personIdent)

		mineDeltakerlister.veilederFor shouldBe null
		mineDeltakerlister.koordinatorFor?.deltakerlister?.size shouldBe 1
	}

	@Test
	fun `getMineDeltakerlister - ansatt er veileder og koordinator - returnerer riktig respons`() {
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val arrangorId2 = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		val deltakerliste2 = getDeltakerliste(arrangorId2)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste2)
		val deltakerId1 = UUID.randomUUID()
		val deltakerId2 = UUID.randomUUID()
		val deltakerId3 = UUID.randomUUID()
		val deltakerId4 = UUID.randomUUID()
		val deltakerId5 = UUID.randomUUID()
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId1))
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId2))
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId3))
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId4))
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId5))
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.randomUUID(),
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller =
					listOf(
						AnsattRolleDbo(arrangorId, AnsattRolle.KOORDINATOR),
						AnsattRolleDbo(arrangorId, AnsattRolle.VEILEDER),
						AnsattRolleDbo(arrangorId2, AnsattRolle.KOORDINATOR),
					),
				deltakerlister =
					listOf(
						KoordinatorDeltakerlisteDbo(deltakerliste.id),
						KoordinatorDeltakerlisteDbo(deltakerliste2.id),
					),
				veilederDeltakere =
					listOf(
						VeilederDeltakerDbo(deltakerId1, Veiledertype.VEILEDER),
						VeilederDeltakerDbo(deltakerId2, Veiledertype.MEDVEILEDER),
						VeilederDeltakerDbo(deltakerId3, Veiledertype.VEILEDER),
						VeilederDeltakerDbo(deltakerId4, Veiledertype.MEDVEILEDER),
						VeilederDeltakerDbo(deltakerId5, Veiledertype.VEILEDER),
					),
			),
		)

		val mineDeltakerlister = koordinatorService.getMineDeltakerlister(personIdent)

		mineDeltakerlister.veilederFor?.veilederFor shouldBe 3
		mineDeltakerlister.veilederFor?.medveilederFor shouldBe 2
		mineDeltakerlister.koordinatorFor?.deltakerlister?.size shouldBe 2
	}

	@Test
	fun `getDeltakerliste - ansatt har ikke koordinatorrolle hos arrangor - returnerer unauthorized`() {
		val personIdent = "12345678910"
		val deltakerlisteId = UUID.randomUUID()
		val arrangorId = UUID.randomUUID()
		arrangorRepository.insertOrUpdateArrangor(
			ArrangorDbo(
				id = arrangorId,
				navn = "Arrangør AS",
				organisasjonsnummer = "88888888",
				overordnetArrangorId = null,
			),
		)
		val deltakerliste =
			DeltakerlisteDbo(
				id = deltakerlisteId,
				navn = "Gjennomføring 1",
				status = DeltakerlisteStatus.GJENNOMFORES,
				arrangorId = arrangorId,
				tiltakNavn = "Navn på tiltak",
				tiltakType = "ARBFORB",
				startDato = LocalDate.of(2023, 2, 1),
				sluttDato = null,
				erKurs = false,
				tilgjengeligForArrangorFraOgMedDato = LocalDate.of(2023, 1, 1),
			)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.randomUUID(),
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller = listOf(AnsattRolleDbo(UUID.randomUUID(), AnsattRolle.KOORDINATOR)),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerlisteId)),
				veilederDeltakere = emptyList(),
			),
		)

		assertThrows<UnauthorizedException> {
			koordinatorService.getDeltakerliste(deltakerlisteId, personIdent)
		}
	}

	@Test
	fun `getDeltakerliste - ansatt har tilgang og har lagt til deltakerliste uten deltakere - returnerer deltakerliste uten deltakere`() {
		val personIdent = "12345678910"
		val deltakerlisteId = UUID.randomUUID()
		val overordnetArrangorId = UUID.randomUUID()
		arrangorRepository.insertOrUpdateArrangor(
			ArrangorDbo(
				id = overordnetArrangorId,
				navn = "Overordnet arrangør AS",
				organisasjonsnummer = "99999999",
				overordnetArrangorId = null,
			),
		)
		val arrangorId = UUID.randomUUID()
		arrangorRepository.insertOrUpdateArrangor(
			ArrangorDbo(
				id = arrangorId,
				navn = "Arrangør AS",
				organisasjonsnummer = "88888888",
				overordnetArrangorId = overordnetArrangorId,
			),
		)
		val deltakerliste =
			DeltakerlisteDbo(
				id = deltakerlisteId,
				navn = "Gjennomføring 1",
				status = DeltakerlisteStatus.GJENNOMFORES,
				arrangorId = arrangorId,
				tiltakNavn = "Navn på tiltak",
				tiltakType = "ARBFORB",
				startDato = LocalDate.of(2023, 2, 1),
				sluttDato = null,
				erKurs = false,
				tilgjengeligForArrangorFraOgMedDato = LocalDate.of(2023, 1, 1),
			)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.randomUUID(),
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller = listOf(AnsattRolleDbo(arrangorId, AnsattRolle.KOORDINATOR)),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerlisteId)),
				veilederDeltakere = emptyList(),
			),
		)

		val koordinatorsDeltakerliste = koordinatorService.getDeltakerliste(deltakerlisteId, personIdent)

		koordinatorsDeltakerliste.id shouldBe deltakerlisteId
		koordinatorsDeltakerliste.navn shouldBe "Gjennomføring 1"
		koordinatorsDeltakerliste.tiltaksnavn shouldBe "Navn på tiltak"
		koordinatorsDeltakerliste.arrangorNavn shouldBe "Overordnet arrangør AS"
		koordinatorsDeltakerliste.startDato shouldBe LocalDate.of(2023, 2, 1)
		koordinatorsDeltakerliste.sluttDato shouldBe null
		koordinatorsDeltakerliste.status shouldBe DeltakerlisteStatus.GJENNOMFORES
		koordinatorsDeltakerliste.koordinatorer.size shouldBe 1
		koordinatorsDeltakerliste.koordinatorer.find { it.fornavn == "Fornavn" && it.etternavn == "Etternavn" } shouldNotBe null
		koordinatorsDeltakerliste.deltakere.size shouldBe 0
		koordinatorsDeltakerliste.erKurs shouldBe false
	}

	@Test
	fun `getDeltakerliste - deltakerliste ikke tilgjengelig - returnerer NoSuchElementException`() {
		val personIdent = "12345678910"
		val deltakerlisteId = UUID.randomUUID()
		val overordnetArrangorId = UUID.randomUUID()
		arrangorRepository.insertOrUpdateArrangor(
			ArrangorDbo(
				id = overordnetArrangorId,
				navn = "Overordnet arrangør AS",
				organisasjonsnummer = "99999999",
				overordnetArrangorId = null,
			),
		)
		val arrangorId = UUID.randomUUID()
		arrangorRepository.insertOrUpdateArrangor(
			ArrangorDbo(
				id = arrangorId,
				navn = "Arrangør AS",
				organisasjonsnummer = "88888888",
				overordnetArrangorId = overordnetArrangorId,
			),
		)
		val deltakerliste =
			DeltakerlisteDbo(
				id = deltakerlisteId,
				navn = "Gjennomføring 1",
				status = DeltakerlisteStatus.GJENNOMFORES,
				arrangorId = arrangorId,
				tiltakNavn = "Navn på tiltak",
				tiltakType = "ARBFORB",
				startDato = LocalDate.now().plusDays(20),
				sluttDato = null,
				erKurs = false,
				tilgjengeligForArrangorFraOgMedDato = LocalDate.now().plusDays(7),
			)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.randomUUID(),
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller = listOf(AnsattRolleDbo(arrangorId, AnsattRolle.KOORDINATOR)),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerlisteId)),
				veilederDeltakere = emptyList(),
			),
		)

		assertThrows<NoSuchElementException> {
			koordinatorService.getDeltakerliste(deltakerlisteId, personIdent)
		}
	}

	@Test
	fun `getDeltakerliste - har tilgang, lagt til deltakerliste - returnerer deltakerliste med deltakere inkl veiledere og endringsmld`() {
		val personIdent = "12345678910"
		val overordnetArrangorId = UUID.randomUUID()
		arrangorRepository.insertOrUpdateArrangor(
			ArrangorDbo(
				id = overordnetArrangorId,
				navn = "Overordnet arrangør AS",
				organisasjonsnummer = "99999999",
				overordnetArrangorId = null,
			),
		)
		val arrangorId = UUID.randomUUID()
		arrangorRepository.insertOrUpdateArrangor(
			ArrangorDbo(
				id = arrangorId,
				navn = "Arrangør AS",
				organisasjonsnummer = "88888888",
				overordnetArrangorId = overordnetArrangorId,
			),
		)
		val deltakerliste = getDeltakerliste(arrangorId)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.randomUUID(),
				personIdent = personIdent,
				fornavn = "Fornavn1",
				mellomnavn = null,
				etternavn = "Etternavn1",
				roller = listOf(AnsattRolleDbo(arrangorId, AnsattRolle.KOORDINATOR)),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id)),
				veilederDeltakere = emptyList(),
			),
		)
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.randomUUID(),
				personIdent = UUID.randomUUID().toString(),
				fornavn = "Fornavn2",
				mellomnavn = null,
				etternavn = "Etternavn2",
				roller = listOf(AnsattRolleDbo(arrangorId, AnsattRolle.KOORDINATOR)),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id)),
				veilederDeltakere = emptyList(),
			),
		)
		val deltaker = getDeltaker(UUID.randomUUID(), deltakerliste.id)
		deltakerRepository.insertOrUpdateDeltaker(deltaker)
		val endringsmelding = getEndringsmelding(deltaker.id)
		endringsmeldingRepository.insertOrUpdateEndringsmelding(endringsmelding)
		val deltaker2 =
			getDeltaker(UUID.randomUUID(), deltakerliste.id).copy(
				skjultDato = LocalDateTime.now(),
				skjultAvAnsattId = UUID.randomUUID(),
			)
		deltakerRepository.insertOrUpdateDeltaker(deltaker2)
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.randomUUID(),
				personIdent = UUID.randomUUID().toString(),
				fornavn = "Fornavn3",
				mellomnavn = null,
				etternavn = "Etternavn3",
				roller = listOf(AnsattRolleDbo(arrangorId, AnsattRolle.VEILEDER)),
				deltakerlister = emptyList(),
				veilederDeltakere =
					listOf(
						VeilederDeltakerDbo(deltaker.id, Veiledertype.VEILEDER),
						VeilederDeltakerDbo(deltaker2.id, Veiledertype.VEILEDER),
					),
			),
		)

		val koordinatorsDeltakerliste = koordinatorService.getDeltakerliste(deltakerliste.id, personIdent)

		koordinatorsDeltakerliste.id shouldBe deltakerliste.id
		koordinatorsDeltakerliste.koordinatorer.size shouldBe 2
		koordinatorsDeltakerliste.deltakere.size shouldBe 1
		val koordinatorsDeltaker = koordinatorsDeltakerliste.deltakere.find { it.id == deltaker.id }
		koordinatorsDeltaker?.status?.type shouldBe StatusType.DELTAR
		koordinatorsDeltaker?.veiledere?.size shouldBe 1
		koordinatorsDeltaker?.veiledere?.find {
			it.fornavn == "Fornavn3" && it.etternavn == "Etternavn3" && it.veiledertype == Veiledertype.VEILEDER
		} shouldNotBe null
		koordinatorsDeltaker?.aktiveEndringsmeldinger?.size shouldBe 1
		koordinatorsDeltaker?.aktiveEndringsmeldinger?.find { it.type == Endringsmelding.Type.FORLENG_DELTAKELSE } shouldNotBe null
	}

	@Test
	fun `getTilgjengeligeVeiledere - ansatt har ikke koordinatorrolle hos arrangor - returnerer unauthorized`() {
		val personIdent = "12345678910"
		val deltakerlisteId = UUID.randomUUID()
		val arrangorId = UUID.randomUUID()
		val deltakerliste =
			DeltakerlisteDbo(
				id = deltakerlisteId,
				navn = "Gjennomføring 1",
				status = DeltakerlisteStatus.GJENNOMFORES,
				arrangorId = arrangorId,
				tiltakNavn = "Navn på tiltak",
				tiltakType = "ARBFORB",
				startDato = LocalDate.of(2023, 2, 1),
				sluttDato = null,
				erKurs = false,
				tilgjengeligForArrangorFraOgMedDato = LocalDate.of(2023, 1, 1),
			)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.randomUUID(),
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller = listOf(AnsattRolleDbo(UUID.randomUUID(), AnsattRolle.KOORDINATOR)),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerlisteId)),
				veilederDeltakere = emptyList(),
			),
		)

		assertThrows<UnauthorizedException> {
			koordinatorService.getTilgjengeligeVeiledere(deltakerlisteId, personIdent)
		}
	}

	@Test
	fun `getTilgjengeligeVeiledere - ansatt har koordinatorrolle hos arrangor men det finnes ingen veiledere - returnerer tom liste`() {
		val personIdent = "12345678910"
		val deltakerlisteId = UUID.randomUUID()
		val arrangorId = UUID.randomUUID()
		val deltakerliste =
			DeltakerlisteDbo(
				id = deltakerlisteId,
				navn = "Gjennomføring 1",
				status = DeltakerlisteStatus.GJENNOMFORES,
				arrangorId = arrangorId,
				tiltakNavn = "Navn på tiltak",
				tiltakType = "ARBFORB",
				startDato = LocalDate.of(2023, 2, 1),
				sluttDato = null,
				erKurs = false,
				tilgjengeligForArrangorFraOgMedDato = LocalDate.of(2023, 1, 1),
			)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.randomUUID(),
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller = listOf(AnsattRolleDbo(arrangorId, AnsattRolle.KOORDINATOR)),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerlisteId)),
				veilederDeltakere = emptyList(),
			),
		)

		koordinatorService.getTilgjengeligeVeiledere(deltakerlisteId, personIdent).size shouldBe 0
	}

	@Test
	fun `getTilgjengeligeVeiledere - ansatt har koordinatorrolle hos arrangor og det finnes veiledere - returnerer veiledere`() {
		val personIdent = "12345678910"
		val deltakerlisteId = UUID.randomUUID()
		val arrangorId = UUID.randomUUID()
		val deltakerliste =
			DeltakerlisteDbo(
				id = deltakerlisteId,
				navn = "Gjennomføring 1",
				status = DeltakerlisteStatus.GJENNOMFORES,
				arrangorId = arrangorId,
				tiltakNavn = "Navn på tiltak",
				tiltakType = "ARBFORB",
				startDato = LocalDate.of(2023, 2, 1),
				sluttDato = null,
				erKurs = false,
				tilgjengeligForArrangorFraOgMedDato = LocalDate.of(2023, 1, 1),
			)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val ansattId1 = UUID.randomUUID()
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = ansattId1,
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller =
					listOf(
						AnsattRolleDbo(arrangorId, AnsattRolle.KOORDINATOR),
						AnsattRolleDbo(arrangorId, AnsattRolle.VEILEDER),
					),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerlisteId)),
				veilederDeltakere = emptyList(),
			),
		)
		val ansattId2 = UUID.randomUUID()
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = ansattId2,
				personIdent = UUID.randomUUID().toString(),
				fornavn = "Fornavn2",
				mellomnavn = null,
				etternavn = "Etternavn2",
				roller = listOf(AnsattRolleDbo(arrangorId, AnsattRolle.VEILEDER)),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerlisteId)),
				veilederDeltakere = emptyList(),
			),
		)
		val ansattId3 = UUID.randomUUID()
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = ansattId3,
				personIdent = UUID.randomUUID().toString(),
				fornavn = "Fornavn3",
				mellomnavn = null,
				etternavn = "Etternavn3",
				roller = listOf(AnsattRolleDbo(UUID.randomUUID(), AnsattRolle.VEILEDER)),
				deltakerlister = emptyList(),
				veilederDeltakere = emptyList(),
			),
		)

		val veiledere = koordinatorService.getTilgjengeligeVeiledere(deltakerlisteId, personIdent)
		veiledere.size shouldBe 2
		veiledere.find { it.ansattId == ansattId1 } shouldNotBe null
		veiledere.find { it.ansattId == ansattId2 } shouldNotBe null
	}

	@Test
	fun `tildelVeiledereForDeltaker - ansatt har ikke koordinatorrolle hos arrangor - returnerer unauthorized`() {
		val personIdent = "12345678910"
		val deltakerlisteId = UUID.randomUUID()
		val arrangorId = UUID.randomUUID()
		val deltakerliste =
			DeltakerlisteDbo(
				id = deltakerlisteId,
				navn = "Gjennomføring 1",
				status = DeltakerlisteStatus.GJENNOMFORES,
				arrangorId = arrangorId,
				tiltakNavn = "Navn på tiltak",
				tiltakType = "ARBFORB",
				startDato = LocalDate.of(2023, 2, 1),
				sluttDato = null,
				erKurs = false,
				tilgjengeligForArrangorFraOgMedDato = LocalDate.of(2023, 1, 1),
			)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.randomUUID()
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId, deltakerlisteId))
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.randomUUID(),
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller = listOf(AnsattRolleDbo(UUID.randomUUID(), AnsattRolle.KOORDINATOR)),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerlisteId)),
				veilederDeltakere = emptyList(),
			),
		)
		val request =
			LeggTilVeiledereRequest(
				veiledere = listOf(VeilederRequest(UUID.randomUUID(), true)),
			)

		assertThrows<UnauthorizedException> {
			koordinatorService.tildelVeiledereForDeltaker(deltakerId, request, personIdent)
		}
	}

	@Test
	fun `tildelVeiledereForDeltaker - request inneholder fire medveiledere - returnerer validation exception`() {
		val personIdent = "12345678910"
		val deltakerlisteId = UUID.randomUUID()
		val arrangorId = UUID.randomUUID()
		val deltakerliste =
			DeltakerlisteDbo(
				id = deltakerlisteId,
				navn = "Gjennomføring 1",
				status = DeltakerlisteStatus.GJENNOMFORES,
				arrangorId = arrangorId,
				tiltakNavn = "Navn på tiltak",
				tiltakType = "ARBFORB",
				startDato = LocalDate.of(2023, 2, 1),
				sluttDato = null,
				erKurs = false,
				tilgjengeligForArrangorFraOgMedDato = LocalDate.of(2023, 1, 1),
			)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.randomUUID()
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId, deltakerlisteId))
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.randomUUID(),
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller = listOf(AnsattRolleDbo(arrangorId, AnsattRolle.KOORDINATOR)),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerlisteId)),
				veilederDeltakere = emptyList(),
			),
		)
		val request =
			LeggTilVeiledereRequest(
				veiledere =
					listOf(
						VeilederRequest(UUID.randomUUID(), true),
						VeilederRequest(UUID.randomUUID(), true),
						VeilederRequest(UUID.randomUUID(), true),
						VeilederRequest(UUID.randomUUID(), true),
					),
			)

		assertThrows<ValidationException> {
			koordinatorService.tildelVeiledereForDeltaker(deltakerId, request, personIdent)
		}
	}

	@Test
	fun `tildelVeiledereForDeltaker - ny veileder har ikke veileder-rolle - returnerer unauthorized`() {
		val personIdent = "12345678910"
		val deltakerlisteId = UUID.randomUUID()
		val arrangorId = UUID.randomUUID()
		val deltakerliste =
			DeltakerlisteDbo(
				id = deltakerlisteId,
				navn = "Gjennomføring 1",
				status = DeltakerlisteStatus.GJENNOMFORES,
				arrangorId = arrangorId,
				tiltakNavn = "Navn på tiltak",
				tiltakType = "ARBFORB",
				startDato = LocalDate.of(2023, 2, 1),
				sluttDato = null,
				erKurs = false,
				tilgjengeligForArrangorFraOgMedDato = LocalDate.of(2023, 1, 1),
			)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.randomUUID()
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId, deltakerlisteId))
		val ansattId = UUID.randomUUID()
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = ansattId,
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller = listOf(AnsattRolleDbo(arrangorId, AnsattRolle.KOORDINATOR)),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerlisteId)),
				veilederDeltakere = emptyList(),
			),
		)
		val request =
			LeggTilVeiledereRequest(
				veiledere =
					listOf(
						VeilederRequest(ansattId, false),
					),
			)

		assertThrows<UnauthorizedException> {
			koordinatorService.tildelVeiledereForDeltaker(deltakerId, request, personIdent)
		}
	}

	@Test
	fun `tildelVeiledereForDeltaker - samme ansatt blir veileder og medveileder - returnerer validation exception`() {
		val personIdent = "12345678910"
		val deltakerlisteId = UUID.randomUUID()
		val arrangorId = UUID.randomUUID()
		val deltakerliste =
			DeltakerlisteDbo(
				id = deltakerlisteId,
				navn = "Gjennomføring 1",
				status = DeltakerlisteStatus.GJENNOMFORES,
				arrangorId = arrangorId,
				tiltakNavn = "Navn på tiltak",
				tiltakType = "ARBFORB",
				startDato = LocalDate.of(2023, 2, 1),
				sluttDato = null,
				erKurs = false,
				tilgjengeligForArrangorFraOgMedDato = LocalDate.of(2023, 1, 1),
			)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.randomUUID()
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId, deltakerlisteId))
		val ansattId = UUID.randomUUID()
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = ansattId,
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller = listOf(AnsattRolleDbo(arrangorId, AnsattRolle.KOORDINATOR), AnsattRolleDbo(arrangorId, AnsattRolle.VEILEDER)),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerlisteId)),
				veilederDeltakere = emptyList(),
			),
		)
		val request =
			LeggTilVeiledereRequest(
				veiledere =
					listOf(
						VeilederRequest(ansattId, false),
						VeilederRequest(ansattId, true),
					),
			)

		assertThrows<ValidationException> {
			koordinatorService.tildelVeiledereForDeltaker(deltakerId, request, personIdent)
		}
	}

	@Test
	fun `tildelVeiledereForDeltaker - ny veileder - lagrer ny veileder`() {
		coEvery { amtArrangorClient.oppdaterVeilederForDeltaker(any(), any()) } just Runs
		val personIdent = "12345678910"
		val deltakerlisteId = UUID.randomUUID()
		val arrangorId = UUID.randomUUID()
		val deltakerliste =
			DeltakerlisteDbo(
				id = deltakerlisteId,
				navn = "Gjennomføring 1",
				status = DeltakerlisteStatus.GJENNOMFORES,
				arrangorId = arrangorId,
				tiltakNavn = "Navn på tiltak",
				tiltakType = "ARBFORB",
				startDato = LocalDate.of(2023, 2, 1),
				sluttDato = null,
				erKurs = false,
				tilgjengeligForArrangorFraOgMedDato = LocalDate.of(2023, 1, 1),
			)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.randomUUID()
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId, deltakerlisteId))
		val ansattId = UUID.randomUUID()
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = ansattId,
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller = listOf(AnsattRolleDbo(arrangorId, AnsattRolle.KOORDINATOR)),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerlisteId)),
				veilederDeltakere = emptyList(),
			),
		)
		val veileder1Id = UUID.randomUUID()
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = veileder1Id,
				personIdent = UUID.randomUUID().toString(),
				fornavn = "Fornavn1",
				mellomnavn = null,
				etternavn = "Etternavn1",
				roller = listOf(AnsattRolleDbo(arrangorId, AnsattRolle.VEILEDER)),
				deltakerlister = emptyList(),
				veilederDeltakere = emptyList(),
			),
		)
		val veileder2Id = UUID.randomUUID()
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = veileder2Id,
				personIdent = UUID.randomUUID().toString(),
				fornavn = "Fornavn2",
				mellomnavn = null,
				etternavn = "Etternavn2",
				roller = listOf(AnsattRolleDbo(arrangorId, AnsattRolle.VEILEDER)),
				deltakerlister = emptyList(),
				veilederDeltakere = listOf(VeilederDeltakerDbo(deltakerId, Veiledertype.MEDVEILEDER)),
			),
		)
		val request =
			LeggTilVeiledereRequest(
				veiledere =
					listOf(
						VeilederRequest(veileder2Id, false),
						VeilederRequest(veileder1Id, true),
					),
			)

		koordinatorService.tildelVeiledereForDeltaker(deltakerId, request, personIdent)

		val deltakersVeiledere = ansattService.getVeiledereForDeltaker(deltakerId)
		deltakersVeiledere.size shouldBe 2
		deltakersVeiledere.find { it.ansattId == veileder1Id && it.veiledertype == Veiledertype.MEDVEILEDER } shouldNotBe null
		deltakersVeiledere.find { it.ansattId == veileder2Id && it.veiledertype == Veiledertype.VEILEDER } shouldNotBe null

		coVerify {
			amtArrangorClient.oppdaterVeilederForDeltaker(
				deltakerId,
				match {
					it.arrangorId == arrangorId && it.veilederSomFjernes.size == 1 && it.veilederSomLeggesTil.size == 2 &&
						it.veilederSomFjernes.contains(VeilederAnsatt(veileder2Id, Veiledertype.MEDVEILEDER)) &&
						it.veilederSomLeggesTil.contains(VeilederAnsatt(veileder1Id, Veiledertype.MEDVEILEDER)) &&
						it.veilederSomLeggesTil.contains(VeilederAnsatt(veileder2Id, Veiledertype.VEILEDER))
				},
			)
		}
	}

	@Test
	fun `tildelVeiledereForDeltaker - fjerner veileder - veileder fjernes`() {
		coEvery { amtArrangorClient.oppdaterVeilederForDeltaker(any(), any()) } just Runs
		val personIdent = "12345678910"
		val deltakerlisteId = UUID.randomUUID()
		val arrangorId = UUID.randomUUID()
		val deltakerliste =
			DeltakerlisteDbo(
				id = deltakerlisteId,
				navn = "Gjennomføring 1",
				status = DeltakerlisteStatus.GJENNOMFORES,
				arrangorId = arrangorId,
				tiltakNavn = "Navn på tiltak",
				tiltakType = "ARBFORB",
				startDato = LocalDate.of(2023, 2, 1),
				sluttDato = null,
				erKurs = false,
				tilgjengeligForArrangorFraOgMedDato = LocalDate.of(2023, 1, 1),
			)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.randomUUID()
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId, deltakerlisteId))
		val ansattId = UUID.randomUUID()
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = ansattId,
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller = listOf(AnsattRolleDbo(arrangorId, AnsattRolle.KOORDINATOR)),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerlisteId)),
				veilederDeltakere = emptyList(),
			),
		)
		val veileder1Id = UUID.randomUUID()
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = veileder1Id,
				personIdent = UUID.randomUUID().toString(),
				fornavn = "Fornavn1",
				mellomnavn = null,
				etternavn = "Etternavn1",
				roller = listOf(AnsattRolleDbo(arrangorId, AnsattRolle.VEILEDER)),
				deltakerlister = emptyList(),
				veilederDeltakere = listOf(VeilederDeltakerDbo(deltakerId, Veiledertype.VEILEDER)),
			),
		)
		val request =
			LeggTilVeiledereRequest(
				veiledere = emptyList(),
			)

		koordinatorService.tildelVeiledereForDeltaker(deltakerId, request, personIdent)

		val deltakersVeiledere = ansattService.getVeiledereForDeltaker(deltakerId)
		deltakersVeiledere.size shouldBe 0

		coVerify {
			amtArrangorClient.oppdaterVeilederForDeltaker(
				deltakerId,
				match {
					it.arrangorId == arrangorId && it.veilederSomFjernes.size == 1 && it.veilederSomLeggesTil.isEmpty() &&
						it.veilederSomFjernes.contains(VeilederAnsatt(veileder1Id, Veiledertype.VEILEDER))
				},
			)
		}
	}
}
