package no.nav.tiltaksarrangor.model

import java.time.LocalDate
import java.util.UUID

data class Endringsmelding(
	val id: UUID,
	val innhold: Innhold
) {
	val type = innhold.type()

	enum class Type {
		LEGG_TIL_OPPSTARTSDATO,
		ENDRE_OPPSTARTSDATO,
		FORLENG_DELTAKELSE,
		AVSLUTT_DELTAKELSE,
		DELTAKER_IKKE_AKTUELL,
		ENDRE_DELTAKELSE_PROSENT
	}

	sealed class Innhold {
		data class LeggTilOppstartsdatoInnhold(
			val oppstartsdato: LocalDate
		) : Innhold()

		data class EndreOppstartsdatoInnhold(
			val oppstartsdato: LocalDate
		) : Innhold()

		data class EndreDeltakelseProsentInnhold(
			val deltakelseProsent: Int,
			val gyldigFraDato: LocalDate?
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

		fun type(): Type {
			return when (this) {
				is LeggTilOppstartsdatoInnhold -> Type.LEGG_TIL_OPPSTARTSDATO
				is EndreOppstartsdatoInnhold -> Type.ENDRE_OPPSTARTSDATO
				is ForlengDeltakelseInnhold -> Type.FORLENG_DELTAKELSE
				is AvsluttDeltakelseInnhold -> Type.AVSLUTT_DELTAKELSE
				is DeltakerIkkeAktuellInnhold -> Type.DELTAKER_IKKE_AKTUELL
				is EndreDeltakelseProsentInnhold -> Type.ENDRE_DELTAKELSE_PROSENT
			}
		}
	}
}
