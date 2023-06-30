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
import no.nav.tiltaksarrangor.ingest.model.AnsattRolle
import no.nav.tiltaksarrangor.ingest.model.EndringsmeldingType
import no.nav.tiltaksarrangor.ingest.model.Innhold
import no.nav.tiltaksarrangor.model.Endringsmelding
import no.nav.tiltaksarrangor.model.Veiledertype
import no.nav.tiltaksarrangor.model.exceptions.SkjultDeltakerException
import no.nav.tiltaksarrangor.model.exceptions.UnauthorizedException
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
	private val endringsmeldingService = EndringsmeldingService(
		amtTiltakClient,
		ansattService,
		endringsmeldingRepository,
		deltakerRepository,
		metricsService
	)

	@AfterEach
	internal fun tearDown() {
		DbTestDataUtils.cleanDatabase(dataSource)
		clearMocks(amtTiltakClient)
	}

	@Test
	fun `getAktiveEndringsmeldinger - ansatt har ikke rolle hos arrangor - returnerer unauthorized`() {
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
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id)),
				veilederDeltakere = emptyList()
			)
		)
		val endringsmelding = getEndringsmelding(deltakerId)
		endringsmeldingRepository.insertOrUpdateEndringsmelding(endringsmelding)

		assertThrows<UnauthorizedException> {
			endringsmeldingService.getAktiveEndringsmeldinger(deltakerId, personIdent)
		}
	}

	@Test
	fun `getAktiveEndringsmeldinger - deltaker er skjult - returnerer skjult deltaker exception`() {
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
		val endringsmelding = getEndringsmelding(deltakerId)
		endringsmeldingRepository.insertOrUpdateEndringsmelding(endringsmelding)

		assertThrows<SkjultDeltakerException> {
			endringsmeldingService.getAktiveEndringsmeldinger(deltakerId, personIdent)
		}
	}

	@Test
	fun `getAktiveEndringsmeldinger - ansatt har tilgang - henter endringsmeldinger`() {
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
					AnsattRolleDbo(arrangorId, AnsattRolle.VEILEDER)
				),
				deltakerlister = emptyList(),
				veilederDeltakere = listOf(VeilederDeltakerDbo(deltakerId, Veiledertype.VEILEDER))
			)
		)
		val endringsmeldingDbo1 = getEndringsmelding(deltakerId)
		val endringsmeldingDbo2 = getEndringsmelding(deltakerId).copy(
			type = EndringsmeldingType.ENDRE_DELTAKELSE_PROSENT,
			innhold = Innhold.EndreDeltakelseProsentInnhold(
				nyDeltakelseProsent = 50,
				dagerPerUke = 2,
				gyldigFraDato = LocalDate.now()
			)
		)
		endringsmeldingRepository.insertOrUpdateEndringsmelding(endringsmeldingDbo1)
		endringsmeldingRepository.insertOrUpdateEndringsmelding(endringsmeldingDbo2)

		val endringsmeldinger = endringsmeldingService.getAktiveEndringsmeldinger(deltakerId, personIdent)
		endringsmeldinger.size shouldBe 2
		val endringsmelding1 = endringsmeldinger.find { it.id == endringsmeldingDbo1.id }
		endringsmelding1?.type shouldBe Endringsmelding.Type.FORLENG_DELTAKELSE
		endringsmelding1?.innhold shouldBe Endringsmelding.Innhold.ForlengDeltakelseInnhold(sluttdato = LocalDate.now().plusMonths(2))

		val endringsmelding2 = endringsmeldinger.find { it.id == endringsmeldingDbo2.id }
		endringsmelding2?.type shouldBe Endringsmelding.Type.ENDRE_DELTAKELSE_PROSENT
		endringsmelding2?.innhold shouldBe Endringsmelding.Innhold.EndreDeltakelseProsentInnhold(
			deltakelseProsent = 50,
			dagerPerUke = 2,
			gyldigFraDato = LocalDate.now()
		)
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
				roller = listOf(
					AnsattRolleDbo(UUID.randomUUID(), AnsattRolle.KOORDINATOR)
				),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id)),
				veilederDeltakere = emptyList()
			)
		)
		val endringsmelding = getEndringsmelding(deltakerId)
		endringsmeldingRepository.insertOrUpdateEndringsmelding(endringsmelding)

		assertThrows<UnauthorizedException> {
			endringsmeldingService.slettEndringsmelding(endringsmelding.id, personIdent)
		}
	}

	@Test
	fun `slettEndringsmelding - ansatt har tilgang - sletter endringsmelding`() {
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
				roller = listOf(
					AnsattRolleDbo(arrangorId, AnsattRolle.KOORDINATOR)
				),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id)),
				veilederDeltakere = emptyList()
			)
		)
		val endringsmelding = getEndringsmelding(deltakerId)
		endringsmeldingRepository.insertOrUpdateEndringsmelding(endringsmelding)

		endringsmeldingService.slettEndringsmelding(endringsmelding.id, personIdent)

		endringsmeldingRepository.getEndringsmelding(endringsmelding.id) shouldBe null
		coVerify { amtTiltakClient.tilbakekallEndringsmelding(endringsmelding.id) }
	}
}
