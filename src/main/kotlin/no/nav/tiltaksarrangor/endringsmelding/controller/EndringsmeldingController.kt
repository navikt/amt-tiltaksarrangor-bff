package no.nav.tiltaksarrangor.endringsmelding.controller

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tiltaksarrangor.endringsmelding.controller.request.EndringsmeldingRequest
import no.nav.tiltaksarrangor.endringsmelding.service.EndringsmeldingService
import no.nav.tiltaksarrangor.model.Endringsmelding
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
class EndringsmeldingController(
	private val endringsmeldingService: EndringsmeldingService,
	private val tokenService: TokenService
) {
	@GetMapping("/deltaker/{deltakerId}/endringsmeldinger")
	@ProtectedWithClaims(issuer = Issuer.TOKEN_X)
	fun getAktiveEndringsmeldinger(@PathVariable deltakerId: UUID): List<Endringsmelding> {
		return endringsmeldingService.getAktiveEndringsmeldinger(deltakerId)
	}

	@PostMapping("/deltaker/{deltakerId}/endringsmelding")
	@ProtectedWithClaims(issuer = Issuer.TOKEN_X)
	fun opprettEndringsmelding(
		@PathVariable deltakerId: UUID,
		@RequestBody request: EndringsmeldingRequest
	) {
		endringsmeldingService.opprettEndringsmelding(deltakerId, request)
	}

	@DeleteMapping("/endringsmelding/{endringsmeldingId}")
	@ProtectedWithClaims(issuer = Issuer.TOKEN_X)
	fun slettEndringsmelding(
		@PathVariable endringsmeldingId: UUID
	) {
		val personIdent = tokenService.getPersonligIdentTilInnloggetAnsatt()
		endringsmeldingService.slettEndringsmelding(endringsmeldingId, personIdent)
	}
}
