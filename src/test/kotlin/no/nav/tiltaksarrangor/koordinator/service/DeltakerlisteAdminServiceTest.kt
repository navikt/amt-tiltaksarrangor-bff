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
import no.nav.tiltaksarrangor.ingest.model.AnsattRolle
import no.nav.tiltaksarrangor.model.DeltakerlisteStatus
import no.nav.tiltaksarrangor.model.exceptions.UnauthorizedException
import no.nav.tiltaksarrangor.repositories.AnsattRepository
import no.nav.tiltaksarrangor.repositories.ArrangorRepository
import no.nav.tiltaksarrangor.repositories.DeltakerRepository
import no.nav.tiltaksarrangor.repositories.DeltakerlisteRepository
import no.nav.tiltaksarrangor.repositories.model.AnsattDbo
import no.nav.tiltaksarrangor.repositories.model.AnsattRolleDbo
import no.nav.tiltaksarrangor.repositories.model.ArrangorDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerlisteDbo
import no.nav.tiltaksarrangor.repositories.model.KoordinatorDeltakerlisteDbo
import no.nav.tiltaksarrangor.service.AnsattService
import no.nav.tiltaksarrangor.service.MetricsService
import no.nav.tiltaksarrangor.testutils.DbTestDataUtils
import no.nav.tiltaksarrangor.testutils.SingletonPostgresContainer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDate
import java.util.UUID

class DeltakerlisteAdminServiceTest {
	private val amtArrangorClient = mockk<AmtArrangorClient>()
	private val metricsService = mockk<MetricsService>(relaxed = true)
	private val dataSource = SingletonPostgresContainer.getDataSource()
	private val template = NamedParameterJdbcTemplate(dataSource)
	private val ansattRepository = AnsattRepository(template)
	private val ansattService = AnsattService(amtArrangorClient, ansattRepository)
	private val deltakerRepository = DeltakerRepository(template)
	private val deltakerlisteRepository = DeltakerlisteRepository(template, deltakerRepository)
	private val arrangorRepository = ArrangorRepository(template)
	private val deltakerlisteAdminService =
		DeltakerlisteAdminService(ansattService, deltakerlisteRepository, arrangorRepository, metricsService)

	@AfterEach
	internal fun tearDown() {
		DbTestDataUtils.cleanDatabase(dataSource)
		clearMocks(amtArrangorClient)
	}

