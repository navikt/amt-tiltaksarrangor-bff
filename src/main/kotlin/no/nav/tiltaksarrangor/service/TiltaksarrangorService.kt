package no.nav.tiltaksarrangor.service

import no.nav.tiltaksarrangor.client.AmtTiltakClient
import no.nav.tiltaksarrangor.client.dto.toEndringsmelding
import no.nav.tiltaksarrangor.model.Deltaker
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
			aktiveEndringsmeldinger = aktiveEndringsmeldinger.map { it.toEndringsmelding() }
		)
	}
}
