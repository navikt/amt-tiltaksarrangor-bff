package no.nav.tiltaksarrangor.endringsmelding.controller.request

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tiltaksarrangor.model.DeltakerStatusAarsak
import java.time.LocalDate

data class EndringsmeldingRequest(
	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
	@JsonSubTypes(
		JsonSubTypes.Type(value = Innhold.LeggTilOppstartsdatoInnhold::class, name = "LEGG_TIL_OPPSTARTSDATO"),
		JsonSubTypes.Type(value = Innhold.EndreOppstartsdatoInnhold::class, name = "ENDRE_OPPSTARTSDATO"),
		JsonSubTypes.Type(value = Innhold.ForlengDeltakelseInnhold::class, name = "FORLENG_DELTAKELSE"),
		JsonSubTypes.Type(value = Innhold.AvsluttDeltakelseInnhold::class, name = "AVSLUTT_DELTAKELSE"),
		JsonSubTypes.Type(value = Innhold.DeltakerIkkeAktuellInnhold::class, name = "DELTAKER_IKKE_AKTUELL"),
		JsonSubTypes.Type(value = Innhold.EndreDeltakelseProsentInnhold::class, name = "ENDRE_DELTAKELSE_PROSENT"),
		JsonSubTypes.Type(value = Innhold.EndreSluttdatoInnhold::class, name = "ENDRE_SLUTTDATO"),
		JsonSubTypes.Type(value = Innhold.DeltakerErAktuellInnhold::class, name = "DELTAKER_ER_AKTUELL")
	)
	val innhold: Innhold
) {
	enum class EndringsmeldingType {
		LEGG_TIL_OPPSTARTSDATO,
		ENDRE_OPPSTARTSDATO,
		FORLENG_DELTAKELSE,
		AVSLUTT_DELTAKELSE,
		DELTAKER_IKKE_AKTUELL,
		ENDRE_DELTAKELSE_PROSENT,
		DELTAKER_ER_AKTUELL,
		ENDRE_SLUTTDATO
	}

	sealed class Innhold(val type: EndringsmeldingType) {

		data class LeggTilOppstartsdatoInnhold(
			val oppstartsdato: LocalDate
		) : Innhold(EndringsmeldingType.LEGG_TIL_OPPSTARTSDATO)

		data class EndreOppstartsdatoInnhold(
			val oppstartsdato: LocalDate
		) : Innhold(EndringsmeldingType.ENDRE_OPPSTARTSDATO)

		data class EndreDeltakelseProsentInnhold(
			val deltakelseProsent: Int,
			val dagerPerUke: Int?,
			val gyldigFraDato: LocalDate?
		) : Innhold(EndringsmeldingType.ENDRE_DELTAKELSE_PROSENT)

		data class ForlengDeltakelseInnhold(
			val sluttdato: LocalDate
		) : Innhold(EndringsmeldingType.FORLENG_DELTAKELSE)

		data class AvsluttDeltakelseInnhold(
			val sluttdato: LocalDate,
			val aarsak: DeltakerStatusAarsak
		) : Innhold(EndringsmeldingType.AVSLUTT_DELTAKELSE)

		data class DeltakerIkkeAktuellInnhold(
			val aarsak: DeltakerStatusAarsak
		) : Innhold(EndringsmeldingType.DELTAKER_IKKE_AKTUELL)

		data class EndreSluttdatoInnhold(
			val sluttdato: LocalDate
		) : Innhold(EndringsmeldingType.ENDRE_SLUTTDATO)

		class DeltakerErAktuellInnhold : Innhold(EndringsmeldingType.DELTAKER_ER_AKTUELL)
	}
}
