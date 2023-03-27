package no.nav.tiltaksarrangor.client.dto

import no.nav.tiltaksarrangor.model.DeltakerStatus
import java.time.LocalDate
import java.util.UUID

data class VeiledersDeltakerDto(
	val id: UUID,
	val fornavn: String,
	val mellomnavn: String?,
	val etternavn: String,
	val fodselsnummer: String,
	val startDato: LocalDate?,
	val sluttDato: LocalDate?,
	val status: DeltakerStatus,
	val deltakerliste: DeltakerlisteDto,
	val erMedveilederFor: Boolean,
	val aktiveEndringsmeldinger: List<EndringsmeldingDto>
)
