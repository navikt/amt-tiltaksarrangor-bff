package no.nav.tiltaksarrangor.service

import no.nav.tiltaksarrangor.client.AmtTiltakClient
import no.nav.tiltaksarrangor.client.dto.toEndringsmelding
import no.nav.tiltaksarrangor.client.dto.toStatus
import no.nav.tiltaksarrangor.client.dto.toVeileder
import no.nav.tiltaksarrangor.client.request.AvsluttDeltakelseRequest
import no.nav.tiltaksarrangor.client.request.DeltakerIkkeAktuellRequest
import no.nav.tiltaksarrangor.client.request.EndreDeltakelsesprosentRequest
import no.nav.tiltaksarrangor.client.request.EndreOppstartsdatoRequest
import no.nav.tiltaksarrangor.client.request.EndreSluttdatoRequest
import no.nav.tiltaksarrangor.client.request.ForlengDeltakelseRequest
import no.nav.tiltaksarrangor.client.request.LeggTilOppstartsdatoRequest
import no.nav.tiltaksarrangor.controller.request.EndringsmeldingRequest
import no.nav.tiltaksarrangor.model.Deltaker
import no.nav.tiltaksarrangor.model.Endringsmelding
import no.nav.tiltaksarrangor.model.NavInformasjon
import no.nav.tiltaksarrangor.model.NavVeileder
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
				sluttDato = deltaker.gjennomforing.sluttDato,
				erKurs = deltaker.gjennomforing.erKurs
			),
			fornavn = deltaker.fornavn,
			mellomnavn = deltaker.mellomnavn,
			etternavn = deltaker.etternavn,
			fodselsnummer = deltaker.fodselsnummer,
			telefonnummer = deltaker.telefonnummer,
			epost = deltaker.epost,
			status = deltaker.status.toStatus(deltaker.gjennomforing.erKurs),
			startDato = deltaker.startDato,
			sluttDato = deltaker.sluttDato,
			deltakelseProsent = deltaker.deltakelseProsent,
			dagerPerUke = deltaker.dagerPerUke,
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

	fun fjernDeltaker(deltakerId: UUID) {
		amtTiltakClient.skjulDeltakerForTiltaksarrangor(deltakerId)
	}

	fun getAktiveEndringsmeldinger(deltakerId: UUID): List<Endringsmelding> {
		return amtTiltakClient.getAktiveEndringsmeldinger(deltakerId).map { it.toEndringsmelding() }
	}

	fun opprettEndringsmelding(deltakerId: UUID, request: EndringsmeldingRequest) {
		when (request.innhold.type) {
			EndringsmeldingRequest.EndringsmeldingType.LEGG_TIL_OPPSTARTSDATO ->
				amtTiltakClient.leggTilOppstartsdato(deltakerId, LeggTilOppstartsdatoRequest((request.innhold as EndringsmeldingRequest.Innhold.LeggTilOppstartsdatoInnhold).oppstartsdato))

			EndringsmeldingRequest.EndringsmeldingType.ENDRE_OPPSTARTSDATO ->
				amtTiltakClient.endreOppstartsdato(deltakerId, EndreOppstartsdatoRequest((request.innhold as EndringsmeldingRequest.Innhold.EndreOppstartsdatoInnhold).oppstartsdato))

			EndringsmeldingRequest.EndringsmeldingType.AVSLUTT_DELTAKELSE -> {
				val innhold = request.innhold as EndringsmeldingRequest.Innhold.AvsluttDeltakelseInnhold
				amtTiltakClient.avsluttDeltakelse(deltakerId, AvsluttDeltakelseRequest(innhold.sluttdato, innhold.aarsak))
			}
			EndringsmeldingRequest.EndringsmeldingType.FORLENG_DELTAKELSE -> amtTiltakClient.forlengDeltakelse(deltakerId, ForlengDeltakelseRequest((request.innhold as EndringsmeldingRequest.Innhold.ForlengDeltakelseInnhold).sluttdato))
			EndringsmeldingRequest.EndringsmeldingType.ENDRE_DELTAKELSE_PROSENT -> {
				val innhold = request.innhold as EndringsmeldingRequest.Innhold.EndreDeltakelseProsentInnhold
				amtTiltakClient.endreDeltakelsesprosent(deltakerId, EndreDeltakelsesprosentRequest(innhold.deltakelseProsent, innhold.dagerPerUke, innhold.gyldigFraDato))
			}
			EndringsmeldingRequest.EndringsmeldingType.DELTAKER_IKKE_AKTUELL -> amtTiltakClient.deltakerIkkeAktuell(deltakerId, DeltakerIkkeAktuellRequest((request.innhold as EndringsmeldingRequest.Innhold.DeltakerIkkeAktuellInnhold).aarsak))
			EndringsmeldingRequest.EndringsmeldingType.ENDRE_SLUTTDATO -> amtTiltakClient.endreSluttdato(deltakerId, EndreSluttdatoRequest((request.innhold as EndringsmeldingRequest.Innhold.EndreSluttdatoInnhold).sluttdato))
			EndringsmeldingRequest.EndringsmeldingType.TILBY_PLASS -> amtTiltakClient.tilbyPlass(deltakerId)
			EndringsmeldingRequest.EndringsmeldingType.SETT_PAA_VENTELISTE -> amtTiltakClient.settPaaVenteliste(deltakerId)
		}
	}

	fun slettEndringsmelding(endringsmeldingId: UUID) {
		amtTiltakClient.tilbakekallEndringsmelding(endringsmeldingId)
	}
}
