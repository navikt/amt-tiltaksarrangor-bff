package no.nav.tiltaksarrangor.ingest.model

enum class EndringsmeldingType {
	LEGG_TIL_OPPSTARTSDATO,
	ENDRE_OPPSTARTSDATO,
	ENDRE_DELTAKELSE_PROSENT,
	FORLENG_DELTAKELSE,
	AVSLUTT_DELTAKELSE,
	DELTAKER_IKKE_AKTUELL,
	TILBY_PLASS,
	SETT_PAA_VENTELISTE,
	ENDRE_SLUTTDATO
}

val typerUtenInnhold = listOf<EndringsmeldingType>(
	EndringsmeldingType.TILBY_PLASS,
	EndringsmeldingType.SETT_PAA_VENTELISTE
)
