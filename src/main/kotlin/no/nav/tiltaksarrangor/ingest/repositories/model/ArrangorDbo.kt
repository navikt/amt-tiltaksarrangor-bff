package no.nav.tiltaksarrangor.ingest.repositories.model

import java.util.UUID

data class ArrangorDbo(
	val id: UUID,
	val navn: String,
	val organisasjonsnummer: String,
	val overordnetEnhetNavn: String?,
	val overordnetEnhetOrganisasjonsnummer: String?
)
