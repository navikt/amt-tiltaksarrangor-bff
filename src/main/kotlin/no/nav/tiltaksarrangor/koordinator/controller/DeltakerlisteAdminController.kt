package no.nav.tiltaksarrangor.koordinator.controller

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tiltaksarrangor.koordinator.model.AdminDeltakerliste
import no.nav.tiltaksarrangor.koordinator.service.DeltakerlisteAdminService
import no.nav.tiltaksarrangor.utils.Issuer
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/tiltaksarrangor/koordinator/admin")
class DeltakerlisteAdminController(
	private val deltakerlisteAdminService: DeltakerlisteAdminService
) {

	@GetMapping("/deltakerlister")
	@ProtectedWithClaims(issuer = Issuer.TOKEN_X)
	fun getAlleDeltakerlister(): List<AdminDeltakerliste> {
		return deltakerlisteAdminService.getAlleDeltakerlister()
	}

	@PostMapping("/deltakerliste/{deltakerlisteId}")
	@ProtectedWithClaims(issuer = Issuer.TOKEN_X)
	fun leggTilDeltakerliste(
		@PathVariable deltakerlisteId: UUID
	) {
		return deltakerlisteAdminService.leggTilDeltakerliste(deltakerlisteId)
	}

	@DeleteMapping("/deltakerliste/{deltakerlisteId}")
	@ProtectedWithClaims(issuer = Issuer.TOKEN_X)
	fun fjernDeltakerliste(
		@PathVariable deltakerlisteId: UUID
	) {
		return deltakerlisteAdminService.fjernDeltakerliste(deltakerlisteId)
	}
}
