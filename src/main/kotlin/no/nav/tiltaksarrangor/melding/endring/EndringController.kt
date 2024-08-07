package no.nav.tiltaksarrangor.melding.endring

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tiltaksarrangor.melding.MeldingTilgangskontrollService
import no.nav.tiltaksarrangor.melding.endring.request.EndringFraArrangorRequest
import no.nav.tiltaksarrangor.melding.endring.request.LeggTilOppstartsdatoRequest
import no.nav.tiltaksarrangor.repositories.model.DeltakerlisteDbo
import no.nav.tiltaksarrangor.utils.Issuer
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/tiltaksarrangor/deltaker/{deltakerId}/endring")
class EndringController(
	private val tilgangskontrollService: MeldingTilgangskontrollService,
	private val endringService: EndringService,
) {
	@PostMapping("/startdato")
	@ProtectedWithClaims(issuer = Issuer.TOKEN_X)
	fun oppstartsdato(
		@PathVariable deltakerId: UUID,
		@RequestBody request: LeggTilOppstartsdatoRequest,
	) = opprettEndring(deltakerId, request)

	private fun opprettEndring(deltakerId: UUID, request: EndringFraArrangorRequest) =
		tilgangskontrollService.medTilgangTilAnsattOgDeltaker(deltakerId) { ansatt, deltaker, deltakerliste ->
			valider(request, deltakerliste)
			endringService.endreDeltaker(deltaker, deltakerliste, ansatt, request)
		}

	private fun valider(request: EndringFraArrangorRequest, deltakerliste: DeltakerlisteDbo) {
		when (request) {
			is LeggTilOppstartsdatoRequest -> validerOppstartsdato(
				request.startdato,
				request.sluttdato,
				deltakerliste,
			)
		}
	}
}
