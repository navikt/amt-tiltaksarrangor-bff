package no.nav.tiltaksarrangor.melding.endring

import com.fasterxml.jackson.module.kotlin.readValue
import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.shouldBe
import io.mockk.every
import no.nav.amt.lib.kafka.config.KafkaConfig
import no.nav.amt.lib.kafka.config.LocalKafkaConfig
import no.nav.amt.lib.models.arrangor.melding.EndringFraArrangor
import no.nav.amt.lib.models.arrangor.melding.Forslag
import no.nav.amt.lib.models.arrangor.melding.Melding
import no.nav.amt.lib.models.arrangor.melding.Vurdering
import no.nav.amt.lib.testing.AsyncUtils
import no.nav.amt.lib.testing.SingletonKafkaProvider
import no.nav.tiltaksarrangor.IntegrationTest
import no.nav.tiltaksarrangor.client.amtarrangor.AmtArrangorClient
import no.nav.tiltaksarrangor.kafka.stringStringConsumer
import no.nav.tiltaksarrangor.melding.MELDING_TOPIC
import no.nav.tiltaksarrangor.melding.endring.request.LeggTilOppstartsdatoRequest
import no.nav.tiltaksarrangor.testutils.DeltakerContext
import no.nav.tiltaksarrangor.unleash.UnleashService
import no.nav.tiltaksarrangor.utils.JsonUtils.objectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.time.LocalDate
import java.util.UUID
import kotlin.reflect.KClass

class EndringServiceTest(
	private val endringService: EndringService,
	@MockkBean private val unleashService: UnleashService,
	@MockkBean @Suppress("unused") private val amtArrangorClient: AmtArrangorClient,
) : IntegrationTest() {
	@TestConfiguration
	class TestConfig {
		@Bean
		@Primary
		fun localKafkaConfig(): KafkaConfig = LocalKafkaConfig(SingletonKafkaProvider.getHost())
	}

	@BeforeEach
	internal fun setup() {
		every { unleashService.erKometMasterForTiltakstype(any()) } returns false
	}

	@Test
	fun `endreDeltaker - ny endring - returnerer oppdaterer deltaker og produserer melding`() {
		with(DeltakerContext(applicationContext)) {
			setVenterPaOppstart()
			val request = LeggTilOppstartsdatoRequest(
				startdato = LocalDate.now().plusWeeks(7),
				sluttdato = LocalDate.now().plusWeeks(42),
			)
			val oppdatertDeltaker = endringService.endreDeltaker(
				deltaker = deltaker,
				deltakerliste = deltakerliste,
				ansatt = koordinator,
				request = request,
			)

			oppdatertDeltaker.startDato shouldBe request.startdato
			oppdatertDeltaker.sluttDato shouldBe request.sluttdato

			assertProducedEndring(deltaker.id, EndringFraArrangor.LeggTilOppstartsdato::class)
		}
	}

	@Test
	fun `endreDeltaker - ny endring - oppdaterer og lagrer deltaker`() {
		with(DeltakerContext(applicationContext)) {
			setVenterPaOppstart()
			val request = LeggTilOppstartsdatoRequest(
				startdato = LocalDate.now().plusWeeks(7),
				sluttdato = LocalDate.now().plusWeeks(42),
			)
			endringService.endreDeltaker(
				deltaker = deltaker,
				deltakerliste = deltakerliste,
				ansatt = koordinator,
				request = request,
			)

			val oppdatertDeltaker = deltakerRepository.getDeltaker(deltaker.id)
			oppdatertDeltaker?.startdato shouldBe request.startdato
			oppdatertDeltaker?.sluttdato shouldBe request.sluttdato
		}
	}
}

fun <T : EndringFraArrangor.Endring> assertProducedEndring(deltakerId: UUID, endringstype: KClass<T>) {
	val cache = mutableMapOf<UUID, Melding>()

	val consumer = stringStringConsumer(MELDING_TOPIC) { k, v ->
		cache[UUID.fromString(k)] = objectMapper.readValue(v)
	}

	consumer.start()

	AsyncUtils.eventually {
		val endring = cache.firstNotNullOf {
			when (val endring = it.value) {
				is EndringFraArrangor -> {
					if (endring.deltakerId == deltakerId) {
						endring
					} else {
						null
					}
				}

				is Forslag,
				is Vurdering,
				-> null
			}
		}

		endring.endring::class shouldBe endringstype
	}

	consumer.stop()
}
