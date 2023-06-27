package no.nav.tiltaksarrangor.client.amttiltak.dto

import no.nav.tiltaksarrangor.model.Veileder
import no.nav.tiltaksarrangor.model.Veiledertype
import java.util.UUID

data class VeilederDto(
	val id: UUID,
	val ansattId: UUID,
	val deltakerId: UUID,
	val erMedveileder: Boolean,
	val fornavn: String,
	val mellomnavn: String?,
	val etternavn: String
)

fun VeilederDto.toVeileder(): Veileder {
	return Veileder(
		ansattId = ansattId,
		deltakerId = deltakerId,
		veiledertype = if (erMedveileder) {
			Veiledertype.MEDVEILEDER
		} else {
			Veiledertype.VEILEDER
		},
		fornavn = fornavn,
		mellomnavn = mellomnavn,
		etternavn = etternavn
	)
}
