package no.nav.tiltaksarrangor.historikk

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tiltaksarrangor.model.DeltakerHistorikk
import no.nav.tiltaksarrangor.service.TiltaksarrangorService
import no.nav.tiltaksarrangor.service.TokenService
import no.nav.tiltaksarrangor.utils.Issuer
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/tiltaksarrangor/deltaker/{deltakerId}/historikk")
class HistorikkController(
	private val tokenService: TokenService,
	private val tiltaksarrangorService: TiltaksarrangorService,
) {
	@GetMapping
	@ProtectedWithClaims(issuer = Issuer.TOKEN_X)
	fun getAlleDeltakerlister(
		@PathVariable deltakerId: UUID,
	): List<DeltakerHistorikk> {
		val personIdent = tokenService.getPersonligIdentTilInnloggetAnsatt()
		return tiltaksarrangorService.getDeltaker(personIdent, deltakerId).historikk
	}
}
