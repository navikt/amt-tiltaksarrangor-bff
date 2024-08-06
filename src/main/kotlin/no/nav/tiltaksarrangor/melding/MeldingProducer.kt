package no.nav.tiltaksarrangor.melding

import no.nav.amt.lib.kafka.Producer
import no.nav.amt.lib.kafka.config.KafkaConfig
import no.nav.amt.lib.kafka.config.KafkaConfigImpl
import no.nav.amt.lib.models.arrangor.melding.EndringFraArrangor
import no.nav.amt.lib.models.arrangor.melding.Forslag
import no.nav.tiltaksarrangor.utils.JsonUtils
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

const val MELDING_TOPIC = "amt.arrangor-melding-v1"

@Service
class MeldingProducer(
	private val kafkaConfig: KafkaConfig,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	private val producer = Producer(kafkaConfig, MELDING_TOPIC)

	fun produce(forslag: Forslag) {
		when (forslag.status) {
			is Forslag.Status.Avvist,
			is Forslag.Status.Godkjent,
			-> error("Forsøkte å produsere forslag ${forslag.id} med status ${forslag.status::class.simpleName}")

			is Forslag.Status.Tilbakekalt,
			is Forslag.Status.Erstattet,
			is Forslag.Status.VenterPaSvar,
			-> {
				producer.produce(forslag.id.toString(), JsonUtils.objectMapper.writeValueAsString(forslag))
				log.info("Produserte forslag ${forslag.id} med status ${forslag.status::class.simpleName}")
			}
		}
	}

	fun produce(endring: EndringFraArrangor) {
		producer.produce(endring.id.toString(), JsonUtils.objectMapper.writeValueAsString(endring))
		log.info("Produserte endring fra arrngør ${endring.id}")
	}
}

@Configuration
@Profile("default")
class KafkaProducerConfig {
	@Bean
	fun config(): KafkaConfig = KafkaConfigImpl()
}
