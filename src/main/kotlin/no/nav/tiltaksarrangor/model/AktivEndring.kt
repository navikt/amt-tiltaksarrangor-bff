package no.nav.tiltaksarrangor.model

import no.nav.amt.lib.models.arrangor.melding.Forslag
import java.time.LocalDate
import java.util.UUID

data class AktivEndring(
	val deltakerId: UUID,
	val endingsType: EndringsType,
	val type: Type,
	val sendt: LocalDate,
) {
	enum class Type {
		Forslag,
		Endringsmelding,
	}

	enum class EndringsType {
		LeggTilOppstartsDato,
		ForlengDeltakelse,
		IkkeAktuell,
		AvsluttDeltakelse,
		Deltakelsesmengde,
		Sluttdato,
		Startdato,
		Sluttarsak,
	}
}

fun getTypeFromEndringsmelding(t: Endringsmelding.Type): AktivEndring.EndringsType {
	return when (t) {
		Endringsmelding.Type.ENDRE_OPPSTARTSDATO -> AktivEndring.EndringsType.Startdato
		Endringsmelding.Type.LEGG_TIL_OPPSTARTSDATO -> AktivEndring.EndringsType.LeggTilOppstartsDato
		Endringsmelding.Type.FORLENG_DELTAKELSE -> AktivEndring.EndringsType.ForlengDeltakelse
		Endringsmelding.Type.DELTAKER_IKKE_AKTUELL -> AktivEndring.EndringsType.IkkeAktuell
		Endringsmelding.Type.ENDRE_SLUTTAARSAK -> AktivEndring.EndringsType.Sluttarsak
		Endringsmelding.Type.AVSLUTT_DELTAKELSE -> AktivEndring.EndringsType.AvsluttDeltakelse
		Endringsmelding.Type.ENDRE_DELTAKELSE_PROSENT -> AktivEndring.EndringsType.Deltakelsesmengde
		Endringsmelding.Type.ENDRE_SLUTTDATO -> AktivEndring.EndringsType.Sluttdato
	}
}

fun getTypeFromForslag(ednring: Forslag.Endring): AktivEndring.EndringsType {
	return when (ednring) {
		is Forslag.ForlengDeltakelse -> AktivEndring.EndringsType.ForlengDeltakelse
		is Forslag.IkkeAktuell -> AktivEndring.EndringsType.IkkeAktuell
		is Forslag.Sluttarsak -> AktivEndring.EndringsType.Sluttarsak
		is Forslag.AvsluttDeltakelse -> AktivEndring.EndringsType.AvsluttDeltakelse
		is Forslag.Deltakelsesmengde -> AktivEndring.EndringsType.Deltakelsesmengde
		is Forslag.Sluttdato -> AktivEndring.EndringsType.Sluttdato
		is Forslag.Startdato -> AktivEndring.EndringsType.Startdato
	}
}
