package no.nav.tiltaksarrangor.service

import no.nav.tiltaksarrangor.client.AmtTiltakClient
import no.nav.tiltaksarrangor.client.dto.EndringsmeldingDto
import no.nav.tiltaksarrangor.client.dto.VeilederDto
import no.nav.tiltaksarrangor.model.Deltaker
import no.nav.tiltaksarrangor.model.DeltakerSluttAarsak
import no.nav.tiltaksarrangor.model.Endringsmelding
import no.nav.tiltaksarrangor.model.NavInformasjon
import no.nav.tiltaksarrangor.model.NavVeileder
import no.nav.tiltaksarrangor.model.Veileder
import no.nav.tiltaksarrangor.model.Veiledertype
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class TiltaksarrangorService(
	private val amtTiltakClient: AmtTiltakClient
) {
	fun getMineRoller(): List<String> {
		return amtTiltakClient.getMineRoller()
	}

	fun getDeltaker(deltakerId: UUID): Deltaker {
		val deltaker = amtTiltakClient.getDeltaker(deltakerId)
		val veiledere = amtTiltakClient.getVeiledere(deltakerId)
		val aktiveEndringsmeldinger = amtTiltakClient.getAktiveEndringsmeldinger(deltakerId)

		return Deltaker(
			id = deltaker.id,
			deltakerliste = Deltaker.Deltakerliste(
				id = deltaker.gjennomforing.id,
				startDato = deltaker.gjennomforing.startDato,
				sluttDato = deltaker.gjennomforing.sluttDato
			),
			fornavn = deltaker.fornavn,
			mellomnavn = deltaker.mellomnavn,
			etternavn = deltaker.etternavn,
			fodselsnummer = deltaker.fodselsnummer,
			telefonnummer = deltaker.telefonnummer,
			epost = deltaker.epost,
			status = deltaker.status,
			startDato = deltaker.startDato,
			sluttDato = deltaker.sluttDato,
			deltakelseProsent = deltaker.deltakelseProsent,
			soktInnPa = deltaker.gjennomforing.navn,
			soktInnDato = deltaker.registrertDato,
			tiltakskode = deltaker.gjennomforing.tiltak.tiltakskode,
			bestillingTekst = deltaker.innsokBegrunnelse,
			fjernesDato = deltaker.fjernesDato,
			navInformasjon = NavInformasjon(
				navkontor = deltaker.navEnhet?.navn,
				navVeileder = deltaker.navVeileder?.let {
					NavVeileder(
						navn = it.navn,
						epost = it.epost,
						telefon = it.telefon
					)
				}
			),
			veiledere = veiledere.map { it.toVeileder() },
			aktiveEndringsmeldinger = aktiveEndringsmeldinger.map { it.toEndringsmelding() }
		)
	}

	fun getAktiveEndringsmeldinger(deltakerId: UUID): List<Endringsmelding> {
		return amtTiltakClient.getAktiveEndringsmeldinger(deltakerId).map { it.toEndringsmelding() }
	}
}

private fun VeilederDto.toVeileder(): Veileder {
	return Veileder(
		id = id,
		ansattId = ansattId,
		deltakerId = deltakerId,
		veiledertype = if (erMedveileder) {
			Veiledertype.MEDVEILEDER
		} else {
			Veiledertype.VEILEDER
		},
		fornavn = fornavn,
		mellomnavn = mellomnavn,
		etternavn = etternavn
	)
}

private fun EndringsmeldingDto.toEndringsmelding(): Endringsmelding {
	return Endringsmelding(
		id = id,
		innhold = innhold.toEndringsmeldingInnhold()
	)
}

private fun EndringsmeldingDto.Innhold.toEndringsmeldingInnhold(): Endringsmelding.Innhold {
	return when (this) {
		is EndringsmeldingDto.Innhold.LeggTilOppstartsdatoInnhold -> Endringsmelding.Innhold.LeggTilOppstartsdatoInnhold(
			this.oppstartsdato
		)

		is EndringsmeldingDto.Innhold.EndreOppstartsdatoInnhold -> Endringsmelding.Innhold.EndreOppstartsdatoInnhold(
			this.oppstartsdato
		)

		is EndringsmeldingDto.Innhold.ForlengDeltakelseInnhold -> Endringsmelding.Innhold.ForlengDeltakelseInnhold(this.sluttdato)
		is EndringsmeldingDto.Innhold.EndreDeltakelseProsentInnhold -> Endringsmelding.Innhold.EndreDeltakelseProsentInnhold(
			this.deltakelseProsent,
			this.gyldigFraDato
		)

		is EndringsmeldingDto.Innhold.AvsluttDeltakelseInnhold -> Endringsmelding.Innhold.AvsluttDeltakelseInnhold(
			this.sluttdato,
			DeltakerSluttAarsak.valueOf(this.aarsak.type.name),
			this.aarsak.beskrivelse
		)

		is EndringsmeldingDto.Innhold.DeltakerIkkeAktuellInnhold -> Endringsmelding.Innhold.DeltakerIkkeAktuellInnhold(
			DeltakerSluttAarsak.valueOf(this.aarsak.type.name),
			this.aarsak.beskrivelse
		)
	}
}
