package no.nav.tiltaksarrangor.repositories.model

import no.nav.amt.lib.models.deltakerliste.tiltakstype.ArenaKode
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
	val tiltakType: ArenaKode,
	val startDato: LocalDate?,
	val sluttDato: LocalDate?,
	val erKurs: Boolean,
	val oppstartstype: Oppstartstype,
	val tilgjengeligForArrangorFraOgMedDato: LocalDate?,
) {
	fun erTilgjengeligForArrangor(): Boolean {
		if (tiltakType.toTiltaksKode().erEnkeltplass()) {
			return false
		} else if (startDato != null) {
			if (tilgjengeligForArrangorFraOgMedDato != null) {
				return !tilgjengeligForArrangorFraOgMedDato.isAfter(LocalDate.now())
			} else {
				return !startDato.isAfter(LocalDate.now().plusDays(14))
			}
		}
		return false
	}

	fun skalViseAdresseForDeltaker(): Boolean = tiltakstyperMedAdresse.contains(tiltakType)

	private val tiltakstyperMedAdresse: Set<ArenaKode> =
		setOf(
			ArenaKode.INDOPPFAG,
			ArenaKode.ARBFORB,
			ArenaKode.AVKLARAG,
			ArenaKode.VASV,
			ArenaKode.ARBRRHDAG,
		)
}
