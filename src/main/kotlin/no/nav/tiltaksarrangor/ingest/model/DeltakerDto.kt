package no.nav.tiltaksarrangor.ingest.model

import no.nav.tiltaksarrangor.repositories.model.DeltakerDbo
import java.time.LocalDate
import java.util.UUID

data class DeltakerDto(
	val id: UUID,
	val deltakerlisteId: UUID,
	val personalia: DeltakerPersonaliaDto,
	val status: DeltakerStatusDto,
	val dagerPerUke: Int?,
	val prosentStilling: Double?,
	val oppstartsdato: LocalDate?,
	val sluttdato: LocalDate?,
	val innsoktDato: LocalDate,
	val bestillingTekst: String?,
	val navKontor: String?,
	val navVeileder: DeltakerNavVeilederDto?,
	val skjult: DeltakerSkjultDto?,
	val deltarPaKurs: Boolean
)

fun DeltakerDto.toDeltakerDbo(): DeltakerDbo {
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
		status = status.type.toStatusType(deltarPaKurs),
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
		skjultAvAnsattId = skjult?.skjultAvAnsattId,
		skjultDato = skjult?.dato
	)
}

val SKJULES_ALLTID_STATUSER = listOf(
	DeltakerStatus.SOKT_INN,
	DeltakerStatus.VENTELISTE,
	DeltakerStatus.PABEGYNT_REGISTRERING,
	DeltakerStatus.FEILREGISTRERT
)

val AVSLUTTENDE_STATUSER = listOf(
	DeltakerStatus.HAR_SLUTTET,
	DeltakerStatus.IKKE_AKTUELL,
	DeltakerStatus.AVBRUTT
)
