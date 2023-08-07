package no.nav.tiltaksarrangor.ingest.jobs

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import no.nav.tiltaksarrangor.IntegrationTest
import no.nav.tiltaksarrangor.ingest.jobs.leaderelection.LeaderElection
import no.nav.tiltaksarrangor.ingest.model.AnsattRolle
import no.nav.tiltaksarrangor.ingest.model.DeltakerlisteStatus
import no.nav.tiltaksarrangor.ingest.model.EndringsmeldingType
import no.nav.tiltaksarrangor.ingest.model.Innhold
import no.nav.tiltaksarrangor.model.StatusType
import no.nav.tiltaksarrangor.model.Veiledertype
import no.nav.tiltaksarrangor.repositories.AnsattRepository
import no.nav.tiltaksarrangor.repositories.DeltakerRepository
import no.nav.tiltaksarrangor.repositories.DeltakerlisteRepository
import no.nav.tiltaksarrangor.repositories.EndringsmeldingRepository
import no.nav.tiltaksarrangor.repositories.model.AnsattDbo
import no.nav.tiltaksarrangor.repositories.model.AnsattRolleDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerlisteDbo
import no.nav.tiltaksarrangor.repositories.model.EndringsmeldingDbo
import no.nav.tiltaksarrangor.repositories.model.KoordinatorDeltakerlisteDbo
import no.nav.tiltaksarrangor.repositories.model.VeilederDeltakerDbo
import no.nav.tiltaksarrangor.testutils.DbTestDataUtils
import no.nav.tiltaksarrangor.testutils.SingletonPostgresContainer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class RyddejobbTest : IntegrationTest() {
	private val dataSource = SingletonPostgresContainer.getDataSource()
	private val template = NamedParameterJdbcTemplate(dataSource)
	private val deltakerRepository = DeltakerRepository(template)
	private val deltakerlisteRepository = DeltakerlisteRepository(template, deltakerRepository)
	private val ansattRepository = AnsattRepository(template)
	private val endringsmeldingRepository = EndringsmeldingRepository(template)
	private val leaderElection = mockk<LeaderElection>()
	private val ryddejobb = Ryddejobb(leaderElection, deltakerlisteRepository, deltakerRepository)

	@BeforeEach
	internal fun setUp() {
		every { leaderElection.isLeader() } returns true
	}

	@AfterEach
	internal fun tearDown() {
		DbTestDataUtils.cleanDatabase(dataSource)
	}

	@Test
	fun `slettUtdaterteDeltakerlisterOgDeltakere - deltakerliste avsluttet for 16 dager siden - sletter deltakerliste og deltaker`() {
		val deltakerlisteId = UUID.randomUUID()
		val deltakerId = UUID.randomUUID()
		val ansattId = UUID.randomUUID()
		deltakerlisteRepository.insertOrUpdateDeltakerliste(getDeltakerliste(deltakerlisteId, DeltakerlisteStatus.AVSLUTTET, LocalDate.now().minusDays(16)))
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId, deltakerlisteId, StatusType.DELTAR, LocalDateTime.now().minusDays(16)))
		ansattRepository.insertOrUpdateAnsatt(getAnsatt(ansattId, deltakerlisteId, deltakerId))

		ryddejobb.slettUtdaterteDeltakerlisterOgDeltakere()

		deltakerlisteRepository.getDeltakerliste(deltakerlisteId) shouldBe null
		deltakerRepository.getDeltaker(deltakerId) shouldBe null
		ansattRepository.getKoordinatorDeltakerlisteDboListe(ansattId).size shouldBe 0
		ansattRepository.getVeilederDeltakerDboListe(ansattId).size shouldBe 0
	}

	@Test
	fun `slettUtdaterteDeltakerlisterOgDeltakere - deltakerliste avsluttet for 12 dager siden - sletter ikke deltakerliste`() {
		val deltakerlisteId = UUID.randomUUID()
		deltakerlisteRepository.insertOrUpdateDeltakerliste(getDeltakerliste(deltakerlisteId, DeltakerlisteStatus.AVSLUTTET, LocalDate.now().minusDays(12)))

		ryddejobb.slettUtdaterteDeltakerlisterOgDeltakere()

		deltakerlisteRepository.getDeltakerliste(deltakerlisteId) shouldNotBe null
	}

	@Test
	fun `slettUtdaterteDeltakerlisterOgDeltakere - deltaker har sluttet for 16 dager siden - sletter deltaker`() {
		val deltakerlisteId = UUID.randomUUID()
		val deltakerId = UUID.randomUUID()
		val ansattId = UUID.randomUUID()
		val endringsmeldingId = UUID.randomUUID()
		deltakerlisteRepository.insertOrUpdateDeltakerliste(getDeltakerliste(deltakerlisteId, DeltakerlisteStatus.GJENNOMFORES, LocalDate.now().plusWeeks(3)))
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId, deltakerlisteId, StatusType.HAR_SLUTTET, LocalDateTime.now().minusDays(16)))
		ansattRepository.insertOrUpdateAnsatt(getAnsatt(ansattId, deltakerlisteId, deltakerId))
		endringsmeldingRepository.insertOrUpdateEndringsmelding(getEndringsmelding(endringsmeldingId, deltakerId))

		ryddejobb.slettUtdaterteDeltakerlisterOgDeltakere()

		deltakerlisteRepository.getDeltakerliste(deltakerlisteId) shouldNotBe null
		deltakerRepository.getDeltaker(deltakerId) shouldBe null
		ansattRepository.getKoordinatorDeltakerlisteDboListe(ansattId).size shouldBe 1
		ansattRepository.getVeilederDeltakerDboListe(ansattId).size shouldBe 0
		endringsmeldingRepository.getEndringsmelding(endringsmeldingId) shouldBe null
	}

	@Test
	fun `slettUtdaterteDeltakerlisterOgDeltakere - deltaker har sluttet for 12 dager siden - sletter ikke deltaker`() {
		val deltakerlisteId = UUID.randomUUID()
		val deltakerId = UUID.randomUUID()
		deltakerlisteRepository.insertOrUpdateDeltakerliste(getDeltakerliste(deltakerlisteId, DeltakerlisteStatus.GJENNOMFORES, LocalDate.now().plusWeeks(3)))
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId, deltakerlisteId, StatusType.HAR_SLUTTET, LocalDateTime.now().minusDays(12)))

		ryddejobb.slettUtdaterteDeltakerlisterOgDeltakere()

		deltakerlisteRepository.getDeltakerliste(deltakerlisteId) shouldNotBe null
		deltakerRepository.getDeltaker(deltakerId) shouldNotBe null
	}

	@Test
	fun `slettUtdaterteDeltakerlisterOgDeltakere - deltaker er skjult - sletter deltaker`() {
		val deltakerlisteId = UUID.randomUUID()
		val deltakerId = UUID.randomUUID()
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId, deltakerlisteId, StatusType.HAR_SLUTTET, LocalDateTime.now().minusDays(12), erSkjult = true))

		ryddejobb.slettUtdaterteDeltakerlisterOgDeltakere()

		deltakerRepository.getDeltaker(deltakerId) shouldBe null
	}

	@Test
	fun `slettUtdaterteDeltakerlisterOgDeltakere - ingenting skal slettes - sletter ingenting`() {
		val deltakerlisteId = UUID.randomUUID()
		val deltakerId = UUID.randomUUID()
		deltakerlisteRepository.insertOrUpdateDeltakerliste(getDeltakerliste(deltakerlisteId, DeltakerlisteStatus.GJENNOMFORES, LocalDate.now().plusWeeks(3)))
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId, deltakerlisteId, StatusType.DELTAR, LocalDateTime.now()))

		ryddejobb.slettUtdaterteDeltakerlisterOgDeltakere()

		deltakerlisteRepository.getDeltakerliste(deltakerlisteId) shouldNotBe null
		deltakerRepository.getDeltaker(deltakerId) shouldNotBe null
	}
}

