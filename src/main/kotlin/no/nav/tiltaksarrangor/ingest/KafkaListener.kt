package no.nav.tiltaksarrangor.ingest

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import no.nav.tiltaksarrangor.utils.JsonUtils.fromJsonString
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
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
	private val log = LoggerFactory.getLogger(javaClass)

	@KafkaListener(
		topics = [ARRANGOR_TOPIC, ARRANGOR_ANSATT_TOPIC, DELTAKERLISTE_TOPIC, DELTAKER_TOPIC, ENDRINGSMELDING_TOPIC],
		properties = ["auto.offset.reset = earliest"],
		containerFactory = "kafkaListenerContainerFactory"
	)
	fun listen(cr: ConsumerRecord<String, String>, acknowledgment: Acknowledgment) {
		when (cr.topic()) {
			ARRANGOR_TOPIC -> ingestService.lagreArrangor(UUID.fromString(cr.key()), cr.value()?.let { fromJsonString(it) })
			ARRANGOR_ANSATT_TOPIC -> ingestService.lagreAnsatt(UUID.fromString(cr.key()), cr.value()?.let { fromJsonString(it) })
			DELTAKERLISTE_TOPIC -> ingestService.lagreDeltakerliste(UUID.fromString(cr.key()), cr.value()?.let { fromJsonString(it) })
			DELTAKER_TOPIC -> ingestService.lagreDeltaker(UUID.fromString(cr.key()), cr.value()?.let { fromJsonString(it) })
			ENDRINGSMELDING_TOPIC -> {
				try {
					ingestService.lagreEndringsmelding(
						UUID.fromString(cr.key()),
						cr.value()?.let { fromJsonString(it) }
					)
				} catch (e: InvalidFormatException) {
					log.warn("Ignorerer melding med feil format, ${e.message}")
				}
			}
			else -> throw IllegalStateException("Mottok melding på ukjent topic: ${cr.topic()}")
		}
		acknowledgment.acknowledge()
	}
}
