package no.nav.tiltaksarrangor.veileder.model

import no.nav.tiltaksarrangor.model.DeltakerStatus
import no.nav.tiltaksarrangor.model.Endringsmelding
import java.time.LocalDate
import java.util.UUID

data class Deltaker(
	val id: UUID,
	val deltakerliste: Deltakerliste,
	val fornavn: String,
	val mellomnavn: String?,
	val etternavn: String,
	val fodselsnummer: String,
	val status: DeltakerStatus,
	val startDato: LocalDate?,
	val sluttDato: LocalDate?,
	val veiledertype: Veiledertype,
	val aktiveEndringsmeldinger: List<Endringsmelding>
) {
	data class Deltakerliste(
		val id: UUID,
		val navn: String
	)
}
