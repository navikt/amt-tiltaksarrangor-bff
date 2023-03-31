package no.nav.tiltaksarrangor.client.dto

import no.nav.tiltaksarrangor.model.DeltakerStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class DeltakerDto(
	val id: UUID,
	val fornavn: String,
	val mellomnavn: String? = null,
	val etternavn: String,
	val fodselsnummer: String,
	val startDato: LocalDate?,
	val sluttDato: LocalDate?,
	val status: DeltakerStatus,
	val registrertDato: LocalDateTime,
	val aktiveEndringsmeldinger: List<EndringsmeldingDto>,
	val aktiveVeiledere: List<VeilederDto>
)
