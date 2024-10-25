package no.nav.tiltaksarrangor.veileder.model

import no.nav.tiltaksarrangor.melding.forslag.AktivtForslagResponse
import no.nav.tiltaksarrangor.model.DeltakerStatus
import no.nav.tiltaksarrangor.model.Endringsmelding
import no.nav.tiltaksarrangor.model.Veiledertype
import java.time.LocalDate
import java.util.UUID

data class Deltaker(
	val id: UUID,
	val fornavn: String,
	val mellomnavn: String?,
	val etternavn: String,
	val fodselsnummer: String,
	val startDato: LocalDate?,
	val sluttDato: LocalDate?,
	val status: DeltakerStatus,
	val deltakerliste: Deltakerliste,
	val veiledertype: Veiledertype,
	val aktiveEndringsmeldinger: List<Endringsmelding>,
	val aktiveForslag: List<AktivtForslagResponse>,
	val adressebeskyttet: Boolean,
) {
	data class Deltakerliste(
		val id: UUID,
		val type: String,
		val navn: String,
	)
}
