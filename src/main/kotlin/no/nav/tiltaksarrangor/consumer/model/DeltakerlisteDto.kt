package no.nav.tiltaksarrangor.consumer.model

import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode
import no.nav.tiltaksarrangor.model.DeltakerlisteStatus
import java.time.LocalDate
import java.util.UUID

data class DeltakerlisteDto(
	val id: UUID,
	val tiltakstype: TiltakstypeDto,
	val navn: String,
	val startDato: LocalDate,
	val sluttDato: LocalDate? = null,
	val status: Status,
	val virksomhetsnummer: String,
	val oppstart: Oppstartstype,
	val tilgjengeligForArrangorFraOgMedDato: LocalDate?,
) {
	data class TiltakstypeDto(
		val id: UUID,
		val navn: String,
		val arenaKode: String, // String tar høyde for andre tiltakstyper enn det vi støtter
		val tiltakskode: String,
	) {
		fun erStottet() = this.tiltakskode in Tiltakskode.entries
			.filterNot { it.erEnkeltplass() }
			.toTypedArray()
			.map { it.name }
	}

	enum class Status {
		GJENNOMFORES,
		AVBRUTT,
		AVLYST,
		AVSLUTTET,
	}

	fun erKurs(): Boolean = oppstart == Oppstartstype.FELLES

	fun toDeltakerlisteStatus(): DeltakerlisteStatus = when (status) {
		Status.GJENNOMFORES -> DeltakerlisteStatus.GJENNOMFORES
		Status.AVSLUTTET -> DeltakerlisteStatus.AVSLUTTET
		else -> throw IllegalStateException("Ukjent status: $status")
	}
}

enum class Oppstartstype {
	LOPENDE,
	FELLES,
}
