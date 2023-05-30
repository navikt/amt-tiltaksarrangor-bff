package no.nav.tiltaksarrangor.ingest.model

enum class EndringsmeldingType {
	LEGG_TIL_OPPSTARTSDATO,
	ENDRE_OPPSTARTSDATO,
	ENDRE_DELTAKELSE_PROSENT,
	FORLENG_DELTAKELSE,
	AVSLUTT_DELTAKELSE,
	DELTAKER_IKKE_AKTUELL,
	DELTAKER_ER_AKTUELL,
	ENDRE_SLUTTDATO
}

val typerUtenInnhold = listOf<EndringsmeldingType>(
	EndringsmeldingType.DELTAKER_ER_AKTUELL
)
