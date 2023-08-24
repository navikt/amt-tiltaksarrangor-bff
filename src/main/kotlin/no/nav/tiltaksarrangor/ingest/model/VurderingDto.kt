package no.nav.tiltaksarrangor.ingest.model

import no.nav.tiltaksarrangor.model.Vurdering
import no.nav.tiltaksarrangor.model.Vurderingstype
import no.nav.tiltaksarrangor.utils.JsonUtils
import org.postgresql.util.PGobject
import java.time.LocalDateTime
import java.util.UUID

data class VurderingDto(
	val id: UUID,
	val deltakerId: UUID,
	val vurderingstype: Vurderingstype,
	val begrunnelse: String?,
	val opprettetAvArrangorAnsattId: UUID,
	val gyldigFra: LocalDateTime,
	val gyldigTil: LocalDateTime?
) {
	fun toVurdering(): Vurdering {
		return Vurdering(
			vurderingstype = vurderingstype,
			begrunnelse = begrunnelse,
			gyldigFra = gyldigFra,
			gyldigTil = gyldigTil
		)
	}
}

fun List<VurderingDto>.toPGObject() = PGobject().also {
	it.type = "json"
	it.value = JsonUtils.objectMapper.writeValueAsString(this)
}
