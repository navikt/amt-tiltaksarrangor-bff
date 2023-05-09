package no.nav.tiltaksarrangor.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Deltaker(
	val id: UUID,
	val deltakerliste: Deltakerliste,
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
	val dagerPerUke: Int?,
	val soktInnPa: String,
	val soktInnDato: LocalDateTime,
	val tiltakskode: String,
	val bestillingTekst: String?,
	val fjernesDato: LocalDateTime?,
	val navInformasjon: NavInformasjon,
	val veiledere: List<Veileder>,
	val aktiveEndringsmeldinger: List<Endringsmelding>
) {
	data class Deltakerliste(
		val id: UUID,
		val startDato: LocalDate?,
		val sluttDato: LocalDate?,
		val erKurs: Boolean
	)
}
