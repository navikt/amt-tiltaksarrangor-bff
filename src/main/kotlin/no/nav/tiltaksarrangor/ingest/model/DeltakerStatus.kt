package no.nav.tiltaksarrangor.ingest.model

import no.nav.tiltaksarrangor.model.StatusType

enum class DeltakerStatus {
	VENTER_PA_OPPSTART,
	DELTAR,
	HAR_SLUTTET,
	IKKE_AKTUELL,
	FEILREGISTRERT,
	SOKT_INN,
	VURDERES,
	VENTELISTE,
	AVBRUTT,
	FULLFORT, // kursstatuser
	PABEGYNT_REGISTRERING,
}

fun DeltakerStatus.toStatusType(erKurs: Boolean): StatusType {
	return when (this) {
		DeltakerStatus.VENTER_PA_OPPSTART -> StatusType.VENTER_PA_OPPSTART
		DeltakerStatus.DELTAR -> StatusType.DELTAR
		DeltakerStatus.IKKE_AKTUELL -> StatusType.IKKE_AKTUELL
		DeltakerStatus.VURDERES -> StatusType.VURDERES
		DeltakerStatus.AVBRUTT -> StatusType.AVBRUTT
		DeltakerStatus.HAR_SLUTTET -> { // denne kan endres når amt-tiltak ikke lenger sender kursdeltakere med status HAR_SLUTTET
			if (erKurs) {
				StatusType.FULLFORT
			} else {
				StatusType.HAR_SLUTTET
			}
		}
		DeltakerStatus.FULLFORT -> StatusType.FULLFORT
		else -> {
			throw IllegalStateException("Status ${this.name} er ikke tillatt og skulle ikke vært lagret")
		}
	}
}
