package no.nav.tiltaksarrangor.melding.forslag

import no.nav.amt.lib.models.arrangor.melding.Forslag
import no.nav.tiltaksarrangor.melding.MeldingProducer
import no.nav.tiltaksarrangor.melding.forslag.request.ForlengDeltakelseRequest
import no.nav.tiltaksarrangor.melding.forslag.request.ForslagRequest
import no.nav.tiltaksarrangor.repositories.model.AnsattDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerMedDeltakerlisteDbo
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class ForslagService(
	private val repository: ForslagRepository,
	private val meldingProducer: MeldingProducer,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun opprettForslag(
		request: ForslagRequest,
		ansatt: AnsattDbo,
		deltakerMedDeltakerliste: DeltakerMedDeltakerlisteDbo,
	): Forslag {
		val endring = when (request) {
			is ForlengDeltakelseRequest -> Forslag.ForlengDeltakelse(request.sluttdato)
		}

		val forslag = repository.upsert(
			Forslag(
				id = UUID.randomUUID(),
				deltakerId = deltakerMedDeltakerliste.deltaker.id,
				opprettetAvArrangorAnsattId = ansatt.id,
				opprettet = LocalDateTime.now(),
				begrunnelse = request.begrunnelse,
				endring = endring,
				status = Forslag.Status.VenterPaSvar,
			),
		)
		meldingProducer.produce(forslag)

		log.info("Opprettet nytt forslag ${forslag.id}")

		return forslag
	}

	fun get(id: UUID) = repository.get(id)

	fun getAktiveForslag(deltakerId: UUID) = repository.getForDeltaker(deltakerId).filter { it.status is Forslag.Status.VenterPaSvar }

	fun delete(id: UUID) {
		val slettedeRader = repository.delete(id)
		if (slettedeRader > 0) {
			log.info("Slettet forslag $id")
		}
	}

	fun tilbakekall(id: UUID, ansatt: AnsattDbo) {
		val opprinneligForslag = repository.get(id).getOrThrow()

		require(opprinneligForslag.status is Forslag.Status.VenterPaSvar) {
			"Kan ikke tilbakekalle forslag $id med status ${opprinneligForslag.status.javaClass.simpleName}"
		}

		val tilbakekaltForslag = opprinneligForslag.copy(
			status = Forslag.Status.Tilbakekalt(
				tilbakekaltAvArrangorAnsattId = ansatt.id,
				tilbakekalt = LocalDateTime.now(),
			),
		)

		repository.delete(id)
		meldingProducer.produce(tilbakekaltForslag)

		log.info("Tilbakekalte forslag $id")
	}
}
