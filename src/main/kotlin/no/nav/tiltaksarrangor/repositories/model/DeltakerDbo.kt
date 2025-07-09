package no.nav.tiltaksarrangor.repositories.model

import no.nav.amt.lib.models.arrangor.melding.Vurdering
import no.nav.amt.lib.models.deltaker.Deltakelsesinnhold
import no.nav.amt.lib.models.deltaker.DeltakerHistorikk
import no.nav.tiltaksarrangor.consumer.model.AdresseDto
import no.nav.tiltaksarrangor.consumer.model.Oppfolgingsperiode
import no.nav.tiltaksarrangor.model.DeltakerStatusAarsak
import no.nav.tiltaksarrangor.model.Kilde
import no.nav.tiltaksarrangor.model.StatusType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class DeltakerDbo(
	val id: UUID,
	val deltakerlisteId: UUID,
	val personident: String,
	val fornavn: String,
	val mellomnavn: String?,
	val etternavn: String,
	val telefonnummer: String?,
	val epost: String?,
	val erSkjermet: Boolean,
	val adresse: AdresseDto?,
	val status: StatusType,
	val statusOpprettetDato: LocalDateTime,
	val statusGyldigFraDato: LocalDateTime,
	val statusAarsak: DeltakerStatusAarsak?,
	val dagerPerUke: Float?,
	val prosentStilling: Double?,
	val startdato: LocalDate?,
	val sluttdato: LocalDate?,
	val innsoktDato: LocalDate,
	val bestillingstekst: String?,
	val navKontor: String?,
	val navVeilederId: UUID?,
	val navVeilederNavn: String?,
	val navVeilederEpost: String?,
	val navVeilederTelefon: String?,
	val skjultAvAnsattId: UUID?,
	val skjultDato: LocalDateTime?,
	val vurderingerFraArrangor: List<Vurdering>?,
	val adressebeskyttet: Boolean,
	val innhold: Deltakelsesinnhold?,
	val kilde: Kilde?,
	val historikk: List<DeltakerHistorikk>,
	val sistEndret: LocalDateTime,
	val forsteVedtakFattet: LocalDate?,
	val erManueltDeltMedArrangor: Boolean,
	val oppfolgingsperioder: List<Oppfolgingsperiode> = emptyList(),
) {
	fun erSkjult(): Boolean = skjultDato != null

	fun skalFjernesDato(): LocalDateTime? = if (status in STATUSER_SOM_KAN_SKJULES) {
		statusGyldigFraDato.plusDays(DAGER_AVSLUTTET_DELTAKER_VISES)
	} else {
		null
	}

	fun skalVises(): Boolean {
		if (sluttdato == null) {
			return true
		}

		val dato = LocalDate.now().minusDays(DAGER_AVSLUTTET_DELTAKER_VISES)
		return sluttdato.isAfter(dato)
	}
}

val STATUSER_SOM_KAN_SKJULES =
	listOf(
		StatusType.IKKE_AKTUELL,
		StatusType.HAR_SLUTTET,
		StatusType.FULLFORT,
		StatusType.AVBRUTT,
	)

const val DAGER_AVSLUTTET_DELTAKER_VISES = 40L
