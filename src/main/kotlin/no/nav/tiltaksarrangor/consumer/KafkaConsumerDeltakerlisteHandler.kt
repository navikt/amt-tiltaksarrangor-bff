package no.nav.tiltaksarrangor.consumer

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.amt.lib.utils.objectMapper
import no.nav.tiltaksarrangor.client.amtarrangor.AmtArrangorClient
import no.nav.tiltaksarrangor.client.amtarrangor.dto.toArrangorDbo
import no.nav.tiltaksarrangor.consumer.model.DeltakerlistePayload
import no.nav.tiltaksarrangor.consumer.model.DeltakerlistePayload.Companion.ENKELTPLASS_V2_TYPE
import no.nav.tiltaksarrangor.repositories.ArrangorRepository
import no.nav.tiltaksarrangor.repositories.DeltakerlisteRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class KafkaConsumerDeltakerlisteHandler(
	private val arrangorRepository: ArrangorRepository,
	private val deltakerlisteRepository: DeltakerlisteRepository,
	private val amtArrangorClient: AmtArrangorClient,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun lagreDeltakerliste(deltakerlisteId: UUID, value: String?) {
		if (value == null) {
			deltakerlisteRepository.deleteDeltakerlisteOgDeltakere(deltakerlisteId)
			log.info("Slettet tombstonet deltakerliste med id $deltakerlisteId")
			return
		}

		objectMapper
			.readTree(value)
			.get("type")
			?.asText()
			?.takeIf { it == ENKELTPLASS_V2_TYPE }
			?.run {
				log.info("Ignorerer deltakerliste for enkeltplass med id $deltakerlisteId.")
				return
			}

		val deltakerlistePayload: DeltakerlistePayload = objectMapper.readValue(value)

		if (deltakerlistePayload.skalLagres()) {
			deltakerlisteRepository.insertOrUpdateDeltakerliste(
				deltakerlistePayload.toDeltakerlisteDbo(
					hentArrangorId(deltakerlistePayload.organisasjonsnummer),
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
}
