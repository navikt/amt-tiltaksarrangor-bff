package no.nav.tiltaksarrangor.ingest.model

import java.time.LocalDate
import java.util.UUID

data class DeltakerlisteFraValpDto(
	val id: UUID,
	val tiltakstype: Tiltakstype,
	val navn: String,
	val startDato: LocalDate,
	val sluttDato: LocalDate? = null,
	val status: Status,
	val virksomhetsnummer: String,
	val oppstart: Oppstartstype
) {

	data class Tiltakstype(
		val id: UUID,
		val navn: String,
		val arenaKode: String
	)

	enum class Status {
		GJENNOMFORES,
		AVBRUTT,
		AVLYST,
		AVSLUTTET,
		APENT_FOR_INNSOK;
	}

	enum class Oppstartstype {
		LOPENDE,
		FELLES
	}

	fun erKurs(): Boolean {
		return oppstart == Oppstartstype.FELLES
	}
}
