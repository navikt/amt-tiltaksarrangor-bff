package no.nav.tiltaksarrangor.ingest.jobs

import no.nav.tiltaksarrangor.ingest.jobs.leaderelection.LeaderElection
import no.nav.tiltaksarrangor.ingest.repositories.DeltakerRepository
import no.nav.tiltaksarrangor.ingest.repositories.DeltakerlisteRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class Ryddejobb(
	private val leaderElection: LeaderElection,
	private val deltakerlisteRepository: DeltakerlisteRepository,
	private val deltakerRepository: DeltakerRepository
) {
	private val log = LoggerFactory.getLogger(javaClass)

	// @Scheduled(cron = "0 0 3 * * *") // kl 03.00 hver natt
	@Scheduled(cron = "0 20 * * * *") // kun for å teste i dev
	fun slettUtdaterteDeltakerlisterOgDeltakere() {
		if (leaderElection.isLeader()) {
			val slettesDato = LocalDate.now().minusDays(14)
			val deltakerlisterSomSkalSlettes = deltakerlisteRepository.getDeltakerlisterSomSkalSlettes(slettesDato)
			deltakerlisterSomSkalSlettes.forEach { deltakerlisteRepository.deleteDeltakerlisteOgDeltakere(it) }
			log.info("Slettet ${deltakerlisterSomSkalSlettes.size} deltakerlister med deltakere")

			val deltakereSomSkalSlettes = deltakerRepository.getDeltakereSomSkalSlettes(slettesDato)
			deltakereSomSkalSlettes.forEach { deltakerRepository.deleteDeltaker(it) }
			log.info("Slettet ${deltakereSomSkalSlettes.size} deltakere")
		} else {
			log.info("Kjører ikke ryddejobb siden denne podden ikke er leader")
		}
	}
}
