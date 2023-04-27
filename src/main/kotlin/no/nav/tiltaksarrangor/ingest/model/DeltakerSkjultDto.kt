package no.nav.tiltaksarrangor.ingest.model

import java.time.LocalDateTime
import java.util.UUID

data class DeltakerSkjultDto(
	val skjultAvAnsattId: UUID,
	val dato: LocalDateTime
)
