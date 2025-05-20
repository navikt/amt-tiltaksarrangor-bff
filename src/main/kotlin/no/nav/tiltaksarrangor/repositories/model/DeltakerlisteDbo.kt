package no.nav.tiltaksarrangor.repositories.model

import no.nav.tiltaksarrangor.consumer.model.Oppstartstype
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
	val oppstartstype: Oppstartstype?, // skal være midlertidig optional til relast
	val tilgjengeligForArrangorFraOgMedDato: LocalDate?,
) {
	fun erTilgjengeligForArrangor(): Boolean = if (startDato != null) {
		if (tilgjengeligForArrangorFraOgMedDato != null) {
			!tilgjengeligForArrangorFraOgMedDato.isAfter(LocalDate.now())
		} else {
			!startDato.isAfter(LocalDate.now().plusDays(14))
		}
	} else {
		false
	}

	fun skalViseAdresseForDeltaker(): Boolean = tiltakstyperMedAdresse.contains(tiltakType)

	private val tiltakstyperMedAdresse =
		setOf(
			"INDOPPFAG",
			"ARBFORB",
			"AVKLARAG",
			"VASV",
			"ARBRRHDAG",
		)
}
