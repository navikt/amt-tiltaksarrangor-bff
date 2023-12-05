package no.nav.tiltaksarrangor.ingest.model

import java.time.LocalDateTime

data class DeltakerStatusDto(
	val type: DeltakerStatus,
	val gyldigFra: LocalDateTime,
	val opprettetDato: LocalDateTime,
)
