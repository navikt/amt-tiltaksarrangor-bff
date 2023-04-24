package no.nav.tiltaksarrangor.client.dto

import no.nav.tiltaksarrangor.model.DeltakerStatus
import java.time.LocalDateTime

data class DeltakerStatusDto(
	val type: StatusType,
	val endretDato: LocalDateTime
)

enum class StatusType {
	VENTER_PA_OPPSTART, DELTAR, HAR_SLUTTET, IKKE_AKTUELL, VURDERES, AVBRUTT
}

fun DeltakerStatusDto.toStatus(erKurs: Boolean): DeltakerStatus {
	return DeltakerStatus(
		type = type.toStatusType(erKurs),
		endretDato = endretDato
	)
}

fun StatusType.toStatusType(erKurs: Boolean): no.nav.tiltaksarrangor.model.StatusType {
	return when (this) {
		StatusType.VENTER_PA_OPPSTART -> no.nav.tiltaksarrangor.model.StatusType.VENTER_PA_OPPSTART
		StatusType.DELTAR -> no.nav.tiltaksarrangor.model.StatusType.DELTAR
		StatusType.IKKE_AKTUELL -> no.nav.tiltaksarrangor.model.StatusType.IKKE_AKTUELL
		StatusType.VURDERES -> no.nav.tiltaksarrangor.model.StatusType.VURDERES
		StatusType.AVBRUTT -> no.nav.tiltaksarrangor.model.StatusType.AVBRUTT
		StatusType.HAR_SLUTTET -> {
			if (erKurs) {
				no.nav.tiltaksarrangor.model.StatusType.FULLFORT
			} else {
				no.nav.tiltaksarrangor.model.StatusType.HAR_SLUTTET
			}
		}
	}
}
