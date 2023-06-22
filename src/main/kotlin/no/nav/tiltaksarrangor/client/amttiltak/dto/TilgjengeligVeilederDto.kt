package no.nav.tiltaksarrangor.client.amttiltak.dto

import java.util.UUID

data class TilgjengeligVeilederDto(
	val ansattId: UUID,
	val fornavn: String,
	val mellomnavn: String?,
	val etternavn: String
)
