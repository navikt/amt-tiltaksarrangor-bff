package no.nav.tiltaksarrangor.ingest.model

import java.util.UUID

data class DeltakerlisteArrangorDto(
	val id: UUID,
	val organisasjonsnummer: String,
	val navn: String,
)
