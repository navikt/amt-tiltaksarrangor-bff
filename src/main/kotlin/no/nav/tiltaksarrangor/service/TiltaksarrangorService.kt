package no.nav.tiltaksarrangor.service

import no.nav.tiltaksarrangor.client.AmtTiltakClient
import no.nav.tiltaksarrangor.client.dto.VeilederDto
import no.nav.tiltaksarrangor.client.dto.toEndringsmelding
import no.nav.tiltaksarrangor.client.request.AvsluttDeltakelseRequest
import no.nav.tiltaksarrangor.client.request.DeltakerIkkeAktuellRequest
import no.nav.tiltaksarrangor.client.request.EndreDeltakelsesprosentRequest
import no.nav.tiltaksarrangor.client.request.EndreOppstartsdatoRequest
import no.nav.tiltaksarrangor.client.request.ForlengDeltakelseRequest
import no.nav.tiltaksarrangor.client.request.LeggTilOppstartsdatoRequest
import no.nav.tiltaksarrangor.controller.request.EndringsmeldingRequest
import no.nav.tiltaksarrangor.model.Deltaker
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

	fun opprettEndringsmelding(deltakerId: UUID, request: EndringsmeldingRequest) {
		when (request.innhold) {
			is EndringsmeldingRequest.Innhold.LeggTilOppstartsdatoInnhold -> amtTiltakClient.leggTilOppstartsdato(deltakerId, LeggTilOppstartsdatoRequest(request.innhold.oppstartsdato))
			is EndringsmeldingRequest.Innhold.EndreOppstartsdatoInnhold -> amtTiltakClient.endreOppstartsdato(deltakerId, EndreOppstartsdatoRequest(request.innhold.oppstartsdato))
			is EndringsmeldingRequest.Innhold.AvsluttDeltakelseInnhold -> amtTiltakClient.avsluttDeltakelse(deltakerId, AvsluttDeltakelseRequest(request.innhold.sluttdato, request.innhold.aarsak))
			is EndringsmeldingRequest.Innhold.ForlengDeltakelseInnhold -> amtTiltakClient.forlengDeltakelse(deltakerId, ForlengDeltakelseRequest(request.innhold.sluttdato))
			is EndringsmeldingRequest.Innhold.EndreDeltakelseProsentInnhold -> amtTiltakClient.endreDeltakelsesprosent(deltakerId, EndreDeltakelsesprosentRequest(request.innhold.deltakelseProsent, request.innhold.gyldigFraDato))
			is EndringsmeldingRequest.Innhold.DeltakerIkkeAktuellInnhold -> amtTiltakClient.deltakerIkkeAktuell(deltakerId, DeltakerIkkeAktuellRequest(request.innhold.aarsak))
		}
	}

	fun slettEndringsmelding(endringsmeldingId: UUID) {
		amtTiltakClient.tilbakekallEndringsmelding(endringsmeldingId)
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
