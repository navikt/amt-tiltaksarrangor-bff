package no.nav.tiltaksarrangor.controller.request

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
		JsonSubTypes.Type(value = Innhold.EndreDeltakelseProsentInnhold::class, name = "ENDRE_DELTAKELSE_PROSENT")
	)
	val innhold: Innhold
) {
	enum class EndringsmeldingType {
		LEGG_TIL_OPPSTARTSDATO,
		ENDRE_OPPSTARTSDATO,
		FORLENG_DELTAKELSE,
		AVSLUTT_DELTAKELSE,
		DELTAKER_IKKE_AKTUELL,
		ENDRE_DELTAKELSE_PROSENT
	}

	sealed class Innhold {
		data class LeggTilOppstartsdatoInnhold(
			val type: EndringsmeldingType = EndringsmeldingType.LEGG_TIL_OPPSTARTSDATO,
			val oppstartsdato: LocalDate
		) : Innhold()

		data class EndreOppstartsdatoInnhold(
			val type: EndringsmeldingType = EndringsmeldingType.ENDRE_OPPSTARTSDATO,
			val oppstartsdato: LocalDate
		) : Innhold()

		data class EndreDeltakelseProsentInnhold(
			val type: EndringsmeldingType = EndringsmeldingType.ENDRE_DELTAKELSE_PROSENT,
			val deltakelseProsent: Int,
			val gyldigFraDato: LocalDate?
		) : Innhold()

		data class ForlengDeltakelseInnhold(
			val type: EndringsmeldingType = EndringsmeldingType.FORLENG_DELTAKELSE,
			val sluttdato: LocalDate
		) : Innhold()

		data class AvsluttDeltakelseInnhold(
			val type: EndringsmeldingType = EndringsmeldingType.AVSLUTT_DELTAKELSE,
			val sluttdato: LocalDate,
			val aarsak: DeltakerStatusAarsak
		) : Innhold()

		data class DeltakerIkkeAktuellInnhold(
			val type: EndringsmeldingType = EndringsmeldingType.DELTAKER_IKKE_AKTUELL,
			val aarsak: DeltakerStatusAarsak
		) : Innhold()
	}
}
