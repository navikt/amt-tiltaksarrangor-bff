package no.nav.tiltaksarrangor.melding.forslag.request

import java.time.LocalDate

sealed interface ForslagRequest {
	val begrunnelse: String
}

data class ForlengDeltakelseRequest(
	val sluttdato: LocalDate,
	override val begrunnelse: String,
) : ForslagRequest
