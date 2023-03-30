package no.nav.tiltaksarrangor.client.dto

import no.nav.tiltaksarrangor.model.Endringsmelding

fun EndringsmeldingDto.toEndringsmelding(): Endringsmelding {
	return Endringsmelding(
		id = id,
		innhold = innhold.toEndringsmeldingInnhold()
	)
}

fun EndringsmeldingDto.Innhold.toEndringsmeldingInnhold(): Endringsmelding.Innhold {
	return when (this) {
		is EndringsmeldingDto.Innhold.LeggTilOppstartsdatoInnhold -> Endringsmelding.Innhold.LeggTilOppstartsdatoInnhold(this.oppstartsdato)
		is EndringsmeldingDto.Innhold.EndreOppstartsdatoInnhold -> Endringsmelding.Innhold.EndreOppstartsdatoInnhold(this.oppstartsdato)
		is EndringsmeldingDto.Innhold.ForlengDeltakelseInnhold -> Endringsmelding.Innhold.ForlengDeltakelseInnhold(this.sluttdato)
		is EndringsmeldingDto.Innhold.EndreDeltakelseProsentInnhold -> Endringsmelding.Innhold.EndreDeltakelseProsentInnhold(this.deltakelseProsent, this.gyldigFraDato)
		is EndringsmeldingDto.Innhold.AvsluttDeltakelseInnhold -> Endringsmelding.Innhold.AvsluttDeltakelseInnhold(this.sluttdato, this.aarsak)
		is EndringsmeldingDto.Innhold.DeltakerIkkeAktuellInnhold -> Endringsmelding.Innhold.DeltakerIkkeAktuellInnhold(this.aarsak)
	}
}