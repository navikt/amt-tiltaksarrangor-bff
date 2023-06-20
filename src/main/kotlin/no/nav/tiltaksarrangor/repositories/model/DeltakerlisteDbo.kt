package no.nav.tiltaksarrangor.repositories.model

import no.nav.tiltaksarrangor.ingest.model.DeltakerlisteStatus
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
	val erKurs: Boolean
)
