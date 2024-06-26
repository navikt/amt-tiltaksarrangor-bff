package no.nav.tiltaksarrangor.kafka

import no.nav.amt.lib.kafka.config.LocalKafkaConfig
import no.nav.amt.lib.testing.SingletonKafkaProvider
import no.nav.tiltaksarrangor.ingest.config.KafkaConfig
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.core.DefaultKafkaConsumerFactory

@Configuration
class TestKafkaConfig(
	private val kafkaConfig: KafkaConfig,
) {
	fun testConsumerProps(groupId: String) = mapOf(
		ConsumerConfig.GROUP_ID_CONFIG to groupId,
		ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
		ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
		ConsumerConfig.MAX_POLL_RECORDS_CONFIG to "1",
	) + kafkaConfig.commonConfig()

	@Bean
	fun testKafkaConsumer(): Consumer<String, String> {
		return DefaultKafkaConsumerFactory(
			testConsumerProps("bff-consumer"),
			StringDeserializer(),
			StringDeserializer(),
		).createConsumer()
	}

	@Bean
	fun testKafkaProducer(): KafkaProducer<String, String> {
		val config =
			mapOf(
				ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
				ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
				ProducerConfig.ACKS_CONFIG to "all",
				ProducerConfig.RETRIES_CONFIG to 10,
				ProducerConfig.RETRY_BACKOFF_MS_CONFIG to 100,
			) + kafkaConfig.commonConfig()
		return KafkaProducer(config)
	}
}

@Configuration
@Profile("test")
class TestKafkaProducerConfig {
	@Bean
	fun testConfig(): no.nav.amt.lib.kafka.config.KafkaConfig {
		return LocalKafkaConfig(SingletonKafkaProvider.getHost())
	}
}