private fun getDeltakerliste(
	deltakerlisteId: UUID,
	status: DeltakerlisteStatus,
	sluttdato: LocalDate
): DeltakerlisteDbo {
	return DeltakerlisteDbo(
		id = deltakerlisteId,
		navn = "Gjennomføring av tiltak",
		status = status,
		startDato = LocalDate.now().minusYears(2),
		sluttDato = sluttdato,
		erKurs = false,
		arrangorId = UUID.randomUUID(),
		_tiltakNavn = "Tiltak",
		tiltakType = "AMO"
	)
}

private fun getDeltaker(
	deltakerId: UUID,
	deltakerlisteId: UUID,
	status: StatusType,
	statusGyldigFraDato: LocalDateTime,
	erSkjult: Boolean = false
): DeltakerDbo {
	return DeltakerDbo(
		id = deltakerId,
		deltakerlisteId = deltakerlisteId,
		personident = "10987654321",
		fornavn = "Fornavn",
		mellomnavn = null,
		etternavn = "Etternavn",
		telefonnummer = "98989898",
		epost = "epost@nav.no",
		erSkjermet = false,
		status = status,
		statusOpprettetDato = LocalDateTime.now().minusWeeks(6),
		statusGyldigFraDato = statusGyldigFraDato,
		dagerPerUke = null,
		prosentStilling = null,
		startdato = LocalDate.now().minusWeeks(5),
		sluttdato = null,
		innsoktDato = LocalDate.now().minusMonths(2),
		bestillingstekst = "Bestilling",
		navKontor = "NAV Oslo",
		navVeilederId = UUID.randomUUID(),
		navVeilederNavn = "Per Veileder",
		navVeilederEpost = null,
		navVeilederTelefon = null,
		skjultAvAnsattId = if (erSkjult) UUID.randomUUID() else null,
		skjultDato = if (erSkjult) LocalDateTime.now() else null
	)
}

private fun getAnsatt(ansattId: UUID, deltakerlisteId: UUID, deltakerId: UUID): AnsattDbo {
	return AnsattDbo(
		id = ansattId,
		personIdent = "12345678910",
		fornavn = "Fornavn",
		mellomnavn = null,
		etternavn = "Etternavn",
		roller = listOf(
			AnsattRolleDbo(UUID.randomUUID(), AnsattRolle.KOORDINATOR),
			AnsattRolleDbo(UUID.randomUUID(), AnsattRolle.VEILEDER)
		),
		deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerlisteId)),
		veilederDeltakere = listOf(VeilederDeltakerDbo(deltakerId, Veiledertype.VEILEDER))
	)
}

private fun getEndringsmelding(endringsmeldingId: UUID, deltakerId: UUID): EndringsmeldingDbo {
	return EndringsmeldingDbo(
		id = endringsmeldingId,
		deltakerId = deltakerId,
		type = EndringsmeldingType.ENDRE_SLUTTDATO,
		innhold = Innhold.EndreSluttdatoInnhold(sluttdato = LocalDate.now().plusWeeks(3))
	)
}
