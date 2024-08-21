package no.nav.tiltaksarrangor.ingest.model

import no.nav.tiltaksarrangor.repositories.model.Deltakelsesinnhold
import no.nav.tiltaksarrangor.repositories.model.DeltakerDbo
import no.nav.tiltaksarrangor.repositories.model.STATUSER_SOM_KAN_SKJULES
import java.time.LocalDate
import java.util.UUID

data class DeltakerDto(
	val id: UUID,
	val deltakerlisteId: UUID,
	val personalia: DeltakerPersonaliaDto,
	val status: DeltakerStatusDto,
	val dagerPerUke: Float?,
	val prosentStilling: Double?,
	val oppstartsdato: LocalDate?,
	val sluttdato: LocalDate?,
	val innsoktDato: LocalDate,
	val bestillingTekst: String?,
	val navKontor: String?,
	val navVeileder: DeltakerNavVeilederDto?,
	val deltarPaKurs: Boolean,
	val vurderingerFraArrangor: List<VurderingDto>?,
	val innhold: Deltakelsesinnhold?,
)

fun DeltakerDto.toDeltakerDbo(lagretDeltaker: DeltakerDbo?): DeltakerDbo {
	val oppdatertStatus = status.type.toStatusType(deltarPaKurs)

	val skalFortsattSkjules = lagretDeltaker?.erSkjult() == true && oppdatertStatus in STATUSER_SOM_KAN_SKJULES

	return DeltakerDbo(
		id = id,
		deltakerlisteId = deltakerlisteId,
		personident = personalia.personident,
		fornavn = personalia.navn.fornavn,
		mellomnavn = personalia.navn.mellomnavn,
		etternavn = personalia.navn.etternavn,
		telefonnummer = personalia.kontaktinformasjon.telefonnummer,
		epost = personalia.kontaktinformasjon.epost,
		erSkjermet = personalia.skjermet,
		adresse = personalia.adresse,
		status = oppdatertStatus,
		statusOpprettetDato = status.opprettetDato,
		statusGyldigFraDato = status.gyldigFra,
		dagerPerUke = dagerPerUke,
		prosentStilling = prosentStilling,
		startdato = oppstartsdato,
		sluttdato = sluttdato,
		innsoktDato = innsoktDato,
		bestillingstekst = bestillingTekst,
		navKontor = navKontor,
		navVeilederId = navVeileder?.id,
		navVeilederNavn = navVeileder?.navn,
		navVeilederEpost = navVeileder?.epost,
		navVeilederTelefon = navVeileder?.telefonnummer,
		skjultAvAnsattId =
			if (skalFortsattSkjules) {
				lagretDeltaker?.skjultAvAnsattId
			} else {
				null
			},
		skjultDato =
			if (skalFortsattSkjules) {
				lagretDeltaker?.skjultDato
			} else {
				null
			},
		vurderingerFraArrangor = vurderingerFraArrangor,
		adressebeskyttet = personalia.adressebeskyttelse != null,
		innhold = innhold,
	)
}

val SKJULES_ALLTID_STATUSER =
	listOf(
		DeltakerStatus.SOKT_INN,
		DeltakerStatus.VENTELISTE,
		DeltakerStatus.PABEGYNT_REGISTRERING,
		DeltakerStatus.FEILREGISTRERT,
		DeltakerStatus.UTKAST_TIL_PAMELDING,
		DeltakerStatus.AVBRUTT_UTKAST,
	)

val AVSLUTTENDE_STATUSER =
	listOf(
		DeltakerStatus.HAR_SLUTTET,
		DeltakerStatus.IKKE_AKTUELL,
		DeltakerStatus.AVBRUTT,
		DeltakerStatus.FULLFORT,
	)
