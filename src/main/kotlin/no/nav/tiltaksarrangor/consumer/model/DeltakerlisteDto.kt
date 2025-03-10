package no.nav.tiltaksarrangor.consumer.model

import no.nav.tiltaksarrangor.model.DeltakerlisteStatus
import java.time.LocalDate
import java.util.UUID

data class DeltakerlisteDto(
	val id: UUID,
	val tiltakstype: Tiltakstype,
	val navn: String,
	val startDato: LocalDate,
	val sluttDato: LocalDate? = null,
	val status: Status,
	val virksomhetsnummer: String,
	val oppstart: Oppstartstype?,
	val tilgjengeligForArrangorFraOgMedDato: LocalDate?,
) {
	data class Tiltakstype(
		val id: UUID,
		val navn: String,
		val arenaKode: String,
	)

	enum class Status {
		GJENNOMFORES,
		AVBRUTT,
		AVLYST,
		AVSLUTTET,
	}

	enum class Oppstartstype {
		LOPENDE,
		FELLES,
	}

	fun erKurs(): Boolean = if (oppstart != null) {
		oppstart == Oppstartstype.FELLES
	} else {
		kursTiltak.contains(tiltakstype.arenaKode)
	}

	private val kursTiltak =
		setOf(
			"JOBBK",
			"GRUPPEAMO",
			"GRUFAGYRKE",
		)

	fun toDeltakerlisteStatus(): DeltakerlisteStatus = when (status) {
		Status.GJENNOMFORES -> DeltakerlisteStatus.GJENNOMFORES
		Status.AVSLUTTET -> DeltakerlisteStatus.AVSLUTTET
		else -> throw IllegalStateException("Ukjent status: $status")
	}
}
