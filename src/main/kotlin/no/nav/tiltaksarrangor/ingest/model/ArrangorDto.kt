package no.nav.tiltaksarrangor.ingest.model

import no.nav.tiltaksarrangor.ingest.repositories.model.ArrangorDbo
import java.util.UUID

data class ArrangorDto(
	val id: UUID,
	val organisasjon: OrganisasjonDto,
	val overordnetOrganisasjon: OrganisasjonDto?,
	val deltakerlister: List<UUID>
)

data class OrganisasjonDto(
	val nummer: String,
	val navn: String
)

fun ArrangorDto.toArrangorDbo(): ArrangorDbo {
	return ArrangorDbo(
		id = id,
		navn = organisasjon.navn,
		organisasjonsnummer = organisasjon.nummer,
		overordnetEnhetNavn = overordnetOrganisasjon?.navn,
		overordnetEnhetOrganisasjonsnummer = overordnetOrganisasjon?.nummer
	)
}
