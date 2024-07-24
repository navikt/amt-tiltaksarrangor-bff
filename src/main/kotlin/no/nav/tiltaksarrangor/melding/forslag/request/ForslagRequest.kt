package no.nav.tiltaksarrangor.melding.forslag.request

import no.nav.amt.lib.models.arrangor.melding.EndringAarsak
import java.time.LocalDate

sealed interface ForslagRequest {
	val begrunnelse: String?
}

data class ForlengDeltakelseRequest(
	val sluttdato: LocalDate,
	override val begrunnelse: String,
) : ForslagRequest

data class AvsluttDeltakelseRequest(
	val sluttdato: LocalDate,
	val aarsak: EndringAarsak,
	override val begrunnelse: String?,
) : ForslagRequest

data class IkkeAktuellRequest(
	val aarsak: EndringAarsak,
	override val begrunnelse: String?,
) : ForslagRequest

data class DeltakelsesmengdeRequest(
	val deltakelsesprosent: Int,
	val dagerPerUke: Int?,
	override val begrunnelse: String,
) : ForslagRequest

data class SluttdatoRequest(
	val sluttdato: LocalDate,
	override val begrunnelse: String,
) : ForslagRequest
