package no.nav.tiltaksarrangor.client.dto

import java.time.LocalDate
import java.util.UUID

data class GjennomforingDto(
	val id: UUID,
	val navn: String,
	val startDato: LocalDate?,
	val sluttDato: LocalDate?,
	val status: Status,
	val tiltak: TiltakDto,
	val arrangor: ArrangorDto
) {
	enum class Status {
		IKKE_STARTET, GJENNOMFORES, AVSLUTTET
	}
}
