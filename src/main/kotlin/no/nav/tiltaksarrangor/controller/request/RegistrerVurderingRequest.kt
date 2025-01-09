package no.nav.tiltaksarrangor.controller.request

import no.nav.amt.lib.models.arrangor.melding.Vurderingstype

data class RegistrerVurderingRequest(
	val vurderingstype: Vurderingstype,
	val begrunnelse: String?,
)
