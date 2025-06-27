package no.nav.tiltaksarrangor.repositories

import io.kotest.matchers.shouldBe
import no.nav.tiltaksarrangor.RepositoryTestBase
import no.nav.tiltaksarrangor.testutils.getNavAnsatt
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [NavAnsattRepository::class])
class NavAnsattRepositoryTest : RepositoryTestBase() {
	@Autowired
	private lateinit var repository: NavAnsattRepository

	@Test
	fun `upsert - ny ansatt - inserter`() {
		val ansatt = getNavAnsatt()
		repository.upsert(ansatt)
		val insertedAnsatt = repository.get(ansatt.id)
		insertedAnsatt shouldBe ansatt
	}

	@Test
	fun `upsert - endret ansatt - oppdaterer`() {
		val ansatt = getNavAnsatt()
		repository.upsert(ansatt)
		val nyEpost = "foo@bar.baz"
		repository.upsert(ansatt.copy(epost = "foo@bar.baz"))

		val insertedAnsatt = repository.get(ansatt.id)
		insertedAnsatt!!.epost shouldBe nyEpost
	}
}
