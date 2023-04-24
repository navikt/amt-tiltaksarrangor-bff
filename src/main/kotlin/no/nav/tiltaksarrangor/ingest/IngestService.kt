package no.nav.tiltaksarrangor.ingest

import no.nav.tiltaksarrangor.ingest.model.ArrangorDto
import no.nav.tiltaksarrangor.ingest.model.toArrangorDbo
import no.nav.tiltaksarrangor.ingest.repositories.ArrangorRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class IngestService(
	private val arrangorRepository: ArrangorRepository
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
}
