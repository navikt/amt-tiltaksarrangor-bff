package no.nav.tiltaksarrangor.ingest.model

import no.nav.tiltaksarrangor.model.Veiledertype
import java.util.UUID

data class VeilederDto(
	val deltakerId: UUID,
	val type: Veiledertype
)
