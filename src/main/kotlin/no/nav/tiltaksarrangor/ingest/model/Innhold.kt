package no.nav.tiltaksarrangor.ingest.model

import no.nav.tiltaksarrangor.model.DeltakerStatusAarsak
import java.time.LocalDate

sealed class Innhold {
	data class LeggTilOppstartsdatoInnhold(
		val oppstartsdato: LocalDate
	) : Innhold()

	data class EndreOppstartsdatoInnhold(
		val oppstartsdato: LocalDate
	) : Innhold()

	data class ForlengDeltakelseInnhold(
		val sluttdato: LocalDate
	) : Innhold()

	data class AvsluttDeltakelseInnhold(
		val sluttdato: LocalDate,
		val aarsak: DeltakerStatusAarsak
	) : Innhold()

	data class DeltakerIkkeAktuellInnhold(
		val aarsak: DeltakerStatusAarsak
	) : Innhold()

	data class EndreDeltakelseProsentInnhold(
		val nyDeltakelseProsent: Int,
		val gyldigFraDato: LocalDate?
	) : Innhold()

	data class EndreSluttdatoInnhold(
		val sluttdato: LocalDate
	) : Innhold()

	class TilbyPlassInnhold : Innhold()

	class SettPaaVentelisteInnhold : Innhold()
}
