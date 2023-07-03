package no.nav.tiltaksarrangor.service

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import no.nav.tiltaksarrangor.client.amtarrangor.AmtArrangorClient
import no.nav.tiltaksarrangor.client.amttiltak.AmtTiltakClient
import no.nav.tiltaksarrangor.ingest.model.AnsattRolle
import no.nav.tiltaksarrangor.ingest.model.EndringsmeldingType
import no.nav.tiltaksarrangor.ingest.model.Innhold
import no.nav.tiltaksarrangor.model.Endringsmelding
import no.nav.tiltaksarrangor.model.StatusType
import no.nav.tiltaksarrangor.model.Veiledertype
import no.nav.tiltaksarrangor.model.exceptions.SkjultDeltakerException
import no.nav.tiltaksarrangor.model.exceptions.UnauthorizedException
import no.nav.tiltaksarrangor.repositories.AnsattRepository
import no.nav.tiltaksarrangor.repositories.DeltakerRepository
import no.nav.tiltaksarrangor.repositories.DeltakerlisteRepository
import no.nav.tiltaksarrangor.repositories.EndringsmeldingRepository
import no.nav.tiltaksarrangor.repositories.model.AnsattDbo
import no.nav.tiltaksarrangor.repositories.model.AnsattRolleDbo
import no.nav.tiltaksarrangor.repositories.model.EndringsmeldingDbo
import no.nav.tiltaksarrangor.repositories.model.KoordinatorDeltakerlisteDbo
import no.nav.tiltaksarrangor.repositories.model.VeilederDeltakerDbo
import no.nav.tiltaksarrangor.testutils.DbTestDataUtils
import no.nav.tiltaksarrangor.testutils.SingletonPostgresContainer
import no.nav.tiltaksarrangor.testutils.getDeltaker
import no.nav.tiltaksarrangor.testutils.getDeltakerliste
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class TiltaksarrangorServiceTest {
	private val amtTiltakClient = mockk<AmtTiltakClient>()
	private val amtArrangorClient = mockk<AmtArrangorClient>()
	private val metricsService = mockk<MetricsService>(relaxed = true)
	private val auditLoggerService = mockk<AuditLoggerService>(relaxed = true)
	private val dataSource = SingletonPostgresContainer.getDataSource()
	private val template = NamedParameterJdbcTemplate(dataSource)
	private val ansattRepository = AnsattRepository(template)
	private val ansattService = AnsattService(amtArrangorClient, ansattRepository)
	private val deltakerRepository = DeltakerRepository(template)
	private val deltakerlisteRepository = DeltakerlisteRepository(template, deltakerRepository)
	private val endringsmeldingRepository = EndringsmeldingRepository(template)
	private val tiltaksarrangorService = TiltaksarrangorService(
		amtTiltakClient,
		ansattService,
		metricsService,
		deltakerRepository,
		endringsmeldingRepository,
		auditLoggerService
	)

	@AfterEach
	internal fun tearDown() {
		DbTestDataUtils.cleanDatabase(dataSource)
		clearMocks(auditLoggerService, amtTiltakClient)
	}

	@Test
	fun `getDeltaker - ansatt har ikke rolle hos arrangor - returnerer unauthorized`() {
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
				roller = listOf(
					AnsattRolleDbo(UUID.randomUUID(), AnsattRolle.KOORDINATOR)
				),
				deltakerlister = emptyList(),
				veilederDeltakere = emptyList()
			)
		)

		assertThrows<UnauthorizedException> {
			tiltaksarrangorService.getDeltaker(personIdent, deltakerId)
		}
	}

	@Test
	fun `getDeltaker - deltaker er skjult - returnerer skjult deltaker exception`() {
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
				roller = listOf(
					AnsattRolleDbo(arrangorId, AnsattRolle.KOORDINATOR)
				),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id)),
				veilederDeltakere = emptyList()
			)
		)

		assertThrows<SkjultDeltakerException> {
			tiltaksarrangorService.getDeltaker(personIdent, deltakerId)
		}
	}

	@Test
	fun `getDeltaker - deltaker finnes og ansatt har tilgang - returnerer deltaker`() {
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
				roller = listOf(
					AnsattRolleDbo(arrangorId, AnsattRolle.KOORDINATOR)
				),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id)),
				veilederDeltakere = emptyList()
			)
		)

		val deltaker = tiltaksarrangorService.getDeltaker(personIdent, deltakerId)

		deltaker.id shouldBe deltakerId
		deltaker.deltakerliste.id shouldBe deltakerliste.id
		deltaker.dagerPerUke shouldBe null
		deltaker.soktInnPa shouldBe deltakerliste.navn
		deltaker.tiltakskode shouldBe deltakerliste.tiltakType
		deltaker.aktiveEndringsmeldinger.size shouldBe 0
		deltaker.veiledere.size shouldBe 0
	}

	@Test
	fun `getDeltaker - deltaker som har veiledere og endringsmeldinger finnes og ansatt har tilgang - returnerer deltaker`() {
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.randomUUID()
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId, deltakerliste.id))
		endringsmeldingRepository.insertOrUpdateEndringsmelding(
			EndringsmeldingDbo(
				id = UUID.randomUUID(),
				deltakerId = deltakerId,
				type = EndringsmeldingType.ENDRE_SLUTTDATO,
				innhold = Innhold.EndreSluttdatoInnhold(sluttdato = LocalDate.now())
			)
		)
		val veilederId = UUID.randomUUID()
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = veilederId,
				personIdent = UUID.randomUUID().toString(),
				fornavn = "Vei",
				mellomnavn = null,
				etternavn = "Leder",
				roller = listOf(
					AnsattRolleDbo(arrangorId, AnsattRolle.VEILEDER)
				),
				deltakerlister = emptyList(),
				veilederDeltakere = listOf(VeilederDeltakerDbo(deltakerId, Veiledertype.VEILEDER))
			)
		)
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.randomUUID(),
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller = listOf(
					AnsattRolleDbo(arrangorId, AnsattRolle.KOORDINATOR)
				),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id)),
				veilederDeltakere = emptyList()
			)
		)

		val deltaker = tiltaksarrangorService.getDeltaker(personIdent, deltakerId)

		deltaker.id shouldBe deltakerId
		deltaker.deltakerliste.id shouldBe deltakerliste.id
		deltaker.dagerPerUke shouldBe null
		deltaker.soktInnPa shouldBe deltakerliste.navn
		deltaker.tiltakskode shouldBe deltakerliste.tiltakType
		deltaker.aktiveEndringsmeldinger.size shouldBe 1
		val endringsmelding = deltaker.aktiveEndringsmeldinger.first()
		endringsmelding.type shouldBe Endringsmelding.Type.ENDRE_SLUTTDATO
		val innhold = endringsmelding.innhold as Endringsmelding.Innhold.EndreSluttdatoInnhold
		innhold.sluttdato shouldBe LocalDate.now()
		deltaker.veiledere.size shouldBe 1
		val veileder = deltaker.veiledere.first()
		veileder.ansattId shouldBe veilederId
		veileder.veiledertype shouldBe Veiledertype.VEILEDER
	}

	@Test
	fun `fjernDeltaker - ansatt har ikke rolle hos arrangor - returnerer unauthorized`() {
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
				roller = listOf(
					AnsattRolleDbo(UUID.randomUUID(), AnsattRolle.KOORDINATOR)
				),
				deltakerlister = emptyList(),
				veilederDeltakere = emptyList()
			)
		)

		assertThrows<UnauthorizedException> {
			tiltaksarrangorService.fjernDeltaker(personIdent, deltakerId)
		}
	}

	@Test
	fun `fjernDeltaker - deltaker er ikke aktuell og ansatt har tilgang - skjuler deltaker`() {
		coEvery { amtTiltakClient.skjulDeltakerForTiltaksarrangor(any()) } just Runs
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.randomUUID()
		val deltaker = getDeltaker(deltakerId, deltakerliste.id).copy(status = StatusType.IKKE_AKTUELL)
		deltakerRepository.insertOrUpdateDeltaker(deltaker)
		val ansattId = UUID.randomUUID()
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = ansattId,
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller = listOf(
					AnsattRolleDbo(arrangorId, AnsattRolle.KOORDINATOR)
				),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id)),
				veilederDeltakere = emptyList()
			)
		)

		tiltaksarrangorService.fjernDeltaker(personIdent, deltakerId)

		val deltakerFraDb = deltakerRepository.getDeltaker(deltakerId)
		deltakerFraDb?.skjultAvAnsattId shouldBe ansattId
		deltakerFraDb?.skjultDato shouldNotBe null
	}

	@Test
	fun `fjernDeltaker - deltaker venter pa oppstart og ansatt har tilgang - returnerer illegal state exception`() {
		coEvery { amtTiltakClient.skjulDeltakerForTiltaksarrangor(any()) } just Runs
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.randomUUID()
		val deltaker = getDeltaker(deltakerId, deltakerliste.id).copy(status = StatusType.VENTER_PA_OPPSTART)
		deltakerRepository.insertOrUpdateDeltaker(deltaker)
		val ansattId = UUID.randomUUID()
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = ansattId,
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller = listOf(
					AnsattRolleDbo(arrangorId, AnsattRolle.KOORDINATOR)
				),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id)),
				veilederDeltakere = emptyList()
			)
		)

		assertThrows<IllegalStateException> {
			tiltaksarrangorService.fjernDeltaker(personIdent, deltakerId)
		}

		val deltakerFraDb = deltakerRepository.getDeltaker(deltakerId)
		deltakerFraDb?.skjultAvAnsattId shouldBe null
		deltakerFraDb?.skjultDato shouldBe null
	}

	@Test
	fun `fjernDeltaker - deltaker kan skjules men amt-tiltak feiler - returnerer feil og lagrer ingenting`() {
		coEvery { amtTiltakClient.skjulDeltakerForTiltaksarrangor(any()) } throws RuntimeException("Noe gikk galt")
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.randomUUID()
		val deltaker = getDeltaker(deltakerId, deltakerliste.id).copy(status = StatusType.HAR_SLUTTET)
		deltakerRepository.insertOrUpdateDeltaker(deltaker)
		val ansattId = UUID.randomUUID()
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = ansattId,
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller = listOf(
					AnsattRolleDbo(arrangorId, AnsattRolle.KOORDINATOR)
				),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id)),
				veilederDeltakere = emptyList()
			)
		)

		assertThrows<RuntimeException> {
			tiltaksarrangorService.fjernDeltaker(personIdent, deltakerId)
		}

		val deltakerFraDb = deltakerRepository.getDeltaker(deltakerId)
		deltakerFraDb?.skjultAvAnsattId shouldBe null
		deltakerFraDb?.skjultDato shouldBe null
	}
}
