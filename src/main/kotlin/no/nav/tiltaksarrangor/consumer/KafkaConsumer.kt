package no.nav.tiltaksarrangor.consumer

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.amt.lib.utils.objectMapper
import no.nav.tiltaksarrangor.consumer.model.TiltakstypePayload
import no.nav.tiltaksarrangor.melding.MELDING_TOPIC
import no.nav.tiltaksarrangor.repositories.TiltakstypeRepository
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class KafkaConsumer(
	private val kafkaConsumerService: KafkaConsumerService,
	private val deltakerlisteHandler: DeltakerlisteHandler,
	private val tiltakstypeRepository: TiltakstypeRepository,
) {
	@KafkaListener(
		topics = [
			ARRANGOR_TOPIC,
			ARRANGOR_ANSATT_TOPIC,
			DELTAKERLISTE_V1_TOPIC, // fjernes etter migrering til v2
			DELTAKERLISTE_V2_TOPIC,
			TILTAKSTYPE_TOPIC,
			DELTAKER_TOPIC,
			ENDRINGSMELDING_TOPIC,
			NAV_ANSATT_TOPIC,
			MELDING_TOPIC,
			NAV_ENHET_TOPIC,
		],
		properties = ["auto.offset.reset = earliest"],
		containerFactory = "kafkaListenerContainerFactory",
	)
	fun listen(consumerRecord: ConsumerRecord<String, String>, acknowledgment: Acknowledgment) {
		when (consumerRecord.topic()) {
			ARRANGOR_TOPIC -> kafkaConsumerService.lagreArrangor(
				UUID.fromString(consumerRecord.key()),
				consumerRecord.value()?.let { objectMapper.readValue(it) },
			)

			ARRANGOR_ANSATT_TOPIC -> kafkaConsumerService.lagreAnsatt(
				UUID.fromString(consumerRecord.key()),
				consumerRecord.value()?.let { objectMapper.readValue(it) },
			)

			// fjernes etter migrering til v2
			DELTAKERLISTE_V1_TOPIC -> deltakerlisteHandler.lagreDeltakerliste(
				topic = DELTAKERLISTE_V1_TOPIC,
				deltakerlisteId = UUID.fromString(consumerRecord.key()),
				value = consumerRecord.value(),
			)

			DELTAKERLISTE_V2_TOPIC -> deltakerlisteHandler.lagreDeltakerliste(
				topic = DELTAKERLISTE_V1_TOPIC,
				deltakerlisteId = UUID.fromString(consumerRecord.key()),
				value = consumerRecord.value(),
			)

			TILTAKSTYPE_TOPIC ->
				objectMapper
					.readValue<TiltakstypePayload>(consumerRecord.value())
					.let { tiltakstypeRepository.upsert(it.toModel()) }

			DELTAKER_TOPIC -> kafkaConsumerService.lagreDeltaker(
				UUID.fromString(consumerRecord.key()),
				consumerRecord.value()?.let { objectMapper.readValue(it) },
			)

			ENDRINGSMELDING_TOPIC -> kafkaConsumerService.lagreEndringsmelding(
				UUID.fromString(consumerRecord.key()),
				consumerRecord.value()?.let { objectMapper.readValue(it) },
			)

			NAV_ANSATT_TOPIC -> kafkaConsumerService.lagreNavAnsatt(
				UUID.fromString(consumerRecord.key()),
				objectMapper.readValue(consumerRecord.value()),
			)

			MELDING_TOPIC -> kafkaConsumerService.handleMelding(
				UUID.fromString(consumerRecord.key()),
				consumerRecord.value()?.let { objectMapper.readValue(it) },
			)

			NAV_ENHET_TOPIC -> kafkaConsumerService.lagreNavEnhet(
				UUID.fromString(consumerRecord.key()),
				objectMapper.readValue(consumerRecord.value()),
			)

			else -> throw IllegalStateException("Mottok melding på ukjent topic: ${consumerRecord.topic()}")
		}

		acknowledgment.acknowledge()
	}

	companion object {
		const val ARRANGOR_TOPIC = "amt.arrangor-v1"
		const val ARRANGOR_ANSATT_TOPIC = "amt.arrangor-ansatt-v1"
		const val DELTAKERLISTE_V1_TOPIC = "team-mulighetsrommet.siste-tiltaksgjennomforinger-v1"
		const val DELTAKERLISTE_V2_TOPIC = "team-mulighetsrommet.siste-tiltaksgjennomforinger-v2"
		const val TILTAKSTYPE_TOPIC = "team-mulighetsrommet.siste-tiltakstyper-v3"
		const val DELTAKER_TOPIC = "amt.deltaker-v2"
		const val ENDRINGSMELDING_TOPIC = "amt.endringsmelding-v1"
		const val NAV_ANSATT_TOPIC = "amt.nav-ansatt-personalia-v1"
		const val NAV_ENHET_TOPIC = "amt.nav-enhet-v1"
	}
}
