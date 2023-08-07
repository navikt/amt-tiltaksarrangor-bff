package no.nav.tiltaksarrangor.repositories.model

import no.nav.tiltaksarrangor.ingest.model.DeltakerlisteStatus
import java.time.LocalDate
import java.util.UUID

data class DeltakerlisteDbo(
	val id: UUID,
	val navn: String,
	val status: DeltakerlisteStatus,
	val arrangorId: UUID,
	private val _tiltakNavn: String,
	val tiltakType: String,
	val startDato: LocalDate?,
	val sluttDato: LocalDate?,
	val erKurs: Boolean
) {

	val tiltakNavn = _tiltakNavn
		get(): String {
			return when (tiltakType) {
				"ARBRRHDAG" -> "Arbeidsforberedende trening"
				"ARBFORB" -> "Arbeidsrettet rehabilitering"
				"DIGIOPPARB" -> "Digitalt oppfølgingstiltak"
				"GRUPPEAMO" -> "Arbeidsmarkedsopplæring"
				else -> field
			}
		}
}
