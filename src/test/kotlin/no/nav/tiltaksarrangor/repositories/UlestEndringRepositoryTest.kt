package no.nav.tiltaksarrangor.repositories

import io.kotest.matchers.shouldBe
import no.nav.tiltaksarrangor.RepositoryTestBase
import no.nav.tiltaksarrangor.model.Oppdatering
import no.nav.tiltaksarrangor.testutils.getDeltaker
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [UlestEndringRepository::class, DeltakerRepository::class])
class UlestEndringRepositoryTest : RepositoryTestBase() {
	@Autowired
	private lateinit var ulestEndringRepository: UlestEndringRepository

	@Autowired
	private lateinit var deltakerRepository: DeltakerRepository

	@Test
	fun `insert - ny ulest endring`() {
		val deltaker = getDeltaker()
		val oppdatering = Oppdatering.NavBrukerEndring("122", "jfieo")
		deltakerRepository.insertOrUpdateDeltaker(deltaker)
		val res = ulestEndringRepository.insert(deltaker.id, oppdatering)

		res.deltakerId shouldBe deltaker.id
		res.oppdatering shouldBe oppdatering
	}

	@Test
	fun `upsert - samme endring flere ganger`() {
		val deltaker = getDeltaker()
		val oppdatering = Oppdatering.NavBrukerEndring("122", "jfieo")
		deltakerRepository.insertOrUpdateDeltaker(deltaker)

		val res2 = ulestEndringRepository.insert(deltaker.id, oppdatering)

		res2.deltakerId shouldBe deltaker.id
		res2.oppdatering shouldBe oppdatering
	}
}
