package no.nav.tiltaksarrangor.consumer.jobs

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import no.nav.tiltaksarrangor.IntegrationTest
import no.nav.tiltaksarrangor.consumer.jobs.leaderelection.LeaderElection
import no.nav.tiltaksarrangor.model.DeltakerlisteStatus
import no.nav.tiltaksarrangor.model.StatusType
import no.nav.tiltaksarrangor.repositories.AnsattRepository
import no.nav.tiltaksarrangor.repositories.DeltakerRepository
import no.nav.tiltaksarrangor.repositories.DeltakerlisteRepository
import no.nav.tiltaksarrangor.repositories.EndringsmeldingRepository
import no.nav.tiltaksarrangor.testutils.DbTestDataUtils
import no.nav.tiltaksarrangor.testutils.DeltakerContext
import no.nav.tiltaksarrangor.testutils.SingletonPostgresContainer
import no.nav.tiltaksarrangor.testutils.getDeltakerliste
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDate
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
	fun `slettUtdaterteDeltakerlisterOgDeltakere - deltakerliste avsluttet for 42 dager siden - sletter deltakerliste og deltaker`() {
		with(DeltakerContext()) {
			deltakerlisteRepository.insertOrUpdateDeltakerliste(
				deltakerliste.copy(
					status = DeltakerlisteStatus.AVSLUTTET,
					sluttDato = LocalDate.now().minusDays(42),
				),
			)
			ryddejobb.slettUtdaterteDeltakerlisterOgDeltakere()

			deltakerlisteRepository.getDeltakerliste(deltakerliste.id) shouldBe null
			deltakerRepository.getDeltaker(deltaker.id) shouldBe null
			ansattRepository.getKoordinatorDeltakerlisteDboListe(koordinator.id).size shouldBe 0
			ansattRepository.getVeilederDeltakerDboListe(veileder.id).size shouldBe 0
		}
	}

	@Test
	fun `slettUtdaterteDeltakerlisterOgDeltakere - deltakerliste avsluttet for 38 dager siden - sletter ikke deltakerliste`() {
		val deltakerliste = getDeltakerliste(UUID.randomUUID()).copy(
			status = DeltakerlisteStatus.AVSLUTTET,
			sluttDato = LocalDate.now().minusDays(38),
		)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)

		ryddejobb.slettUtdaterteDeltakerlisterOgDeltakere()

		deltakerlisteRepository.getDeltakerliste(deltakerliste.id) shouldNotBe null
	}

	@Test
	fun `slettUtdaterteDeltakerlisterOgDeltakere - deltaker har sluttet for 42 dager siden - sletter deltaker`() {
		with(DeltakerContext()) {
			medStatus(StatusType.HAR_SLUTTET, 42)
			medEndringsmelding()

			ryddejobb.slettUtdaterteDeltakerlisterOgDeltakere()

			deltakerlisteRepository.getDeltakerliste(deltakerliste.id) shouldNotBe null
			deltakerRepository.getDeltaker(deltaker.id) shouldBe null
			ansattRepository.getKoordinatorDeltakerlisteDboListe(koordinator.id).size shouldBe 1
			ansattRepository.getVeilederDeltakerDboListe(veileder.id).size shouldBe 0
			endringsmeldingRepository.getEndringsmeldingerForDeltaker(deltaker.id) shouldBe emptyList()
		}
	}

	@Test
	fun `slettUtdaterteDeltakerlisterOgDeltakere - deltaker har sluttet for 38 dager siden - sletter ikke deltaker`() {
		with(DeltakerContext()) {
			medStatus(StatusType.HAR_SLUTTET, 38)
			medEndringsmelding()

			ryddejobb.slettUtdaterteDeltakerlisterOgDeltakere()

			deltakerlisteRepository.getDeltakerliste(deltakerliste.id) shouldNotBe null
			deltakerRepository.getDeltaker(deltaker.id) shouldNotBe null
		}
	}

	@Test
	fun `slettUtdaterteDeltakerlisterOgDeltakere - ingenting skal slettes - sletter ingenting`() {
		with(DeltakerContext()) {
			ryddejobb.slettUtdaterteDeltakerlisterOgDeltakere()

			deltakerlisteRepository.getDeltakerliste(deltakerliste.id) shouldNotBe null
			deltakerRepository.getDeltaker(deltaker.id) shouldNotBe null
		}
	}
}
