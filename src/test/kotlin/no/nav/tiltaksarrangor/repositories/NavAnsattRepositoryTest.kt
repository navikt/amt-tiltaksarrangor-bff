package no.nav.tiltaksarrangor.repositories

import io.kotest.matchers.shouldBe
import no.nav.tiltaksarrangor.testutils.SingletonPostgresContainer
import no.nav.tiltaksarrangor.testutils.getNavAnsatt
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

class NavAnsattRepositoryTest {
	private val dataSource = SingletonPostgresContainer.getDataSource()
	private val template = NamedParameterJdbcTemplate(dataSource)
	private val repository = NavAnsattRepository(template)

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
