package no.nav.tiltaksarrangor.koordinator.model

import java.time.LocalDate
import java.util.UUID

data class Deltakerliste(
	val id: UUID,
	val navn: String,
	val tiltaksnavn: String,
	val arrangorNavn: String,
	val startDato: LocalDate?,
	val sluttDato: LocalDate?,
	val status: Status,
	val koordinatorer: List<Koordinator>,
	val deltakere: List<Deltaker>,
	val erKurs: Boolean,
	val tiltakType: String
) {
	enum class Status {
		PLANLAGT, GJENNOMFORES, AVSLUTTET
	}
}
