package no.nav.tiltaksarrangor.ingest.model

enum class EndringsmeldingType {
	LEGG_TIL_OPPSTARTSDATO,
	ENDRE_OPPSTARTSDATO,
	ENDRE_DELTAKELSE_PROSENT,
	FORLENG_DELTAKELSE,
	AVSLUTT_DELTAKELSE,
	DELTAKER_IKKE_AKTUELL,
	TILBY_PLASS,
	ENDRE_SLUTTDATO
}

val typerUtenInnhold = listOf<EndringsmeldingType>(
	EndringsmeldingType.TILBY_PLASS
)
