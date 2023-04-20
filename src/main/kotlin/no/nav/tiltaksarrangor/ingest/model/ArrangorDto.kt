package no.nav.tiltaksarrangor.ingest.model

import no.nav.tiltaksarrangor.ingest.repositories.model.ArrangorDbo
import java.util.UUID

data class ArrangorDto(
	val id: UUID,
	val navn: String,
	val organisasjonsnummer: String,
	val overordnetArrangorId: UUID?,
	val deltakerlister: List<UUID>
)

fun ArrangorDto.toArrangorDbo(): ArrangorDbo {
	return ArrangorDbo(
		id = id,
		navn = navn,
		organisasjonsnummer = organisasjonsnummer,
		overordnetArrangorId = overordnetArrangorId
	)
}
