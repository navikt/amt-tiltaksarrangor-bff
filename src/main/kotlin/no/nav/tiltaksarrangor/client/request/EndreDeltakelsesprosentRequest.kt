package no.nav.tiltaksarrangor.client.request

import java.time.LocalDate

data class EndreDeltakelsesprosentRequest(
	val deltakelseProsent: Int,
	val gyldigFraDato: LocalDate?
)
