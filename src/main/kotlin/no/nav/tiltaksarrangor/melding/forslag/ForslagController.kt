package no.nav.tiltaksarrangor.melding.forslag

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tiltaksarrangor.melding.forslag.request.AvsluttDeltakelseRequest
import no.nav.tiltaksarrangor.melding.forslag.request.DeltakelsesmengdeRequest
import no.nav.tiltaksarrangor.melding.forslag.request.ForlengDeltakelseRequest
import no.nav.tiltaksarrangor.melding.forslag.request.ForslagRequest
import no.nav.tiltaksarrangor.melding.forslag.request.IkkeAktuellRequest
import no.nav.tiltaksarrangor.model.exceptions.UnauthorizedException
import no.nav.tiltaksarrangor.repositories.DeltakerRepository
import no.nav.tiltaksarrangor.repositories.model.AnsattDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerMedDeltakerlisteDbo
import no.nav.tiltaksarrangor.service.AnsattService
import no.nav.tiltaksarrangor.service.TilgangskontrollService
import no.nav.tiltaksarrangor.service.TokenService
import no.nav.tiltaksarrangor.unleash.UnleashService
import no.nav.tiltaksarrangor.utils.Issuer
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/tiltaksarrangor/deltaker/{deltakerId}/forslag")
class ForslagController(
	private val tokenService: TokenService,
	private val tilgangskontrollService: TilgangskontrollService,
	private val ansattService: AnsattService,
	private val deltakerRepository: DeltakerRepository,
	private val forslagService: ForslagService,
	private val unleashService: UnleashService,
) {
	@PostMapping("/forleng")
	@ProtectedWithClaims(issuer = Issuer.TOKEN_X)
	fun forleng(
		@PathVariable deltakerId: UUID,
		@RequestBody request: ForlengDeltakelseRequest,
	): AktivtForslagResponse = opprettForslag(deltakerId, request)

	@PostMapping("/avslutt")
	@ProtectedWithClaims(issuer = Issuer.TOKEN_X)
	fun avslutt(
		@PathVariable deltakerId: UUID,
		@RequestBody request: AvsluttDeltakelseRequest,
	): AktivtForslagResponse = opprettForslag(deltakerId, request)

	@PostMapping("/ikke-aktuell")
	@ProtectedWithClaims(issuer = Issuer.TOKEN_X)
	fun ikkeAktuell(
		@PathVariable deltakerId: UUID,
		@RequestBody request: IkkeAktuellRequest,
	): AktivtForslagResponse = opprettForslag(deltakerId, request)

	@PostMapping("/deltakelsesmengde")
	@ProtectedWithClaims(issuer = Issuer.TOKEN_X)
	fun deltakelsesmengde(
		@PathVariable deltakerId: UUID,
		@RequestBody request: DeltakelsesmengdeRequest,
	): AktivtForslagResponse = opprettForslag(deltakerId, request)

	@PostMapping("/{forslagId}/tilbakekall")
	@ProtectedWithClaims(issuer = Issuer.TOKEN_X)
	fun tilbakekall(
		@PathVariable deltakerId: UUID,
		@PathVariable forslagId: UUID,
	) {
		medAnsattOgDeltaker(deltakerId) { ansatt, _ ->
			forslagService.tilbakekall(forslagId, ansatt)
		}
	}

	private fun opprettForslag(deltakerId: UUID, request: ForslagRequest) = medAnsattOgDeltaker(deltakerId) { ansatt, deltaker ->
		val forslag = forslagService.opprettForslag(
			request,
			ansatt,
			deltaker,
		)
		forslag.tilAktivtForslagResponse()
	}

	private fun <T> medAnsattOgDeltaker(deltakerId: UUID, block: (ansatt: AnsattDbo, deltaker: DeltakerMedDeltakerlisteDbo) -> T): T {
		val personident = tokenService.getPersonligIdentTilInnloggetAnsatt()
		val ansatt = ansattService.getAnsattMedRoller(personident)
		val deltakerMedDeltakerliste = deltakerRepository
			.getDeltakerMedDeltakerliste(deltakerId)
			?.takeIf { it.deltakerliste.erTilgjengeligForArrangor() }
			?: throw NoSuchElementException("Fant ikke deltaker med id $deltakerId")

		if (!unleashService.erForslagSkruddPa(deltakerMedDeltakerliste.deltakerliste.tiltakType)) {
			throw UnauthorizedException("Endepunkt er utilgjenglig")
		}

		tilgangskontrollService.verifiserTilgangTilDeltakerOgMeldinger(ansatt, deltakerMedDeltakerliste)

		return block(ansatt, deltakerMedDeltakerliste)
	}
}
