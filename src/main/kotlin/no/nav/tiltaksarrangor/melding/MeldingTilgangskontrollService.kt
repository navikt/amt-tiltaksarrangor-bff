package no.nav.tiltaksarrangor.melding

import no.nav.tiltaksarrangor.model.exceptions.UnauthorizedException
import no.nav.tiltaksarrangor.repositories.DeltakerRepository
import no.nav.tiltaksarrangor.repositories.model.AnsattDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerlisteDbo
import no.nav.tiltaksarrangor.service.AnsattService
import no.nav.tiltaksarrangor.service.TilgangskontrollService
import no.nav.tiltaksarrangor.service.TokenService
import no.nav.tiltaksarrangor.unleash.UnleashService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class MeldingTilgangskontrollService(
	private val tokenService: TokenService,
	private val ansattService: AnsattService,
	private val deltakerRepository: DeltakerRepository,
	private val tilgangskontrollService: TilgangskontrollService,
	private val unleashService: UnleashService,
) {
	fun <T> medTilgangTilAnsattOgDeltaker(
		deltakerId: UUID,
		block: (ansatt: AnsattDbo, deltaker: DeltakerDbo, deltakerliste: DeltakerlisteDbo) -> T,
	): T {
		val personident = tokenService.getPersonligIdentTilInnloggetAnsatt()
		val ansatt = ansattService.getAnsattMedRoller(personident)
		val deltakerMedDeltakerliste = deltakerRepository
			.getDeltakerMedDeltakerliste(deltakerId)
			?.takeIf { it.deltakerliste.erTilgjengeligForArrangor() }
			?: throw NoSuchElementException("Fant ikke deltaker med id $deltakerId")

		if (!unleashService.erKometMasterForTiltakstype(deltakerMedDeltakerliste.deltakerliste.tiltakType)) {
			throw UnauthorizedException("Endepunkt er utilgjenglig")
		}

		tilgangskontrollService.verifiserTilgangTilDeltakerOgMeldinger(ansatt, deltakerMedDeltakerliste)

		return block(ansatt, deltakerMedDeltakerliste.deltaker, deltakerMedDeltakerliste.deltakerliste)
	}
}
