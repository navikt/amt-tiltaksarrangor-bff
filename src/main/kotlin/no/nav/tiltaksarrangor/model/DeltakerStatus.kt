package no.nav.tiltaksarrangor.model

import java.time.LocalDateTime

data class DeltakerStatus(
	val type: StatusType,
	val endretDato: LocalDateTime,
	val aarsak: DeltakerStatusAarsak?,
)

enum class StatusType {
	VENTER_PA_OPPSTART,
	DELTAR,
	HAR_SLUTTET,
	IKKE_AKTUELL,
	VURDERES,
	AVBRUTT,
	FULLFORT,
	SOKT_INN,
}
