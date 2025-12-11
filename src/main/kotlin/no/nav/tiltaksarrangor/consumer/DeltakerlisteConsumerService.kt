package no.nav.tiltaksarrangor.consumer

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.amt.lib.utils.objectMapper
import no.nav.tiltaksarrangor.client.amtarrangor.AmtArrangorClient
import no.nav.tiltaksarrangor.client.amtarrangor.dto.toArrangorDbo
import no.nav.tiltaksarrangor.consumer.ConsumerUtils.tiltakskodeErStottet
import no.nav.tiltaksarrangor.repositories.ArrangorRepository
import no.nav.tiltaksarrangor.repositories.DeltakerlisteRepository
import no.nav.tiltaksarrangor.repositories.TiltakstypeRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class DeltakerlisteConsumerService(
	private val arrangorRepository: ArrangorRepository,
	private val deltakerlisteRepository: DeltakerlisteRepository,
	private val tiltakstypeRepository: TiltakstypeRepository,
	private val amtArrangorClient: AmtArrangorClient,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun lagreDeltakerliste(deltakerlisteId: UUID, value: String?) {
		if (value == null) {
			deltakerlisteRepository.deleteDeltakerlisteOgDeltakere(deltakerlisteId)
			log.info("Slettet tombstonet deltakerliste med id $deltakerlisteId")
			return
		}

		val tiltakskodeFromJson =
			getTiltakskodeFromDeltakerlisteJson(value)
				?: getSecondaryTiltakskodeFromDeltakerlisteJson(value)
		if (!tiltakskodeErStottet(tiltakskodeFromJson)) {
			log.info("Tiltakskode $tiltakskodeFromJson er ikke støttet.")
			return
		}

		val deltakerlistePayload: GjennomforingV2KafkaPayload.Gruppe = objectMapper.readValue(value)

		if (deltakerlistePayload.skalLagres()) {
			deltakerlisteRepository.insertOrUpdateDeltakerliste(
				deltakerlistePayload.toDeltakerlisteDbo(
					arrangorId = hentArrangorId(deltakerlistePayload.arrangor.organisasjonsnummer),
					navnTiltakstype = tiltakstypeRepository
						.getByTiltakskode(deltakerlistePayload.tiltakskode.name)
						?.navn
						?: throw IllegalStateException("Tiltakstype med tiltakskode ${deltakerlistePayload.tiltakskode} finnes ikke i db"),
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

		fun getTiltakskodeFromDeltakerlisteJson(messageJson: String): String? = objectMapper
			.readTree(messageJson)
			.get(TILTAKSKODE_KEY)
			?.asString()

		fun getSecondaryTiltakskodeFromDeltakerlisteJson(messageJson: String): String = objectMapper
			.readTree(messageJson)
			.get(TILTAKSTYPE_KEY)
			?.get(TILTAKSKODE_KEY)
			?.asString() ?: FALLBACK_TILTAKSKODE
	}
}
