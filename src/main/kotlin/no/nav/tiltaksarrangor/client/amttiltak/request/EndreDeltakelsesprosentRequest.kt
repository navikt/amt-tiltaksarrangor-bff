package no.nav.tiltaksarrangor.client.amttiltak.request

import java.time.LocalDate

data class EndreDeltakelsesprosentRequest(
	val deltakelseProsent: Int,
	val dagerPerUke: Int?,
	val gyldigFraDato: LocalDate?
)
