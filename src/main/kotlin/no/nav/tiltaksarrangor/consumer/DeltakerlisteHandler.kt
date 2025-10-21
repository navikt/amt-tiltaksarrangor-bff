package no.nav.tiltaksarrangor.consumer

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.amt.lib.utils.objectMapper
import no.nav.tiltaksarrangor.client.amtarrangor.AmtArrangorClient
import no.nav.tiltaksarrangor.client.amtarrangor.dto.toArrangorDbo
import no.nav.tiltaksarrangor.consumer.KafkaConsumer.Companion.DELTAKERLISTE_V2_TOPIC
import no.nav.tiltaksarrangor.consumer.model.DeltakerlistePayload
import no.nav.tiltaksarrangor.repositories.ArrangorRepository
import no.nav.tiltaksarrangor.repositories.DeltakerlisteRepository
import no.nav.tiltaksarrangor.repositories.TiltakstypeRepository
import no.nav.tiltaksarrangor.unleash.UnleashToggle
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class DeltakerlisteHandler(
	private val arrangorRepository: ArrangorRepository,
	private val deltakerlisteRepository: DeltakerlisteRepository,
	private val tiltakstypeRepository: TiltakstypeRepository,
	private val amtArrangorClient: AmtArrangorClient,
	private val unleashToggle: UnleashToggle,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun lagreDeltakerliste(
		topic: String,
		deltakerlisteId: UUID,
		value: String?,
	) {
		if (value == null) {
			deltakerlisteRepository.deleteDeltakerlisteOgDeltakere(deltakerlisteId)
			log.info("Slettet tombstonet deltakerliste med id $deltakerlisteId")
			return
		}

		if (topic == DELTAKERLISTE_V2_TOPIC && !unleashToggle.skalLeseGjennomforingerV2()) {
			log.info("Unleash er ikke enabled for $DELTAKERLISTE_V2_TOPIC")
			return
		}

		val tiltakskodeFromJson = getTiltakskodeFromMessageJson(value)
		if (!unleashToggle.erKometMasterForTiltakstype(tiltakskodeFromJson)) {
			log.info("Tiltakskode $tiltakskodeFromJson er ikke støttet.")
			return
		}

		val deltakerlistePayload: DeltakerlistePayload = objectMapper.readValue(value)

		if (deltakerlistePayload.skalLagres()) {
			deltakerlisteRepository.insertOrUpdateDeltakerliste(
				deltakerlistePayload.toDeltakerlisteDbo(
					arrangorId = hentArrangorId(deltakerlistePayload.organisasjonsnummer),
					navnTiltakstype = tiltakstypeRepository.getById(deltakerlistePayload.tiltakstype.id)?.navn
						?: throw IllegalStateException("Tiltakstype med id ${deltakerlistePayload.tiltakstype.id} finnes ikke i db"),
				),
			)
			log.info("Lagret deltakerliste med id $deltakerlisteId")
		} else {
			val antallSlettedeDeltakerlister = deltakerlisteRepository.deleteDeltakerlisteOgDeltakere(deltakerlisteId)
			if (antallSlettedeDeltakerlister > 0) {
				log.info("Slettet deltakerliste med id $deltakerlisteId")
			} else {
				log.info("Ignorert deltakerliste med id $deltakerlisteId")
			}
		}
	}

	private fun hentArrangorId(organisasjonsnummer: String): UUID {
		arrangorRepository.getArrangor(organisasjonsnummer)?.let { arrangor -> return arrangor.id }

		log.info("Fant ikke arrangør med orgnummer $organisasjonsnummer i databasen, henter fra amt-arrangør")
		val arrangor =
			amtArrangorClient.getArrangor(organisasjonsnummer)
				?: throw RuntimeException("Kunne ikke hente arrangør med orgnummer $organisasjonsnummer")

		arrangorRepository.insertOrUpdateArrangor(arrangor.toArrangorDbo())
		return arrangor.id
	}

	companion object {
		private const val TILTAKSTYPE_KEY = "tiltakstype"
		private const val TILTAKSKODE_KEY = "tiltakskode"
		private const val FALLBACK_TILTAKSKODE = "UKJENT"

		fun getTiltakskodeFromMessageJson(messageJson: String): String = objectMapper
			.readTree(messageJson)
			.get(TILTAKSTYPE_KEY)
			?.get(TILTAKSKODE_KEY)
			?.asText() ?: FALLBACK_TILTAKSKODE
	}
}
