package no.nav.tiltaksarrangor.client.amttiltak.dto

import java.time.LocalDate
import java.util.UUID

data class GjennomforingDto(
	val id: UUID,
	val navn: String,
	val startDato: LocalDate?,
	val sluttDato: LocalDate?,
	val status: Status,
	val tiltak: TiltakDto,
	val arrangor: ArrangorDto,
	val erKurs: Boolean
) {
	enum class Status {
		APENT_FOR_INNSOK, GJENNOMFORES, AVSLUTTET
	}
}
