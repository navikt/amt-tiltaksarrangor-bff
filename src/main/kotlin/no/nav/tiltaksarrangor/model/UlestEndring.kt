package no.nav.tiltaksarrangor.model

import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.util.UUID

data class UlestEndring(
	val id: UUID,
	val deltakerId: UUID,
	val oppdatering: Oppdatering,
) {
	fun erSvarFraNav(): Boolean = when (oppdatering) {
		is Oppdatering.DeltakelsesEndring -> oppdatering.endring.forslag != null
		is Oppdatering.AvvistForslag -> true
	}

	fun erOppdateringFraNav(): Boolean = !erSvarFraNav()
}

@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed interface Oppdatering {
	data class DeltakelsesEndring(
		val endring: no.nav.amt.lib.models.deltaker.DeltakerEndring,
	) : Oppdatering

	data class AvvistForslag(
		val forslag: no.nav.amt.lib.models.arrangor.melding.Forslag,
	) : Oppdatering
}
