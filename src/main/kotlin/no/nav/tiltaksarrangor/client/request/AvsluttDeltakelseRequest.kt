package no.nav.tiltaksarrangor.client.request

import no.nav.tiltaksarrangor.model.DeltakerStatusAarsak
import java.time.LocalDate

data class AvsluttDeltakelseRequest(
	val sluttdato: LocalDate,
	val aarsak: DeltakerStatusAarsak
)
