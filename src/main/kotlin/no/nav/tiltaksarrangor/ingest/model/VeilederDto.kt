package no.nav.tiltaksarrangor.ingest.model

import java.util.UUID

data class VeilederDto(
	val deltakerId: UUID,
	val type: Veiledertype
)
