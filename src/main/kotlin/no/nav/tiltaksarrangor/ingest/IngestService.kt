package no.nav.tiltaksarrangor.ingest

import no.nav.tiltaksarrangor.ingest.model.AVSLUTTENDE_STATUSER
import no.nav.tiltaksarrangor.ingest.model.AnsattDto
import no.nav.tiltaksarrangor.ingest.model.ArrangorDto
import no.nav.tiltaksarrangor.ingest.model.DeltakerDto
import no.nav.tiltaksarrangor.ingest.model.DeltakerlisteDto
import no.nav.tiltaksarrangor.ingest.model.DeltakerlisteStatus
import no.nav.tiltaksarrangor.ingest.model.SKJULES_ALLTID_STATUSER
import no.nav.tiltaksarrangor.ingest.model.toAnsattDbo
import no.nav.tiltaksarrangor.ingest.model.toArrangorDbo
import no.nav.tiltaksarrangor.ingest.model.toDeltakerDbo
import no.nav.tiltaksarrangor.ingest.model.toDeltakerlisteDbo
import no.nav.tiltaksarrangor.ingest.repositories.AnsattRepository
import no.nav.tiltaksarrangor.ingest.repositories.ArrangorRepository
import no.nav.tiltaksarrangor.ingest.repositories.DeltakerRepository
import no.nav.tiltaksarrangor.ingest.repositories.DeltakerlisteRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Component
class IngestService(
	private val arrangorRepository: ArrangorRepository,
	private val ansattRepository: AnsattRepository,
	private val deltakerlisteRepository: DeltakerlisteRepository,
	private val deltakerRepository: DeltakerRepository
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
			log.info("Slettet tombstonet deltakerliste med id $deltakerlisteId")
		} else if (deltakerlisteDto.skalLagres()) {
			deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerlisteDto.toDeltakerlisteDbo())
			log.info("Lagret deltakerliste med id $deltakerlisteId")
		} else {
			val antallSlettedeDeltakerlister = deltakerlisteRepository.deleteDeltakerliste(deltakerlisteId)
			if (antallSlettedeDeltakerlister > 0) {
				log.info("Slettet deltakerliste med id $deltakerlisteId")
			} else {
				log.info("Ignorert deltakerliste med id $deltakerlisteId")
			}
		}
	}

	fun lagreDeltaker(deltakerId: UUID, deltakerDto: DeltakerDto?) {
		if (deltakerDto == null) {
			deltakerRepository.deleteDeltaker(deltakerId)
			log.info("Slettet tombstonet deltaker med id $deltakerId")
		} else if (deltakerDto.skalLagres()) {
			deltakerRepository.insertOrUpdateDeltaker(deltakerDto.toDeltakerDbo())
			log.info("Lagret deltaker med id $deltakerId")
		} else {
			val antallSlettedeDeltakere = deltakerRepository.deleteDeltaker(deltakerId)
			if (antallSlettedeDeltakere > 0) {
				log.info("Slettet deltaker med id $deltakerId")
			} else {
				log.info("Ignorert deltaker med id $deltakerId")
			}
		}
	}
}

fun DeltakerlisteDto.skalLagres(): Boolean {
	if (status == DeltakerlisteStatus.GJENNOMFORES) {
		return true
	} else if (status == DeltakerlisteStatus.AVSLUTTET && sluttDato != null && LocalDate.now()
		.isBefore(sluttDato.plusDays(15))
	) {
		return true
	}
	return false
}

fun DeltakerDto.skalLagres(): Boolean {
	if (status.type in SKJULES_ALLTID_STATUSER) {
		return false
	} else if (status.type in AVSLUTTENDE_STATUSER) {
		return !LocalDateTime.now().isAfter(status.gyldigFra.plusWeeks(2))
	}
	return true
}
