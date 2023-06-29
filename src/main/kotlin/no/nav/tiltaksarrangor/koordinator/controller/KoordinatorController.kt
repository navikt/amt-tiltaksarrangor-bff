package no.nav.tiltaksarrangor.koordinator.controller

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tiltaksarrangor.koordinator.model.Deltakerliste
import no.nav.tiltaksarrangor.koordinator.model.LeggTilVeiledereRequest
import no.nav.tiltaksarrangor.koordinator.model.MineDeltakerlister
import no.nav.tiltaksarrangor.koordinator.model.TilgjengeligVeileder
import no.nav.tiltaksarrangor.koordinator.service.KoordinatorService
import no.nav.tiltaksarrangor.service.TokenService
import no.nav.tiltaksarrangor.utils.Issuer
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/tiltaksarrangor/koordinator")
class KoordinatorController(
	private val tokenService: TokenService,
	private val koordinatorService: KoordinatorService
) {
	@GetMapping("/mine-deltakerlister")
	@ProtectedWithClaims(issuer = Issuer.TOKEN_X)
	fun getMineDeltakerlister(): MineDeltakerlister {
		val personIdent = tokenService.getPersonligIdentTilInnloggetAnsatt()
		return koordinatorService.getMineDeltakerlister(personIdent)
	}

	@GetMapping("/deltakerliste/{deltakerlisteId}")
	@ProtectedWithClaims(issuer = Issuer.TOKEN_X)
	fun getDeltakerliste(
		@PathVariable deltakerlisteId: UUID
	): Deltakerliste {
		val personIdent = tokenService.getPersonligIdentTilInnloggetAnsatt()
		return koordinatorService.getDeltakerliste(deltakerlisteId, personIdent)
	}

	@GetMapping("/{deltakerlisteId}/veiledere")
	@ProtectedWithClaims(issuer = Issuer.TOKEN_X)
	fun getTilgjengeligeVeiledere(
		@PathVariable deltakerlisteId: UUID
	): List<TilgjengeligVeileder> {
		return koordinatorService.getTilgjengeligeVeiledere(deltakerlisteId)
	}

	@PostMapping("/veiledere", params = ["deltakerId"])
	@ProtectedWithClaims(issuer = Issuer.TOKEN_X)
	fun tildelVeiledereForDeltaker(
		@RequestParam("deltakerId") deltakerId: UUID,
		@RequestBody request: LeggTilVeiledereRequest
	) {
		koordinatorService.tildelVeiledereForDeltaker(deltakerId, request)
	}
}