	@Test
	fun `getAlleDeltakerlister - ansatt er ikke koordinator - returnerer unauthorized`() {
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		arrangorRepository.insertOrUpdateArrangor(
			ArrangorDbo(
				id = arrangorId,
				navn = "Arrangør AS",
				organisasjonsnummer = "88888888",
				overordnetArrangorId = null,
			),
		)
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.randomUUID(),
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller = listOf(AnsattRolleDbo(arrangorId, AnsattRolle.VEILEDER)),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(UUID.randomUUID())),
				veilederDeltakere = emptyList(),
			),
		)

		assertThrows<UnauthorizedException> {
			deltakerlisteAdminService.getAlleDeltakerlister(personIdent)
		}
	}

	@Test
	fun `getAlleDeltakerlister - ansatt er koordinator for flere arrangorer - returnerer deltakerlister`() {
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		arrangorRepository.insertOrUpdateArrangor(
			ArrangorDbo(
				id = arrangorId,
				navn = "Arrangør AS",
				organisasjonsnummer = "88888888",
				overordnetArrangorId = null,
			),
		)
		val overordnetArrangorId = UUID.randomUUID()
		arrangorRepository.insertOrUpdateArrangor(
			ArrangorDbo(
				id = overordnetArrangorId,
				navn = "Overordnet arrangør AS",
				organisasjonsnummer = "777777777",
				overordnetArrangorId = null,
			),
		)
		val arrangorId2 = UUID.randomUUID()
		arrangorRepository.insertOrUpdateArrangor(
			ArrangorDbo(
				id = arrangorId2,
				navn = "Annen arrangør AS",
				organisasjonsnummer = "99999999",
				overordnetArrangorId = overordnetArrangorId,
			),
		)
		val deltakerliste1 =
			DeltakerlisteDbo(
				id = UUID.randomUUID(),
				navn = "Gjennomføring 1",
				status = DeltakerlisteStatus.GJENNOMFORES,
				arrangorId = arrangorId,
				tiltakNavn = "Navn på tiltak",
				tiltakType = "ARBFORB",
				startDato = LocalDate.of(2023, 2, 1),
				sluttDato = null,
				erKurs = false,
			)
		val deltakerliste2 =
			DeltakerlisteDbo(
				id = UUID.randomUUID(),
				navn = "Gjennomføring 2",
				status = DeltakerlisteStatus.GJENNOMFORES,
				arrangorId = arrangorId2,
				tiltakNavn = "Annet tiltak",
				tiltakType = "INDOPPFAG",
				startDato = LocalDate.of(2023, 5, 1),
				sluttDato = LocalDate.of(2023, 6, 1),
				erKurs = false,
			)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste1)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste2)
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.randomUUID(),
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller = listOf(AnsattRolleDbo(arrangorId, AnsattRolle.KOORDINATOR), AnsattRolleDbo(arrangorId2, AnsattRolle.KOORDINATOR)),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste1.id)),
				veilederDeltakere = emptyList(),
			),
		)

		val adminDeltakerlister = deltakerlisteAdminService.getAlleDeltakerlister(personIdent)

		adminDeltakerlister.size shouldBe 2

		val adminDeltakerliste1 = adminDeltakerlister.find { it.id == deltakerliste1.id }
		adminDeltakerliste1?.navn shouldBe "Gjennomføring 1"
		adminDeltakerliste1?.tiltaksnavn shouldBe "Navn på tiltak"
		adminDeltakerliste1?.arrangorNavn shouldBe "Arrangør AS"
		adminDeltakerliste1?.arrangorOrgnummer shouldBe "88888888"
		adminDeltakerliste1?.arrangorParentNavn shouldBe "Arrangør AS"
		adminDeltakerliste1?.startDato shouldBe LocalDate.of(2023, 2, 1)
		adminDeltakerliste1?.sluttDato shouldBe null
		adminDeltakerliste1?.lagtTil shouldBe true

		val adminDeltakerliste2 = adminDeltakerlister.find { it.id == deltakerliste2.id }
		adminDeltakerliste2?.navn shouldBe "Gjennomføring 2"
		adminDeltakerliste2?.tiltaksnavn shouldBe "Annet tiltak"
		adminDeltakerliste2?.arrangorNavn shouldBe "Overordnet arrangør AS"
		adminDeltakerliste2?.arrangorOrgnummer shouldBe "99999999"
		adminDeltakerliste2?.arrangorParentNavn shouldBe "Annen arrangør AS"
		adminDeltakerliste2?.startDato shouldBe LocalDate.of(2023, 5, 1)
		adminDeltakerliste2?.sluttDato shouldBe LocalDate.of(2023, 6, 1)
		adminDeltakerliste2?.lagtTil shouldBe false
	}

	@Test
	fun `leggTilDeltakerliste - ansatt er ikke koordinator hos arrangor - returnerer unauthorized`() {
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.randomUUID(),
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller = listOf(AnsattRolleDbo(arrangorId, AnsattRolle.KOORDINATOR)),
				deltakerlister = emptyList(),
				veilederDeltakere = emptyList(),
			),
		)
		val deltakerliste =
			DeltakerlisteDbo(
				id = UUID.randomUUID(),
				navn = "Gjennomføring 1",
				status = DeltakerlisteStatus.GJENNOMFORES,
				arrangorId = UUID.randomUUID(),
				tiltakNavn = "Navn på tiltak",
				tiltakType = "ARBFORB",
				startDato = LocalDate.of(2023, 2, 1),
				sluttDato = null,
				erKurs = false,
			)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)

		assertThrows<UnauthorizedException> {
			deltakerlisteAdminService.leggTilDeltakerliste(deltakerliste.id, personIdent)
		}
	}

	@Test
	fun `leggTilDeltakerliste - ansatt har allerede lagt til deltakerliste - endrer ignenting`() {
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste =
			DeltakerlisteDbo(
				id = UUID.randomUUID(),
				navn = "Gjennomføring 1",
				status = DeltakerlisteStatus.GJENNOMFORES,
				arrangorId = arrangorId,
				tiltakNavn = "Navn på tiltak",
				tiltakType = "ARBFORB",
				startDato = LocalDate.of(2023, 2, 1),
				sluttDato = null,
				erKurs = false,
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
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id)),
				veilederDeltakere = emptyList(),
			),
		)

		deltakerlisteAdminService.leggTilDeltakerliste(deltakerliste.id, personIdent)

		coVerify(exactly = 0) { amtArrangorClient.leggTilDeltakerlisteForKoordinator(any(), any(), any()) }
	}

	@Test
	fun `leggTilDeltakerliste - ansatt har tilgang til deltakerliste - deltakerliste legges til`() {
		coEvery { amtArrangorClient.leggTilDeltakerlisteForKoordinator(any(), any(), any()) } just Runs

		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste =
			DeltakerlisteDbo(
				id = UUID.randomUUID(),
				navn = "Gjennomføring 1",
				status = DeltakerlisteStatus.GJENNOMFORES,
				arrangorId = arrangorId,
				tiltakNavn = "Navn på tiltak",
				tiltakType = "ARBFORB",
				startDato = LocalDate.of(2023, 2, 1),
				sluttDato = null,
				erKurs = false,
			)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val ansattId = UUID.randomUUID()
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = ansattId,
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller = listOf(AnsattRolleDbo(arrangorId, AnsattRolle.KOORDINATOR)),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(UUID.randomUUID())),
				veilederDeltakere = emptyList(),
			),
		)

		deltakerlisteAdminService.leggTilDeltakerliste(deltakerliste.id, personIdent)

		val ansattFraDb = ansattRepository.getAnsatt(ansattId)
		ansattFraDb?.deltakerlister?.size shouldBe 2
		ansattFraDb?.deltakerlister?.find { it.deltakerlisteId == deltakerliste.id } shouldNotBe null

		coVerify(exactly = 1) { amtArrangorClient.leggTilDeltakerlisteForKoordinator(ansattId, deltakerliste.id, arrangorId) }
	}

	@Test
	fun `fjernDeltakerliste - ansatt er ikke koordinator hos arrangor - returnerer unauthorized`() {
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.randomUUID(),
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller = listOf(AnsattRolleDbo(arrangorId, AnsattRolle.KOORDINATOR)),
				deltakerlister = emptyList(),
				veilederDeltakere = emptyList(),
			),
		)
		val deltakerliste =
			DeltakerlisteDbo(
				id = UUID.randomUUID(),
				navn = "Gjennomføring 1",
				status = DeltakerlisteStatus.GJENNOMFORES,
				arrangorId = UUID.randomUUID(),
				tiltakNavn = "Navn på tiltak",
				tiltakType = "ARBFORB",
				startDato = LocalDate.of(2023, 2, 1),
				sluttDato = null,
				erKurs = false,
			)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)

		assertThrows<UnauthorizedException> {
			deltakerlisteAdminService.fjernDeltakerliste(deltakerliste.id, personIdent)
		}
	}

	@Test
	fun `fjernDeltakerliste - ansatt har ikke lagt til deltakerliste - endrer ignenting`() {
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste =
			DeltakerlisteDbo(
				id = UUID.randomUUID(),
				navn = "Gjennomføring 1",
				status = DeltakerlisteStatus.GJENNOMFORES,
				arrangorId = arrangorId,
				tiltakNavn = "Navn på tiltak",
				tiltakType = "ARBFORB",
				startDato = LocalDate.of(2023, 2, 1),
				sluttDato = null,
				erKurs = false,
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
				deltakerlister = emptyList(),
				veilederDeltakere = emptyList(),
			),
		)

		deltakerlisteAdminService.fjernDeltakerliste(deltakerliste.id, personIdent)

		coVerify(exactly = 0) { amtArrangorClient.fjernDeltakerlisteForKoordinator(any(), any(), any()) }
	}

	@Test
	fun `fjernDeltakerliste - ansatt har lagt til deltakerliste - deltakerliste fjernes`() {
		coEvery { amtArrangorClient.fjernDeltakerlisteForKoordinator(any(), any(), any()) } just Runs

		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste =
			DeltakerlisteDbo(
				id = UUID.randomUUID(),
				navn = "Gjennomføring 1",
				status = DeltakerlisteStatus.GJENNOMFORES,
				arrangorId = arrangorId,
				tiltakNavn = "Navn på tiltak",
				tiltakType = "ARBFORB",
				startDato = LocalDate.of(2023, 2, 1),
				sluttDato = null,
				erKurs = false,
			)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val ansattId = UUID.randomUUID()
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = ansattId,
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller = listOf(AnsattRolleDbo(arrangorId, AnsattRolle.KOORDINATOR)),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(UUID.randomUUID()), KoordinatorDeltakerlisteDbo(deltakerliste.id)),
				veilederDeltakere = emptyList(),
			),
		)

		deltakerlisteAdminService.fjernDeltakerliste(deltakerliste.id, personIdent)

		val ansattFraDb = ansattRepository.getAnsatt(ansattId)
		ansattFraDb?.deltakerlister?.size shouldBe 1
		ansattFraDb?.deltakerlister?.find { it.deltakerlisteId == deltakerliste.id } shouldBe null

		coVerify(exactly = 1) { amtArrangorClient.fjernDeltakerlisteForKoordinator(ansattId, deltakerliste.id, arrangorId) }
	}
}
