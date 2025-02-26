package no.nav.tiltaksarrangor.model

import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.amt.lib.models.arrangor.melding.Forslag
import no.nav.amt.lib.models.arrangor.melding.Forslag.Status
import no.nav.amt.lib.models.deltaker.DeltakerEndring
import java.time.LocalDate
import java.util.UUID

data class UlestEndring(
	val id: UUID,
	val deltakerId: UUID,
	val oppdatering: Oppdatering,
	val oppdatert: LocalDate,
) {
	fun erSvarFraNav(): Boolean = when (oppdatering) {
		is Oppdatering.DeltakelsesEndring -> oppdatering.endring.forslag != null
		is Oppdatering.AvvistForslag -> true
		is Oppdatering.BrukerEndring,
		is Oppdatering.NavEndring,
		-> false
	}

	fun erOppdateringFraNav(): Boolean = !erSvarFraNav()

	fun hentNavAnsattId(): UUID? = when (oppdatering) {
		is Oppdatering.DeltakelsesEndring -> oppdatering.endring.endretAv
		is Oppdatering.AvvistForslag -> oppdatering.forslag.getNavAnsattForEndring().id
		is Oppdatering.BrukerEndring,
		is Oppdatering.NavEndring,
		-> null
	}

	fun navEnheter(): UUID? = when (oppdatering) {
		is Oppdatering.DeltakelsesEndring -> oppdatering.endring.endretAvEnhet
		is Oppdatering.AvvistForslag -> oppdatering.forslag.getNavAnsattForEndring().enhetId
		is Oppdatering.BrukerEndring,
		is Oppdatering.NavEndring,
		-> null
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

@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed interface Oppdatering {
	data class DeltakelsesEndring(
		val endring: DeltakerEndring,
	) : Oppdatering

	data class AvvistForslag(
		val forslag: Forslag,
	) : Oppdatering

	data class BrukerEndring(
		val telefonnummer: String?,
		val epost: String?,
	) : Oppdatering

	data class NavEndring(
		val navVeilederNavn: String?,
		val navVeilederEpost: String?,
		val navVeilederTelefonnummer: String?,
		val navEnhet: String?,
	) : Oppdatering

	val id get() = when (this) {
		is DeltakelsesEndring -> endring.id
		is AvvistForslag -> forslag.id
		is BrukerEndring,
		is NavEndring,
		-> UUID.randomUUID()
	}
}
