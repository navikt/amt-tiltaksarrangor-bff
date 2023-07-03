package no.nav.tiltaksarrangor.endringsmelding.controller.request

import no.nav.tiltaksarrangor.ingest.model.EndringsmeldingType
import no.nav.tiltaksarrangor.ingest.model.Innhold
import no.nav.tiltaksarrangor.repositories.model.EndringsmeldingDbo
import java.util.UUID

fun EndringsmeldingRequest.toEndringsmeldingDbo(endringsmeldingId: UUID, deltakerId: UUID): EndringsmeldingDbo {
	return EndringsmeldingDbo(
		id = endringsmeldingId,
		deltakerId = deltakerId,
		type = EndringsmeldingType.valueOf(innhold.type.name),
		innhold = innhold.toEndringsmeldingInnhold()
	)
}

fun EndringsmeldingRequest.Innhold.toEndringsmeldingInnhold(): Innhold? {
	return when (this) {
		is EndringsmeldingRequest.Innhold.LeggTilOppstartsdatoInnhold -> Innhold.LeggTilOppstartsdatoInnhold(this.oppstartsdato)
		is EndringsmeldingRequest.Innhold.EndreOppstartsdatoInnhold -> Innhold.EndreOppstartsdatoInnhold(this.oppstartsdato)
		is EndringsmeldingRequest.Innhold.ForlengDeltakelseInnhold -> Innhold.ForlengDeltakelseInnhold(this.sluttdato)
		is EndringsmeldingRequest.Innhold.EndreDeltakelseProsentInnhold -> Innhold.EndreDeltakelseProsentInnhold(this.deltakelseProsent, this.dagerPerUke, this.gyldigFraDato)
		is EndringsmeldingRequest.Innhold.AvsluttDeltakelseInnhold -> Innhold.AvsluttDeltakelseInnhold(this.sluttdato, this.aarsak)
		is EndringsmeldingRequest.Innhold.DeltakerIkkeAktuellInnhold -> Innhold.DeltakerIkkeAktuellInnhold(this.aarsak)
		is EndringsmeldingRequest.Innhold.DeltakerErAktuellInnhold -> null
		is EndringsmeldingRequest.Innhold.EndreSluttdatoInnhold -> Innhold.EndreSluttdatoInnhold(this.sluttdato)
	}
}
