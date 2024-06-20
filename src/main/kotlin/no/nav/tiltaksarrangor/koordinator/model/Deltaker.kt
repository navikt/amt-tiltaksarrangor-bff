package no.nav.tiltaksarrangor.koordinator.model

import no.nav.tiltaksarrangor.model.DeltakerStatus
import no.nav.tiltaksarrangor.model.Endringsmelding
import no.nav.tiltaksarrangor.model.Veileder
import no.nav.tiltaksarrangor.model.Vurdering
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Deltaker(
	val id: UUID,
	val fornavn: String,
	val mellomnavn: String?,
	val etternavn: String,
	val fodselsnummer: String,
	val soktInnDato: LocalDateTime,
	val startDato: LocalDate?,
	val sluttDato: LocalDate?,
	val status: DeltakerStatus,
	val veiledere: List<Veileder>,
	val navKontor: String?,
	val aktiveEndringsmeldinger: List<Endringsmelding>,
	val gjeldendeVurderingFraArrangor: Vurdering?,
	val adressebeskyttet: Boolean,
	val erVeilederForDeltaker: Boolean,
)
