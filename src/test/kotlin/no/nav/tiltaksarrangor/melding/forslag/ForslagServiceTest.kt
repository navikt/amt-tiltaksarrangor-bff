package no.nav.tiltaksarrangor.melding.forslag

import io.kotest.matchers.shouldBe
import no.nav.amt.lib.kafka.config.LocalKafkaConfig
import no.nav.amt.lib.models.arrangor.melding.Forslag
import no.nav.amt.lib.testing.SingletonKafkaProvider
import no.nav.tiltaksarrangor.melding.MeldingProducer
import no.nav.tiltaksarrangor.melding.forslag.request.ForlengDeltakelseRequest
import no.nav.tiltaksarrangor.repositories.model.DeltakerMedDeltakerlisteDbo
import no.nav.tiltaksarrangor.testutils.DbTestDataUtils.shouldBeCloseTo
import no.nav.tiltaksarrangor.testutils.SingletonPostgresContainer
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDate
import java.time.LocalDateTime

class ForslagServiceTest {
	private val dataSource = SingletonPostgresContainer.getDataSource()
	private val template = NamedParameterJdbcTemplate(dataSource)
	private val repository = ForslagRepository(template)
	private val producer = MeldingProducer(LocalKafkaConfig(SingletonKafkaProvider.getHost()))
	private val forslagService = ForslagService(repository, producer)

	@Test
	fun `opprettForslag - forlengelse - produserer og returnerer nytt forslag`() {
		with(ForslagCtx(forlengDeltakelseForslag())) {
			val sluttdato = LocalDate.now().plusWeeks(42)
			val begrunnelse = "Fordi..."
			val request = ForlengDeltakelseRequest(sluttdato, begrunnelse)
			val opprettetForslag =
				forslagService.opprettForslag(request, koordinator, DeltakerMedDeltakerlisteDbo(deltaker, deltakerliste))

			opprettetForslag.deltakerId shouldBe deltaker.id
			opprettetForslag.opprettetAvArrangorAnsattId shouldBe koordinator.id
			opprettetForslag.status shouldBe Forslag.Status.VenterPaSvar
			opprettetForslag.endring shouldBe Forslag.ForlengDeltakelse(sluttdato)
			opprettetForslag.begrunnelse shouldBe begrunnelse
			opprettetForslag.opprettet shouldBeCloseTo LocalDateTime.now()

			assertProducedForslag(opprettetForslag.id, opprettetForslag.endring::class)
		}
	}
}
