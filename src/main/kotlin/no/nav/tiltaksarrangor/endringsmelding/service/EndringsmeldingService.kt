package no.nav.tiltaksarrangor.endringsmelding.service

import no.nav.tiltaksarrangor.client.amttiltak.AmtTiltakClient
import no.nav.tiltaksarrangor.client.amttiltak.dto.toEndringsmelding
import no.nav.tiltaksarrangor.client.amttiltak.request.AvsluttDeltakelseRequest
import no.nav.tiltaksarrangor.client.amttiltak.request.DeltakerIkkeAktuellRequest
import no.nav.tiltaksarrangor.client.amttiltak.request.EndreDeltakelsesprosentRequest
import no.nav.tiltaksarrangor.client.amttiltak.request.EndreOppstartsdatoRequest
import no.nav.tiltaksarrangor.client.amttiltak.request.EndreSluttdatoRequest
import no.nav.tiltaksarrangor.client.amttiltak.request.ForlengDeltakelseRequest
import no.nav.tiltaksarrangor.client.amttiltak.request.LeggTilOppstartsdatoRequest
import no.nav.tiltaksarrangor.endringsmelding.controller.request.EndringsmeldingRequest
import no.nav.tiltaksarrangor.model.Endringsmelding
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class EndringsmeldingService(
	private val amtTiltakClient: AmtTiltakClient
) {
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
			EndringsmeldingRequest.EndringsmeldingType.DELTAKER_ER_AKTUELL -> amtTiltakClient.deltakerErAktuell(deltakerId)
		}
	}

	fun slettEndringsmelding(endringsmeldingId: UUID) {
		amtTiltakClient.tilbakekallEndringsmelding(endringsmeldingId)
	}
}
