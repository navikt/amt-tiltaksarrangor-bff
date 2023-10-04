package no.nav.tiltaksarrangor.ingest

import no.nav.tiltaksarrangor.utils.JsonUtils.fromJsonString
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.util.UUID

const val ARRANGOR_TOPIC = "amt.arrangor-v1"
const val ARRANGOR_ANSATT_TOPIC = "amt.arrangor-ansatt-v1"
const val DELTAKERLISTE_TOPIC = "team-mulighetsrommet.siste-tiltaksgjennomforinger-v1"
const val DELTAKER_TOPIC = "amt.deltaker-v2"
const val ENDRINGSMELDING_TOPIC = "amt.endringsmelding-v1"

@Component
class KafkaListener(
	val ingestService: IngestService
) {

	@KafkaListener(
		topics = [ARRANGOR_TOPIC, ARRANGOR_ANSATT_TOPIC, DELTAKERLISTE_TOPIC, DELTAKER_TOPIC],
		properties = ["auto.offset.reset = earliest"],
		containerFactory = "kafkaListenerContainerFactory"
	)
	fun listen(cr: ConsumerRecord<String, String>, acknowledgment: Acknowledgment) {
		when (cr.topic()) {
			ARRANGOR_TOPIC -> ingestService.lagreArrangor(UUID.fromString(cr.key()), cr.value()?.let { fromJsonString(it) })
			ARRANGOR_ANSATT_TOPIC -> ingestService.lagreAnsatt(UUID.fromString(cr.key()), cr.value()?.let { fromJsonString(it) })
			DELTAKERLISTE_TOPIC -> ingestService.lagreDeltakerliste(UUID.fromString(cr.key()), cr.value()?.let { fromJsonString(it) })
			DELTAKER_TOPIC -> ingestService.lagreDeltaker(UUID.fromString(cr.key()), cr.value()?.let { fromJsonString(it) })
			else -> throw IllegalStateException("Mottok melding på ukjent topic: ${cr.topic()}")
		}
		acknowledgment.acknowledge()
	}

	@KafkaListener(
		topics = [ENDRINGSMELDING_TOPIC],
		properties = ["auto.offset.reset = earliest"],
		containerFactory = "kafkaListenerContainerFactoryV2"
	)
	fun listenV2(cr: ConsumerRecord<String, String>, acknowledgment: Acknowledgment) {
		when (cr.topic()) {
			ENDRINGSMELDING_TOPIC -> {
				if (cr.key() != "32873fe9-0c11-4593-b2a9-48466d6b5f92") {
					ingestService.lagreEndringsmelding(
						UUID.fromString(cr.key()),
						cr.value()?.let { fromJsonString(it) })
				}
			}
			else -> throw IllegalStateException("Mottok melding på ukjent topic: ${cr.topic()}")
		}
		acknowledgment.acknowledge()
	}
}
