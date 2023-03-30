package no.nav.tiltaksarrangor.koordinator.controller

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tiltaksarrangor.koordinator.model.MineDeltakerlister
import no.nav.tiltaksarrangor.koordinator.service.KoordinatorService
import no.nav.tiltaksarrangor.utils.Issuer
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/tiltaksarrangor/koordinator")
class KoordinatorController(
	private val koordinatorService: KoordinatorService
) {
	@GetMapping("/mine-deltakerlister")
	@ProtectedWithClaims(issuer = Issuer.TOKEN_X)
	fun getMineDeltakere(): MineDeltakerlister {
		return koordinatorService.getMineDeltakerlister()
	}
}
