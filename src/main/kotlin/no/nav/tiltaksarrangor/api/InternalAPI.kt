package no.nav.tiltaksarrangor.api

import jakarta.servlet.http.HttpServletRequest
import no.nav.security.token.support.core.api.Unprotected
import no.nav.tiltaksarrangor.client.AmtDeltakerClient
import no.nav.tiltaksarrangor.repositories.DeltakerRepository
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/internal")
class InternalAPI(
	private val amtDeltakerClient: AmtDeltakerClient,
	private val deltakerRepository: DeltakerRepository,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	@Unprotected
	@GetMapping("/deltakere/trigger-oppdatering-av-oppfolgingsperiode")
	fun oppdaterPersonidenter(servlet: HttpServletRequest) {
		if (!isInternal(servlet)) {
			throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
		}
		val deltakere = deltakerRepository.getDeltakereUtenOppfolgingsperiode()
		if (deltakere.isEmpty()) {
			log.info("Ingen deltakere funnet uten oppfølgingsperiode.")
			return
		}

		log.info("Trigger oppdatering av oppfølgingsperiode for ${deltakere.size} deltakere.")
		deltakere.chunked(1000).forEach {
			amtDeltakerClient.reproduserDeltakere(it)
		}
	}

	private fun isInternal(servlet: HttpServletRequest): Boolean = servlet.remoteAddr == "127.0.0.1"
}
