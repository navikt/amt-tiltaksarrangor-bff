package no.nav.tiltaksarrangor.repositories

import io.kotest.matchers.shouldBe
import no.nav.tiltaksarrangor.testutils.SingletonPostgresContainer
import no.nav.tiltaksarrangor.testutils.getNavEnhet
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

class NavEnhetRepositoryTest {
	private val repository = NavEnhetRepository(NamedParameterJdbcTemplate(SingletonPostgresContainer.getDataSource()))

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
