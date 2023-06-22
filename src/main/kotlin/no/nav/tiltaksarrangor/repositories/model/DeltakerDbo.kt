package no.nav.tiltaksarrangor.repositories.model

import no.nav.tiltaksarrangor.ingest.model.DeltakerStatus
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
	val status: DeltakerStatus,
	val statusOpprettetDato: LocalDateTime,
	val statusGyldigFraDato: LocalDateTime,
	val dagerPerUke: Int?,
	val prosentStilling: Double?,
	val startdato: LocalDate?,
	val sluttdato: LocalDate?,
	val innsoktDato: LocalDate,
	val bestillingstekst: String?,
	val navKontor: String?,
	val navVeilederId: UUID?,
	val navVeilederNavn: String?,
	val navVeilederEpost: String?,
	val skjultAvAnsattId: UUID?,
	val skjultDato: LocalDateTime?
)
