package no.nav.tiltaksarrangor.repositories

import io.kotest.matchers.shouldBe
import no.nav.tiltaksarrangor.RepositoryTestBase
import no.nav.tiltaksarrangor.testutils.getNavEnhet
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [NavEnhetRepository::class])
class NavEnhetRepositoryTest : RepositoryTestBase() {
	@Autowired
	private lateinit var repository: NavEnhetRepository

	@Test
	fun `upsert - ny enhet - inserter`() {
		val enhet = getNavEnhet()
		repository.upsert(enhet)
		val insertedEnhet = repository.get(enhet.id)
		insertedEnhet!!.enhetsnummer shouldBe enhet.enhetsnummer
	}

	@Test
	fun `upsert - endret enhet - oppdaterer`() {
		val enhet = getNavEnhet()
		repository.upsert(enhet)

		val navn = "Nytt Navn"
		repository.upsert(enhet.copy(navn = navn))

		repository.get(enhet.id)!!.navn shouldBe navn
	}
}
