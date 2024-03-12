package no.nav.tiltaksarrangor.repositories.model

import no.nav.tiltaksarrangor.model.DeltakerlisteStatus
import java.time.LocalDate
import java.util.UUID

data class DeltakerlisteDbo(
	val id: UUID,
	val navn: String,
	val status: DeltakerlisteStatus,
	val arrangorId: UUID,
	val tiltakNavn: String,
	val tiltakType: String,
	val startDato: LocalDate?,
	val sluttDato: LocalDate?,
	val erKurs: Boolean,
) {
	fun cleanTiltaksnavn() = when (tiltakNavn) {
		"Arbeidsforberedende trening (AFT)" -> "Arbeidsforberedende trening"
		"Arbeidsrettet rehabilitering (dag)" -> "Arbeidsrettet rehabilitering"
		"Digitalt oppfølgingstiltak for arbeidsledige (jobbklubb)" -> "Digitalt oppfølgingstiltak"
		"Gruppe AMO" -> "Arbeidsmarkedsopplæring"
		else -> tiltakNavn
	}

	fun skalViseAdresseForDeltaker(): Boolean {
		return tiltakstyperMedAdresse.contains(tiltakType)
	}

	private val tiltakstyperMedAdresse =
		setOf(
			"INDOPPFAG",
			"ARBFORB",
			"AVKLARAG",
			"VASV",
			"ARBRRHDAG",
		)
}
