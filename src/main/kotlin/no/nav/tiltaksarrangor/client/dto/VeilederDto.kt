package no.nav.tiltaksarrangor.client.dto

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
