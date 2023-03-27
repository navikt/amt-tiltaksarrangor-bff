package no.nav.tiltaksarrangor.veileder.service

import no.nav.tiltaksarrangor.client.AmtTiltakClient
import no.nav.tiltaksarrangor.client.dto.VeiledersDeltakerDto
import no.nav.tiltaksarrangor.client.dto.toEndringsmelding
import no.nav.tiltaksarrangor.veileder.model.Deltaker
import no.nav.tiltaksarrangor.veileder.model.Veiledertype
import org.springframework.stereotype.Component

@Component
class VeilederService(
	private val amtTiltakClient: AmtTiltakClient
) {
	fun getMineDeltakere(): List<Deltaker> {
		val mineDeltakere = amtTiltakClient.getVeiledersDeltakere()
		return mineDeltakere.map { it.toDeltaker() }
	}
}

private fun VeiledersDeltakerDto.toDeltaker(): Deltaker {
	return Deltaker(
		id = id,
		deltakerliste = Deltaker.Deltakerliste(
			id = deltakerliste.id,
			navn = deltakerliste.navn
		),
		fornavn = fornavn,
		mellomnavn = mellomnavn,
		etternavn = etternavn,
		fodselsnummer = fodselsnummer,
		status = status,
		startDato = startDato,
		sluttDato = sluttDato,
		veiledertype = if (erMedveilederFor) {
			Veiledertype.MEDVEILEDER
		} else {
			Veiledertype.VEILEDER
		},
		aktiveEndringsmeldinger = aktiveEndringsmeldinger.map { it.toEndringsmelding() }
	)
}
