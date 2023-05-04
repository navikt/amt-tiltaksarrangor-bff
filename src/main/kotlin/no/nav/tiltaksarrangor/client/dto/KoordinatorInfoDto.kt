package no.nav.tiltaksarrangor.client.dto

import java.time.LocalDate
import java.util.UUID

data class KoordinatorInfoDto(
	val deltakerlister: List<DeltakerlisteDto>
) {
	data class DeltakerlisteDto(
		val id: UUID,
		val navn: String,
		val type: String,
		val startdato: LocalDate?,
		val sluttdato: LocalDate?,
		val erKurs: Boolean
	)
}
