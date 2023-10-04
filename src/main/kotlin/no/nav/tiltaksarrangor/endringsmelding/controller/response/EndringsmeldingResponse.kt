package no.nav.tiltaksarrangor.endringsmelding.controller.response

import no.nav.tiltaksarrangor.model.Endringsmelding

data class EndringsmeldingResponse(
	val aktiveEndringsmeldinger: List<Endringsmelding>,
	val historiskeEndringsmeldinger: List<Endringsmelding>
)
