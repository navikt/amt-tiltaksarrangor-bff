package no.nav.tiltaksarrangor.endringsmelding.service

import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import no.nav.tiltaksarrangor.client.amtarrangor.AmtArrangorClient
import no.nav.tiltaksarrangor.client.amttiltak.AmtTiltakClient
import no.nav.tiltaksarrangor.endringsmelding.controller.request.EndringsmeldingRequest
import no.nav.tiltaksarrangor.ingest.model.AnsattRolle
import no.nav.tiltaksarrangor.ingest.model.EndringsmeldingType
import no.nav.tiltaksarrangor.ingest.model.Innhold
import no.nav.tiltaksarrangor.model.DeltakerStatusAarsak
import no.nav.tiltaksarrangor.model.Endringsmelding
import no.nav.tiltaksarrangor.model.StatusType
import no.nav.tiltaksarrangor.model.Veiledertype
import no.nav.tiltaksarrangor.model.exceptions.SkjultDeltakerException
import no.nav.tiltaksarrangor.model.exceptions.UnauthorizedException
import no.nav.tiltaksarrangor.model.exceptions.ValidationException
import no.nav.tiltaksarrangor.repositories.AnsattRepository
import no.nav.tiltaksarrangor.repositories.DeltakerRepository
import no.nav.tiltaksarrangor.repositories.DeltakerlisteRepository
import no.nav.tiltaksarrangor.repositories.EndringsmeldingRepository
import no.nav.tiltaksarrangor.repositories.model.AnsattDbo
import no.nav.tiltaksarrangor.repositories.model.AnsattRolleDbo
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

class EndringsmeldingServiceTest {
	private val amtTiltakClient = mockk<AmtTiltakClient>()
	private val amtArrangorClient = mockk<AmtArrangorClient>()
	private val metricsService = mockk<MetricsService>(relaxed = true)
	private val dataSource = SingletonPostgresContainer.getDataSource()
	private val template = NamedParameterJdbcTemplate(dataSource)
	private val ansattRepository = AnsattRepository(template)
	private val ansattService = AnsattService(amtArrangorClient, ansattRepository)
	private val deltakerRepository = DeltakerRepository(template)
	private val deltakerlisteRepository = DeltakerlisteRepository(template, deltakerRepository)
	private val endringsmeldingRepository = EndringsmeldingRepository(template)
	private val endringsmeldingService =
		EndringsmeldingService(
			amtTiltakClient,
			ansattService,
			endringsmeldingRepository,
			deltakerRepository,
			metricsService,
		)

	@AfterEach
	internal fun tearDown() {
		DbTestDataUtils.cleanDatabase(dataSource)
		clearMocks(amtTiltakClient)
	}

