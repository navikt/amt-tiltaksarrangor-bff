package no.nav.tiltaksarrangor.client.amttiltak.request

import no.nav.amt.lib.models.arrangor.melding.Vurderingstype
import java.time.LocalDateTime
import java.util.UUID

data class RegistrerVurderingRequest(
	val id: UUID,
	val opprettet: LocalDateTime,
	val vurderingstype: Vurderingstype,
	val begrunnelse: String?,
)
