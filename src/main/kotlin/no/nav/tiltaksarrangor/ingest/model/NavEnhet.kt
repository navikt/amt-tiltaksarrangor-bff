package no.nav.tiltaksarrangor.ingest.model

import java.util.UUID

data class NavEnhet(
	val id: UUID,
	val enhetsnummer: String,
	val navn: String,
)
