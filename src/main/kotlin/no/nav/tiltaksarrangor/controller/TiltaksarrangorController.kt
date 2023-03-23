package no.nav.tiltaksarrangor.controller

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tiltaksarrangor.model.Deltaker
import no.nav.tiltaksarrangor.service.TiltaksarrangorService
import no.nav.tiltaksarrangor.utils.Issuer
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/tiltaksarrangor")
class TiltaksarrangorController(
	private val tiltaksarrangorService: TiltaksarrangorService
) {
	@GetMapping("/meg/roller")
	@ProtectedWithClaims(issuer = Issuer.TOKEN_X)
	fun getMineRoller(): List<String> {
		return tiltaksarrangorService.getMineRoller()
	}

	@GetMapping("/deltaker/{deltakerId}")
	@ProtectedWithClaims(issuer = Issuer.TOKEN_X)
	fun getDeltaker(@PathVariable deltakerId: UUID): Deltaker {
		return tiltaksarrangorService.getDeltaker(deltakerId)
	}
}
