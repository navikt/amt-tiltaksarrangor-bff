package no.nav.tiltaksarrangor.controller

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tiltaksarrangor.client.AmtTiltakClient
import no.nav.tiltaksarrangor.utils.Issuer
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/tiltaksarrangor")
class TiltaksarrangorController(
	private val amtTiltakClient: AmtTiltakClient
) {
	private val log = LoggerFactory.getLogger(javaClass)

	@GetMapping("/meg/roller")
	@ProtectedWithClaims(issuer = Issuer.TOKEN_X)
	fun getMineRoller(): List<String> {
		log.info("Mottok kall for å hente roller")
		return amtTiltakClient.getMineRoller()
	}
}
