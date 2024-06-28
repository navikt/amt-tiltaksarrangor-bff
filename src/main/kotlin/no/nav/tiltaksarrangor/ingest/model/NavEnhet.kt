package no.nav.tiltaksarrangor.ingest.model

import java.util.UUID

data class NavEnhet(
	val id: UUID,
	val enhetId: String,
	val navn: String,
)
