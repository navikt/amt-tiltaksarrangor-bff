package no.nav.tiltaksarrangor.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Deltaker(
	val id: UUID,
	val deltakerlisteId: UUID,
	val fornavn: String,
	val mellomnavn: String?,
	val etternavn: String,
	val fodselsnummer: String,
	val telefonnummer: String?,
	val epost: String?,
	val status: DeltakerStatus,
	val startDato: LocalDate?,
	val sluttDato: LocalDate?,
	val deltakelseProsent: Int?,
	val soktInnPa: String,
	val soktInnDato: LocalDateTime,
	val tiltakskode: String,
	val bestillingTekst: String?,
	val fjernesDato: LocalDateTime?,
	val navInformasjon: NavInformasjon,
	val aktiveEndringsmeldinger: List<Endringsmelding>
)
