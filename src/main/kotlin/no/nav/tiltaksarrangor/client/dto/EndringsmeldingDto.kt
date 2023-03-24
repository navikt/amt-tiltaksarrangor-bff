package no.nav.tiltaksarrangor.client.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDate
import java.util.UUID

data class EndringsmeldingDto(
	val id: UUID,
	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
	@JsonSubTypes(
		JsonSubTypes.Type(value = Innhold.LeggTilOppstartsdatoInnhold::class, name = "LEGG_TIL_OPPSTARTSDATO"),
		JsonSubTypes.Type(value = Innhold.EndreOppstartsdatoInnhold::class, name = "ENDRE_OPPSTARTSDATO"),
		JsonSubTypes.Type(value = Innhold.ForlengDeltakelseInnhold::class, name = "FORLENG_DELTAKELSE"),
		JsonSubTypes.Type(value = Innhold.AvsluttDeltakelseInnhold::class, name = "AVSLUTT_DELTAKELSE"),
		JsonSubTypes.Type(value = Innhold.DeltakerIkkeAktuellInnhold::class, name = "DELTAKER_IKKE_AKTUELL"),
		JsonSubTypes.Type(value = Innhold.EndreDeltakelseProsentInnhold::class, name = "ENDRE_DELTAKELSE_PROSENT")
	)
	val innhold: Innhold,
	val type: String
) {
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
	}
}
