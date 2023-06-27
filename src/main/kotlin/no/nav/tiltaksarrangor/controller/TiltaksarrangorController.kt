package no.nav.tiltaksarrangor.controller

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tiltaksarrangor.controller.request.EndringsmeldingRequest
import no.nav.tiltaksarrangor.model.Deltaker
import no.nav.tiltaksarrangor.model.Endringsmelding
import no.nav.tiltaksarrangor.service.TiltaksarrangorService
import no.nav.tiltaksarrangor.service.TokenService
import no.nav.tiltaksarrangor.utils.Issuer
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
	private val tiltaksarrangorService: TiltaksarrangorService
) {
	@GetMapping("/meg/roller")
	@ProtectedWithClaims(issuer = Issuer.TOKEN_X)
	fun getMineRoller(): List<String> {
		val personIdent = tokenService.getPersonligIdentTilInnloggetAnsatt()
		return tiltaksarrangorService.getMineRoller(personIdent)
	}

	@GetMapping("/deltaker/{deltakerId}")
	@ProtectedWithClaims(issuer = Issuer.TOKEN_X)
	fun getDeltaker(@PathVariable deltakerId: UUID): Deltaker {
		val personIdent = tokenService.getPersonligIdentTilInnloggetAnsatt()
		return tiltaksarrangorService.getDeltaker(personIdent, deltakerId)
	}

	@DeleteMapping("/deltaker/{deltakerId}")
	@ProtectedWithClaims(issuer = Issuer.TOKEN_X)
	fun fjernDeltaker(
		@PathVariable deltakerId: UUID
	) {
		tiltaksarrangorService.fjernDeltaker(deltakerId)
	}

	@GetMapping("/deltaker/{deltakerId}/endringsmeldinger")
	@ProtectedWithClaims(issuer = Issuer.TOKEN_X)
	fun getAktiveEndringsmeldinger(@PathVariable deltakerId: UUID): List<Endringsmelding> {
		return tiltaksarrangorService.getAktiveEndringsmeldinger(deltakerId)
	}

	@PostMapping("/deltaker/{deltakerId}/endringsmelding")
	@ProtectedWithClaims(issuer = Issuer.TOKEN_X)
	fun opprettEndringsmelding(
		@PathVariable deltakerId: UUID,
		@RequestBody request: EndringsmeldingRequest
	) {
		tiltaksarrangorService.opprettEndringsmelding(deltakerId, request)
	}

	@DeleteMapping("/endringsmelding/{endringsmeldingId}")
	@ProtectedWithClaims(issuer = Issuer.TOKEN_X)
	fun slettEndringsmelding(
		@PathVariable endringsmeldingId: UUID
	) {
		tiltaksarrangorService.slettEndringsmelding(endringsmeldingId)
	}
}
