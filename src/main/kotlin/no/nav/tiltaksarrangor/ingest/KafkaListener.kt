package no.nav.tiltaksarrangor.ingest

import no.nav.tiltaksarrangor.utils.JsonUtils.fromJsonString
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.util.UUID

const val ARRANGOR_TOPIC = "amt.arrangor-v1"
const val ARRANGOR_ANSATT_TOPIC = "amt.arrangor-ansatt-v1"

@Component
class KafkaListener(
	val ingestService: IngestService
) {

	@KafkaListener(
		topics = [ARRANGOR_TOPIC, ARRANGOR_ANSATT_TOPIC],
		properties = ["auto.offset.reset = earliest"],
		containerFactory = "kafkaListenerContainerFactory"
	)
	fun listen(cr: ConsumerRecord<String, String>, acknowledgment: Acknowledgment) {
		when (cr.topic()) {
			ARRANGOR_TOPIC -> ingestService.lagreArrangor(UUID.fromString(cr.key()), cr.value()?.let { fromJsonString(it) })
			ARRANGOR_ANSATT_TOPIC -> ingestService.lagreAnsatt(UUID.fromString(cr.key()), cr.value()?.let { fromJsonString(it) })
			else -> throw IllegalStateException("Mottok melding på ukjent topic: ${cr.topic()}")
		}
		acknowledgment.acknowledge()
	}
}
