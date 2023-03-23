package no.nav.tiltaksarrangor.client.dto

import no.nav.tiltaksarrangor.model.DeltakerStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class DeltakerDetaljerDto(
	val id: UUID,
	val fornavn: String,
	val mellomnavn: String? = null,
	val etternavn: String,
	val fodselsnummer: String,
	val telefonnummer: String?,
	val epost: String?,
	val deltakelseProsent: Int?,
	val navEnhet: NavEnhetDto?,
	val navVeileder: NavVeilederDto?,
	val startDato: LocalDate?,
	val sluttDato: LocalDate?,
	val registrertDato: LocalDateTime,
	val status: DeltakerStatus,
	val gjennomforing: GjennomforingDto,
	val fjernesDato: LocalDateTime?,
	val innsokBegrunnelse: String?
)
