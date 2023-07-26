package no.nav.tiltaksarrangor.repositories.model

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
	val status: StatusType,
	val statusOpprettetDato: LocalDateTime,
	val statusGyldigFraDato: LocalDateTime,
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
	val skjultDato: LocalDateTime?
) {
	fun erSkjult(): Boolean {
		return skjultDato != null
	}

	fun skalFjernesDato(): LocalDateTime? {
		return if (status in STATUSER_SOM_KAN_SKJULES) {
			statusGyldigFraDato.plusWeeks(2)
		} else {
			null
		}
	}
}

val STATUSER_SOM_KAN_SKJULES = listOf(
	StatusType.IKKE_AKTUELL,
	StatusType.HAR_SLUTTET,
	StatusType.AVBRUTT
)
