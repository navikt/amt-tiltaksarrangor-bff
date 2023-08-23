package no.nav.tiltaksarrangor.controller.request

import no.nav.tiltaksarrangor.model.Vurderingstype

data class RegistrerVurderingRequest(
	val vurderingstype: Vurderingstype,
	val begrunnelse: String?
)