	@Test
	fun `getAlleEndringsmeldinger - ansatt har ikke rolle hos arrangor - returnerer unauthorized`() {
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.randomUUID()
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId, deltakerliste.id))
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.randomUUID(),
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller =
					listOf(
						AnsattRolleDbo(UUID.randomUUID(), AnsattRolle.KOORDINATOR),
					),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id)),
				veilederDeltakere = emptyList(),
			),
		)
		val endringsmelding = getEndringsmelding(deltakerId)
		endringsmeldingRepository.insertOrUpdateEndringsmelding(endringsmelding)

		assertThrows<UnauthorizedException> {
			endringsmeldingService.getAlleEndringsmeldinger(deltakerId, personIdent)
		}
	}

	@Test
	fun `getAlleEndringsmeldinger - deltaker er skjult - returnerer skjult deltaker exception`() {
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.randomUUID()
		val deltaker = getDeltaker(deltakerId, deltakerliste.id).copy(skjultDato = LocalDateTime.now(), skjultAvAnsattId = UUID.randomUUID())
		deltakerRepository.insertOrUpdateDeltaker(deltaker)
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
					),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id)),
				veilederDeltakere = emptyList(),
			),
		)
		val endringsmelding = getEndringsmelding(deltakerId)
		endringsmeldingRepository.insertOrUpdateEndringsmelding(endringsmelding)

		assertThrows<SkjultDeltakerException> {
			endringsmeldingService.getAlleEndringsmeldinger(deltakerId, personIdent)
		}
	}

	@Test
	fun `getAlleEndringsmeldinger - deltakerliste er ikke tilgjengelig - returnerer NoSuchElementException`() {
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId).copy(
			startDato = LocalDate.now().plusMonths(1),
			tilgjengeligForArrangorFraOgMedDato = LocalDate.now().plusDays(2),
		)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.randomUUID()
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId, deltakerliste.id))
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.randomUUID(),
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller =
					listOf(
						AnsattRolleDbo(arrangorId, AnsattRolle.VEILEDER),
					),
				deltakerlister = emptyList(),
				veilederDeltakere = listOf(VeilederDeltakerDbo(deltakerId, Veiledertype.VEILEDER)),
			),
		)
		val endringsmeldingDbo1 = getEndringsmelding(deltakerId)
		val endringsmeldingDbo2 =
			getEndringsmelding(deltakerId).copy(
				type = EndringsmeldingType.ENDRE_DELTAKELSE_PROSENT,
				innhold =
					Innhold.EndreDeltakelseProsentInnhold(
						nyDeltakelseProsent = 50,
						dagerPerUke = 2,
						gyldigFraDato = LocalDate.now(),
					),
			)
		endringsmeldingRepository.insertOrUpdateEndringsmelding(endringsmeldingDbo1)
		endringsmeldingRepository.insertOrUpdateEndringsmelding(endringsmeldingDbo2)

		assertThrows<NoSuchElementException> {
			endringsmeldingService.getAlleEndringsmeldinger(deltakerId, personIdent)
		}
	}

	@Test
	fun `getAlleEndringsmeldinger - ansatt har tilgang - henter endringsmeldinger`() {
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.randomUUID()
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId, deltakerliste.id))
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.randomUUID(),
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller =
					listOf(
						AnsattRolleDbo(arrangorId, AnsattRolle.VEILEDER),
					),
				deltakerlister = emptyList(),
				veilederDeltakere = listOf(VeilederDeltakerDbo(deltakerId, Veiledertype.VEILEDER)),
			),
		)
		val endringsmeldingDbo1 = getEndringsmelding(deltakerId)
		val endringsmeldingDbo2 =
			getEndringsmelding(deltakerId).copy(
				type = EndringsmeldingType.ENDRE_DELTAKELSE_PROSENT,
				innhold =
					Innhold.EndreDeltakelseProsentInnhold(
						nyDeltakelseProsent = 50,
						dagerPerUke = 2,
						gyldigFraDato = LocalDate.now(),
					),
			)
		val endringsmeldingDbo3 = getEndringsmelding(deltakerId).copy(status = Endringsmelding.Status.UTFORT)
		endringsmeldingRepository.insertOrUpdateEndringsmelding(endringsmeldingDbo1)
		endringsmeldingRepository.insertOrUpdateEndringsmelding(endringsmeldingDbo2)
		endringsmeldingRepository.insertOrUpdateEndringsmelding(endringsmeldingDbo3)

		val endringsmeldingResponse = endringsmeldingService.getAlleEndringsmeldinger(deltakerId, personIdent)
		endringsmeldingResponse.aktiveEndringsmeldinger.size shouldBe 2
		val endringsmelding1 = endringsmeldingResponse.aktiveEndringsmeldinger.find { it.id == endringsmeldingDbo1.id }
		endringsmelding1?.type shouldBe Endringsmelding.Type.FORLENG_DELTAKELSE
		endringsmelding1?.innhold shouldBe Endringsmelding.Innhold.ForlengDeltakelseInnhold(sluttdato = LocalDate.now().plusMonths(2))

		val endringsmelding2 = endringsmeldingResponse.aktiveEndringsmeldinger.find { it.id == endringsmeldingDbo2.id }
		endringsmelding2?.type shouldBe Endringsmelding.Type.ENDRE_DELTAKELSE_PROSENT
		endringsmelding2?.innhold shouldBe
			Endringsmelding.Innhold.EndreDeltakelseProsentInnhold(
				deltakelseProsent = 50,
				dagerPerUke = 2,
				gyldigFraDato = LocalDate.now(),
			)
		endringsmeldingResponse.historiskeEndringsmeldinger.size shouldBe 1
		val historiskEndringsmelding = endringsmeldingResponse.historiskeEndringsmeldinger.first()
		historiskEndringsmelding.id shouldBe endringsmeldingDbo3.id
		historiskEndringsmelding.status shouldBe Endringsmelding.Status.UTFORT
	}

	@Test
	fun `getAlleEndringsmeldinger - ansatt er koordinator, deltaker adressebeskyttet - returnerer unauthorized`() {
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.randomUUID()
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId, deltakerliste.id, adressebeskyttet = true))
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
					),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id)),
				veilederDeltakere = emptyList(),
			),
		)
		val endringsmeldingDbo = getEndringsmelding(deltakerId)
		endringsmeldingRepository.insertOrUpdateEndringsmelding(endringsmeldingDbo)

		assertThrows<UnauthorizedException> {
			endringsmeldingService.getAlleEndringsmeldinger(deltakerId, personIdent)
		}
	}

	@Test
	fun `getAlleEndringsmeldinger - ansatt er veileder, deltaker adressebeskyttet - henter endringsmeldinger`() {
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.randomUUID()
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId, deltakerliste.id, adressebeskyttet = true))
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.randomUUID(),
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller =
					listOf(
						AnsattRolleDbo(arrangorId, AnsattRolle.VEILEDER),
					),
				deltakerlister = emptyList(),
				veilederDeltakere = listOf(VeilederDeltakerDbo(deltakerId, Veiledertype.VEILEDER)),
			),
		)
		val endringsmeldingDbo = getEndringsmelding(deltakerId)
		endringsmeldingRepository.insertOrUpdateEndringsmelding(endringsmeldingDbo)

		val endringsmeldingResponse = endringsmeldingService.getAlleEndringsmeldinger(deltakerId, personIdent)
		endringsmeldingResponse.aktiveEndringsmeldinger.size shouldBe 1
		val endringsmelding = endringsmeldingResponse.aktiveEndringsmeldinger.find { it.id == endringsmeldingDbo.id }
		endringsmelding?.type shouldBe Endringsmelding.Type.FORLENG_DELTAKELSE
		endringsmelding?.innhold shouldBe Endringsmelding.Innhold.ForlengDeltakelseInnhold(sluttdato = LocalDate.now().plusMonths(2))
	}

	@Test
	fun `slettEndringsmelding - ansatt har ikke rolle hos arrangor - returnerer unauthorized`() {
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.randomUUID()
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId, deltakerliste.id))
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.randomUUID(),
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller =
					listOf(
						AnsattRolleDbo(UUID.randomUUID(), AnsattRolle.KOORDINATOR),
					),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id)),
				veilederDeltakere = emptyList(),
			),
		)
		val endringsmelding = getEndringsmelding(deltakerId)
		endringsmeldingRepository.insertOrUpdateEndringsmelding(endringsmelding)

		assertThrows<UnauthorizedException> {
			endringsmeldingService.slettEndringsmelding(endringsmelding.id, personIdent)
		}
	}

	@Test
	fun `slettEndringsmelding - ansatt har tilgang - merker endringsmelding som tilbakekalt`() {
		coEvery { amtTiltakClient.tilbakekallEndringsmelding(any()) } just Runs
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.randomUUID()
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId, deltakerliste.id))
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
					),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id)),
				veilederDeltakere = emptyList(),
			),
		)
		val endringsmelding = getEndringsmelding(deltakerId)
		endringsmeldingRepository.insertOrUpdateEndringsmelding(endringsmelding)

		endringsmeldingService.slettEndringsmelding(endringsmelding.id, personIdent)

		endringsmeldingRepository.getEndringsmelding(endringsmelding.id)?.status shouldBe Endringsmelding.Status.TILBAKEKALT
		coVerify { amtTiltakClient.tilbakekallEndringsmelding(endringsmelding.id) }
	}

	@Test
	fun `opprettEndringsmelding - ansatt har ikke rolle hos arrangor - returnerer unauthorized`() {
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.randomUUID()
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId, deltakerliste.id))
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.randomUUID(),
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller =
					listOf(
						AnsattRolleDbo(UUID.randomUUID(), AnsattRolle.KOORDINATOR),
					),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id)),
				veilederDeltakere = emptyList(),
			),
		)
		val endringsmeldingRequest =
			EndringsmeldingRequest(
				innhold = EndringsmeldingRequest.Innhold.LeggTilOppstartsdatoInnhold(LocalDate.now()),
			)

		assertThrows<UnauthorizedException> {
			endringsmeldingService.opprettEndringsmelding(deltakerId, endringsmeldingRequest, personIdent)
		}
	}

	@Test
	fun `opprettEndringsmelding - deltaker er skjult - returnerer skjult deltaker exception`() {
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.randomUUID()
		val deltaker = getDeltaker(deltakerId, deltakerliste.id).copy(skjultDato = LocalDateTime.now(), skjultAvAnsattId = UUID.randomUUID())
		deltakerRepository.insertOrUpdateDeltaker(deltaker)
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
					),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id)),
				veilederDeltakere = emptyList(),
			),
		)
		val endringsmeldingRequest =
			EndringsmeldingRequest(
				innhold = EndringsmeldingRequest.Innhold.LeggTilOppstartsdatoInnhold(LocalDate.now()),
			)

		assertThrows<SkjultDeltakerException> {
			endringsmeldingService.opprettEndringsmelding(deltakerId, endringsmeldingRequest, personIdent)
		}
	}

	@Test
	fun `opprettEndringsmelding - legg til oppstartdato - oppretter endringsmelding`() {
		val endringsmeldingId = UUID.randomUUID()
		coEvery { amtTiltakClient.leggTilOppstartsdato(any(), any()) } returns endringsmeldingId
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.randomUUID()
		val deltaker = getDeltaker(deltakerId, deltakerliste.id)
		deltakerRepository.insertOrUpdateDeltaker(deltaker)
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
					),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id)),
				veilederDeltakere = emptyList(),
			),
		)
		val endringsmeldingRequest =
			EndringsmeldingRequest(
				innhold = EndringsmeldingRequest.Innhold.LeggTilOppstartsdatoInnhold(LocalDate.now()),
			)

		endringsmeldingService.opprettEndringsmelding(deltakerId, endringsmeldingRequest, personIdent)

		val endringsmeldinger = endringsmeldingRepository.getEndringsmeldingerForDeltaker(deltakerId)
		endringsmeldinger.size shouldBe 1
		val endringsmelding = endringsmeldinger.first()
		endringsmelding.id shouldBe endringsmeldingId
		endringsmelding.type shouldBe EndringsmeldingType.LEGG_TIL_OPPSTARTSDATO
		(endringsmelding.innhold as Innhold.LeggTilOppstartsdatoInnhold).oppstartsdato shouldBe LocalDate.now()
	}

	@Test
	fun `opprettEndringsmelding - endre deltakelsesprosent - oppretter endringsmelding`() {
		val endringsmeldingId = UUID.randomUUID()
		coEvery { amtTiltakClient.endreDeltakelsesprosent(any(), any()) } returns endringsmeldingId
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.randomUUID()
		val deltaker = getDeltaker(deltakerId, deltakerliste.id)
		deltakerRepository.insertOrUpdateDeltaker(deltaker)
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
					),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id)),
				veilederDeltakere = emptyList(),
			),
		)
		val endringsmeldingRequest =
			EndringsmeldingRequest(
				innhold =
					EndringsmeldingRequest.Innhold.EndreDeltakelseProsentInnhold(
						deltakelseProsent = 50,
						dagerPerUke = 4,
						gyldigFraDato = LocalDate.now(),
					),
			)

		endringsmeldingService.opprettEndringsmelding(deltakerId, endringsmeldingRequest, personIdent)

		val endringsmeldinger = endringsmeldingRepository.getEndringsmeldingerForDeltaker(deltakerId)
		endringsmeldinger.size shouldBe 1
		val endringsmelding = endringsmeldinger.first()
		endringsmelding.id shouldBe endringsmeldingId
		endringsmelding.type shouldBe EndringsmeldingType.ENDRE_DELTAKELSE_PROSENT
		(endringsmelding.innhold as Innhold.EndreDeltakelseProsentInnhold).nyDeltakelseProsent shouldBe 50
		(endringsmelding.innhold as Innhold.EndreDeltakelseProsentInnhold).dagerPerUke shouldBe 4
		(endringsmelding.innhold as Innhold.EndreDeltakelseProsentInnhold).gyldigFraDato shouldBe LocalDate.now()
	}

	@Test
	fun `opprettEndringsmelding - har endringsmelding av samme type - oppdaterer gammel endringsmelding og oppretter ny`() {
		val endringsmeldingId = UUID.randomUUID()
		coEvery { amtTiltakClient.forlengDeltakelse(any(), any()) } returns endringsmeldingId
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.randomUUID()
		val deltaker = getDeltaker(deltakerId, deltakerliste.id)
		deltakerRepository.insertOrUpdateDeltaker(deltaker)
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
					),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id)),
				veilederDeltakere = emptyList(),
			),
		)
		val endringsmelding1 = getEndringsmelding(deltakerId)
		endringsmeldingRepository.insertOrUpdateEndringsmelding(endringsmelding1)
		val endringsmeldingRequest =
			EndringsmeldingRequest(
				innhold =
					EndringsmeldingRequest.Innhold.ForlengDeltakelseInnhold(
						sluttdato = LocalDate.now(),
					),
			)

		endringsmeldingService.opprettEndringsmelding(deltakerId, endringsmeldingRequest, personIdent)

		val endringsmeldinger = endringsmeldingRepository.getEndringsmeldingerForDeltaker(deltakerId)
		endringsmeldinger.size shouldBe 2
		val aktivEndringsmelding = endringsmeldinger.find { it.erAktiv() }
		aktivEndringsmelding?.id shouldBe endringsmeldingId
		aktivEndringsmelding?.type shouldBe EndringsmeldingType.FORLENG_DELTAKELSE
		(aktivEndringsmelding?.innhold as Innhold.ForlengDeltakelseInnhold).sluttdato shouldBe LocalDate.now()
		val utdatertEndringsmelding = endringsmeldinger.find { !it.erAktiv() }
		utdatertEndringsmelding?.id shouldBe endringsmelding1.id
		utdatertEndringsmelding?.status shouldBe Endringsmelding.Status.UTDATERT
	}

	@Test
	fun `opprettEndringsmelding - har utfort endringsmelding av samme type - oppdaterer ikke gammel endringsmelding og oppretter ny`() {
		val endringsmeldingId = UUID.randomUUID()
		coEvery { amtTiltakClient.forlengDeltakelse(any(), any()) } returns endringsmeldingId
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.randomUUID()
		val deltaker = getDeltaker(deltakerId, deltakerliste.id)
		deltakerRepository.insertOrUpdateDeltaker(deltaker)
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
					),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id)),
				veilederDeltakere = emptyList(),
			),
		)
		val endringsmelding1 = getEndringsmelding(deltakerId).copy(status = Endringsmelding.Status.UTFORT)
		endringsmeldingRepository.insertOrUpdateEndringsmelding(endringsmelding1)
		val endringsmeldingRequest =
			EndringsmeldingRequest(
				innhold =
					EndringsmeldingRequest.Innhold.ForlengDeltakelseInnhold(
						sluttdato = LocalDate.now(),
					),
			)

		endringsmeldingService.opprettEndringsmelding(deltakerId, endringsmeldingRequest, personIdent)

		val endringsmeldinger = endringsmeldingRepository.getEndringsmeldingerForDeltaker(deltakerId)
		endringsmeldinger.size shouldBe 2
		val aktivEndringsmelding = endringsmeldinger.find { it.erAktiv() }
		aktivEndringsmelding?.id shouldBe endringsmeldingId
		aktivEndringsmelding?.type shouldBe EndringsmeldingType.FORLENG_DELTAKELSE
		(aktivEndringsmelding?.innhold as Innhold.ForlengDeltakelseInnhold).sluttdato shouldBe LocalDate.now()
		val utdatertEndringsmelding = endringsmeldinger.find { !it.erAktiv() }
		utdatertEndringsmelding?.id shouldBe endringsmelding1.id
		utdatertEndringsmelding?.status shouldBe Endringsmelding.Status.UTFORT
	}

	@Test
	fun `opprettEndringsmelding - endre sluttaarsak, deltaker har ikke riktig status - feiler`() {
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.randomUUID()
		val deltaker = getDeltaker(deltakerId, deltakerliste.id).copy(status = StatusType.DELTAR)
		deltakerRepository.insertOrUpdateDeltaker(deltaker)
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
					),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id)),
				veilederDeltakere = emptyList(),
			),
		)
		val endringsmeldingRequest =
			EndringsmeldingRequest(
				innhold =
					EndringsmeldingRequest.Innhold.EndreSluttaarsakInnhold(
						DeltakerStatusAarsak(DeltakerStatusAarsak.Type.SYK, ""),
					),
			)

		assertThrows<ValidationException> {
			endringsmeldingService.opprettEndringsmelding(deltakerId, endringsmeldingRequest, personIdent)
		}
	}

	@Test
	fun `opprettEndringsmelding - endre sluttaarsak, deltaker deltar på et kurstiltak - feiler`() {
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId).copy(erKurs = true)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.randomUUID()
		val deltaker = getDeltaker(deltakerId, deltakerliste.id).copy(status = StatusType.HAR_SLUTTET)
		deltakerRepository.insertOrUpdateDeltaker(deltaker)
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
					),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id)),
				veilederDeltakere = emptyList(),
			),
		)
		val endringsmeldingRequest =
			EndringsmeldingRequest(
				innhold =
					EndringsmeldingRequest.Innhold.EndreSluttaarsakInnhold(
						DeltakerStatusAarsak(DeltakerStatusAarsak.Type.SYK, ""),
					),
			)

		assertThrows<ValidationException> {
			endringsmeldingService.opprettEndringsmelding(deltakerId, endringsmeldingRequest, personIdent)
		}
	}

	@Test
	fun `opprettEndringsmelding - endre sluttaarsak - oppretter ny endringsmelding`() {
		val endringsmeldingId = UUID.randomUUID()
		coEvery { amtTiltakClient.endreSluttaarsak(any(), any()) } returns endringsmeldingId
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId).copy(erKurs = false)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.randomUUID()
		val deltaker = getDeltaker(deltakerId, deltakerliste.id).copy(status = StatusType.HAR_SLUTTET)
		deltakerRepository.insertOrUpdateDeltaker(deltaker)
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
					),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id)),
				veilederDeltakere = emptyList(),
			),
		)
		val aarsak = DeltakerStatusAarsak(DeltakerStatusAarsak.Type.SYK, "")

		val endringsmeldingRequest =
			EndringsmeldingRequest(
				innhold = EndringsmeldingRequest.Innhold.EndreSluttaarsakInnhold(aarsak),
			)

		endringsmeldingService.opprettEndringsmelding(deltakerId, endringsmeldingRequest, personIdent)

		val endringsmeldinger = endringsmeldingRepository.getEndringsmeldingerForDeltaker(deltakerId)
		endringsmeldinger.size shouldBe 1
		val endringsmelding = endringsmeldinger.first()
		endringsmelding.id shouldBe endringsmeldingId
		endringsmelding.type shouldBe EndringsmeldingType.ENDRE_SLUTTAARSAK
		(endringsmelding.innhold as Innhold.EndreSluttaarsakInnhold).aarsak shouldBe aarsak
	}
}
