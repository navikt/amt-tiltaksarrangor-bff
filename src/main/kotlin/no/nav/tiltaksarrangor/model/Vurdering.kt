package no.nav.tiltaksarrangor.model

import java.time.LocalDateTime

data class Vurdering(
	val vurderingstype: Vurderingstype,
	val begrunnelse: String?,
	val gyldigFra: LocalDateTime,
	val gyldigTil: LocalDateTime?,
)
