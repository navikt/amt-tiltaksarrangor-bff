package no.nav.tiltaksarrangor.koordinator.model

import no.nav.amt.lib.models.deltakerliste.tiltakstype.ArenaKode
import no.nav.tiltaksarrangor.model.DeltakerlisteStatus
import java.time.LocalDate
import java.util.UUID

data class Deltakerliste(
	val id: UUID,
	val navn: String,
	val tiltaksnavn: String,
	val arrangorNavn: String,
	val startDato: LocalDate?,
	val sluttDato: LocalDate?,
	val status: DeltakerlisteStatus,
	val koordinatorer: List<Koordinator>,
	val deltakere: List<Deltaker>,
	val erKurs: Boolean,
	val tiltakType: ArenaKode,
)
