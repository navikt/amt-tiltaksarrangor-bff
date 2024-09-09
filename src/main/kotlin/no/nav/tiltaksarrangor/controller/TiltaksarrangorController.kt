package no.nav.tiltaksarrangor.controller

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tiltaksarrangor.controller.request.RegistrerVurderingRequest
import no.nav.tiltaksarrangor.model.Deltaker
import no.nav.tiltaksarrangor.service.TiltaksarrangorService
import no.nav.tiltaksarrangor.service.TokenService
import no.nav.tiltaksarrangor.utils.Issuer
import no.nav.tiltaksarrangor.utils.JsonUtils.objectMapper
import no.nav.tiltaksarrangor.utils.writePolymorphicListAsString
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/tiltaksarrangor")
class TiltaksarrangorController(
	private val tokenService: TokenService,
	private val tiltaksarrangorService: TiltaksarrangorService,
) {
	@GetMapping("/meg/roller")
	@ProtectedWithClaims(issuer = Issuer.TOKEN_X)
	fun getMineRoller(): List<String> {
		val personIdent = tokenService.getPersonligIdentTilInnloggetAnsatt()
		return tiltaksarrangorService.getMineRoller(personIdent)
	}

	@GetMapping("/deltaker/{deltakerId}")
	@ProtectedWithClaims(issuer = Issuer.TOKEN_X)
	fun getDeltaker(
		@PathVariable deltakerId: UUID,
	): Deltaker {
		val personIdent = tokenService.getPersonligIdentTilInnloggetAnsatt()
		return tiltaksarrangorService.getDeltaker(personIdent, deltakerId)
	}

	@GetMapping("/deltaker/{deltakerId}/historikk")
	@ProtectedWithClaims(issuer = Issuer.TOKEN_X)
	fun getDeltakerhistorikk(
		@PathVariable deltakerId: UUID,
	): String {
		val personIdent = tokenService.getPersonligIdentTilInnloggetAnsatt()
		return objectMapper.writePolymorphicListAsString(tiltaksarrangorService.getDeltakerHistorikk(personIdent, deltakerId))
	}

	@PostMapping("/deltaker/{deltakerId}/vurdering")
	@ProtectedWithClaims(issuer = Issuer.TOKEN_X)
	fun registrerVurdering(
		@PathVariable deltakerId: UUID,
		@RequestBody request: RegistrerVurderingRequest,
	) {
		val personIdent = tokenService.getPersonligIdentTilInnloggetAnsatt()
		tiltaksarrangorService.registrerVurdering(personIdent, deltakerId, request)
	}

	@DeleteMapping("/deltaker/{deltakerId}")
	@ProtectedWithClaims(issuer = Issuer.TOKEN_X)
	fun fjernDeltaker(
		@PathVariable deltakerId: UUID,
	) {
		val personIdent = tokenService.getPersonligIdentTilInnloggetAnsatt()
		tiltaksarrangorService.fjernDeltaker(personIdent, deltakerId)
	}
}
