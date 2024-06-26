package no.nav.tiltaksarrangor.melding.forslag

import io.kotest.matchers.shouldBe
import no.nav.amt.lib.models.arrangor.melding.Forslag
import no.nav.tiltaksarrangor.testutils.DbTestDataUtils.shouldBeCloseTo
import no.nav.tiltaksarrangor.testutils.SingletonPostgresContainer
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDateTime
import java.util.UUID

class ForslagRepositoryTest {
	private val dataSource = SingletonPostgresContainer.getDataSource()
	private val template = NamedParameterJdbcTemplate(dataSource)
	private val repository = ForslagRepository(template)

	@Test
	fun `upsert - nytt forslag - inserter`() {
		with(ForslagCtx(forlengDeltakelseForslag())) {
			val insertedForslag = repository.upsert(forslag!!)

			insertedForslag.id shouldBe forslag.id
			insertedForslag.deltakerId shouldBe forslag.deltakerId
			insertedForslag.opprettetAvArrangorAnsattId shouldBe forslag.opprettetAvArrangorAnsattId
			insertedForslag.opprettet shouldBeCloseTo forslag.opprettet
			insertedForslag.endring shouldBe forslag.endring
			insertedForslag.status shouldBe forslag.status
		}
	}

	@Test
	fun `upsert - oppdatert forslag - oppdaterer`() {
		with(ForslagCtx(forlengDeltakelseForslag())) {
			repository.upsert(forslag!!)

			val nyStatus = Forslag.Status.Godkjent(
				godkjentAv = Forslag.NavAnsatt(UUID.randomUUID(), UUID.randomUUID()),
				godkjent = LocalDateTime.now(),
			)

			val oppdatertForslag = forslag.copy(status = nyStatus)

			val oppdatertStatus = repository.upsert(oppdatertForslag).status as Forslag.Status.Godkjent

			oppdatertStatus.godkjent shouldBe nyStatus.godkjent
			oppdatertStatus.godkjentAv shouldBe nyStatus.godkjentAv
		}
	}

	@Test
	fun `delete - sletter forslag`() {
		with(ForslagCtx(forlengDeltakelseForslag())) {
			setForslagGodkjent()
			upsertForslag()

			repository.delete(forslag.id) shouldBe 1
			repository.get(forslag.id).isFailure shouldBe true
		}
	}

	@Test
	fun `delete - forslag finnes ikke - returnerer antall slettede rader`() {
		repository.delete(UUID.randomUUID()) shouldBe 0
	}
}
