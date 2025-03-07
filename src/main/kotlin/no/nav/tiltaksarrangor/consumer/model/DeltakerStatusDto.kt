package no.nav.tiltaksarrangor.consumer.model

import no.nav.tiltaksarrangor.model.DeltakerStatusAarsak
import java.time.LocalDateTime

data class DeltakerStatusDto(
	val type: DeltakerStatus,
	val gyldigFra: LocalDateTime,
	val opprettetDato: LocalDateTime,
	val aarsak: DeltakerStatusAarsak.Type?,
	val aarsaksbeskrivelse: String?,
)
