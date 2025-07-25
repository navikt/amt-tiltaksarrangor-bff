package no.nav.tiltaksarrangor.consumer

import no.nav.tiltaksarrangor.melding.MELDING_TOPIC
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
const val NAV_ANSATT_TOPIC = "amt.nav-ansatt-personalia-v1"
const val NAV_ENHET_TOPIC = "amt.nav-enhet-v1"

@Component
class KafkaConsumer(
	val kafkaConsumerService: KafkaConsumerService,
) {
	@KafkaListener(
		topics = [
			ARRANGOR_TOPIC,
			ARRANGOR_ANSATT_TOPIC,
			DELTAKERLISTE_TOPIC,
			DELTAKER_TOPIC,
			ENDRINGSMELDING_TOPIC,
			NAV_ANSATT_TOPIC,
			MELDING_TOPIC,
			NAV_ENHET_TOPIC,
		],
		properties = ["auto.offset.reset = earliest"],
		containerFactory = "kafkaListenerContainerFactory",
	)
	fun listen(cr: ConsumerRecord<String, String>, acknowledgment: Acknowledgment) {
		when (cr.topic()) {
			ARRANGOR_TOPIC -> kafkaConsumerService.lagreArrangor(UUID.fromString(cr.key()), cr.value()?.let { fromJsonString(it) })
			ARRANGOR_ANSATT_TOPIC -> kafkaConsumerService.lagreAnsatt(UUID.fromString(cr.key()), cr.value()?.let { fromJsonString(it) })
			DELTAKERLISTE_TOPIC -> kafkaConsumerService.lagreDeltakerliste(UUID.fromString(cr.key()), cr.value()?.let { fromJsonString(it) })
			DELTAKER_TOPIC -> kafkaConsumerService.lagreDeltaker(UUID.fromString(cr.key()), cr.value()?.let { fromJsonString(it) })
			ENDRINGSMELDING_TOPIC -> kafkaConsumerService.lagreEndringsmelding(UUID.fromString(cr.key()), cr.value()?.let { fromJsonString(it) })
			NAV_ANSATT_TOPIC -> kafkaConsumerService.lagreNavAnsatt(UUID.fromString(cr.key()), fromJsonString(cr.value()))
			MELDING_TOPIC -> kafkaConsumerService.handleMelding(UUID.fromString(cr.key()), cr.value()?.let { fromJsonString(it) })
			NAV_ENHET_TOPIC -> kafkaConsumerService.lagreNavEnhet(UUID.fromString(cr.key()), fromJsonString(cr.value()))
			else -> throw IllegalStateException("Mottok melding på ukjent topic: ${cr.topic()}")
		}
		acknowledgment.acknowledge()
	}
}
