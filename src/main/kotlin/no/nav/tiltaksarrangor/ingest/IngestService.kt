package no.nav.tiltaksarrangor.ingest

import no.nav.tiltaksarrangor.ingest.model.AnsattDto
import no.nav.tiltaksarrangor.ingest.model.ArrangorDto
import no.nav.tiltaksarrangor.ingest.model.DeltakerlisteDto
import no.nav.tiltaksarrangor.ingest.model.toAnsattDbo
import no.nav.tiltaksarrangor.ingest.model.toArrangorDbo
import no.nav.tiltaksarrangor.ingest.model.toDeltakerlisteDbo
import no.nav.tiltaksarrangor.ingest.repositories.AnsattRepository
import no.nav.tiltaksarrangor.ingest.repositories.ArrangorRepository
import no.nav.tiltaksarrangor.ingest.repositories.DeltakerlisteRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class IngestService(
	private val arrangorRepository: ArrangorRepository,
	private val ansattRepository: AnsattRepository,
	private val deltakerlisteRepository: DeltakerlisteRepository
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun lagreArrangor(arrangorId: UUID, arrangor: ArrangorDto?) {
		if (arrangor == null) {
			arrangorRepository.deleteArrangor(arrangorId)
			log.info("Slettet arrangør med id $arrangorId")
		} else {
			arrangorRepository.insertOrUpdateArrangor(arrangor.toArrangorDbo())
			log.info("Lagret arrangør med id $arrangorId")
		}
	}

	fun lagreAnsatt(ansattId: UUID, ansatt: AnsattDto?) {
		if (ansatt == null) {
			ansattRepository.deleteAnsatt(ansattId)
			log.info("Slettet ansatt med id $ansattId")
		} else {
			ansattRepository.insertOrUpdateAnsatt(ansatt.toAnsattDbo())
			log.info("Lagret ansatt med id $ansattId")
		}
	}

	fun lagreDeltakerliste(deltakerlisteId: UUID, deltakerlisteDto: DeltakerlisteDto?) {
		if (deltakerlisteDto == null) {
			deltakerlisteRepository.deleteDeltakerliste(deltakerlisteId)
			log.info("Slettet deltakerliste med id $deltakerlisteId")
		} else {
			deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerlisteDto.toDeltakerlisteDbo())
			log.info("Lagret deltakerliste med id $deltakerlisteId")
		}
	}
}
