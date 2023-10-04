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

val endringsmeldingerSomIgnoreres = listOf(
	"32873fe9-0c11-4593-b2a9-48466d6b5f92",
	"ec8dd190-b1ef-4dc0-9c14-435c6614bd85",
	"ea3127ba-aa11-4608-8896-0285e36043d4",
	"9506ad02-322c-47fa-96f7-1245a3c12f20",
	"97ee9903-523c-47fd-88cd-38d3a654057f",
	"1013326e-7d14-46cd-8a09-95d2533811f8",
	"9ba30873-61a6-41c6-b0f2-2f726530e5aa",
	"13560a66-9a48-49b5-82ac-34157ba6c2ce",
	"2bc631b3-72bc-4830-a29d-79cef24e9416",
	"aa34dcc3-2a7c-4108-ac0f-b01211d6ddd0",
	"b8b2a353-9254-4800-87df-38bf03c6b1c7",
	"18db9910-fb46-45cb-9a9f-330e82f1c7ba"
)

@Component
class KafkaListener(
	val ingestService: IngestService
) {

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
				if (cr.key() !in endringsmeldingerSomIgnoreres) {
					ingestService.lagreEndringsmelding(
						UUID.fromString(cr.key()),
						cr.value()?.let { fromJsonString(it) }
					)
				}
			}
			else -> throw IllegalStateException("Mottok melding på ukjent topic: ${cr.topic()}")
		}
		acknowledgment.acknowledge()
	}
}
