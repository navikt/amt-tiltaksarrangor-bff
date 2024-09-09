package no.nav.tiltaksarrangor.ingest.model

import java.util.UUID

data class NavAnsatt(
	val id: UUID,
	val navIdent: String,
	val navn: String,
	val epost: String?,
	val telefon: String?,
)
