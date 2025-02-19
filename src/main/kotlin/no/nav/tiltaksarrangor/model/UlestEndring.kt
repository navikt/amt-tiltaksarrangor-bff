package no.nav.tiltaksarrangor.model

import no.nav.amt.lib.models.arrangor.melding.Forslag
import no.nav.amt.lib.models.arrangor.melding.Forslag.Status
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

	fun hentNavAnsattId(): UUID = when (oppdatering) {
		is Oppdatering.DeltakelsesEndring -> oppdatering.endring.endretAv
		is Oppdatering.AvvistForslag -> oppdatering.forslag.getNavAnsattForEndring().id
	}

	fun navEnheter(): UUID = when (oppdatering) {
		is Oppdatering.DeltakelsesEndring -> oppdatering.endring.endretAvEnhet
		is Oppdatering.AvvistForslag -> oppdatering.forslag.getNavAnsattForEndring().enhetId
	}
}

private fun Forslag.getNavAnsattForEndring(): Forslag.NavAnsatt = when (val status = this.status) {
	is Status.Avvist -> status.avvistAv
	is Status.Godkjent,
	is Status.Erstattet,
	is Status.Tilbakekalt,
	Status.VenterPaSvar,
	-> throw IllegalStateException("Forslaget har status $status som ikke skal brukes i uleste endringer")
}

sealed interface Oppdatering {
	data class DeltakelsesEndring(
		val endring: no.nav.amt.lib.models.deltaker.DeltakerEndring,
	) : Oppdatering

	data class AvvistForslag(
		val forslag: no.nav.amt.lib.models.arrangor.melding.Forslag,
	) : Oppdatering
}
