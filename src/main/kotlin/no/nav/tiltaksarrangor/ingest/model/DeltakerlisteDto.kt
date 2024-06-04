package no.nav.tiltaksarrangor.ingest.model

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
		PLANLAGT,
		GJENNOMFORES,
		AVBRUTT,
		AVLYST,
		AVSLUTTET,
		APENT_FOR_INNSOK,
	}

	enum class Oppstartstype {
		LOPENDE,
		FELLES,
	}

	fun erKurs(): Boolean {
		if (oppstart != null) {
			return oppstart == Oppstartstype.FELLES
		} else {
			return kursTiltak.contains(tiltakstype.arenaKode)
		}
	}

	private val kursTiltak =
		setOf(
			"JOBBK",
			"GRUPPEAMO",
			"GRUFAGYRKE",
		)

	fun toDeltakerlisteStatus(): DeltakerlisteStatus {
		return when (status) {
			Status.PLANLAGT, Status.APENT_FOR_INNSOK -> DeltakerlisteStatus.PLANLAGT
			Status.GJENNOMFORES -> DeltakerlisteStatus.GJENNOMFORES
			Status.AVSLUTTET -> DeltakerlisteStatus.AVSLUTTET
			else -> throw IllegalStateException("Ukjent status: $status")
		}
	}
}
