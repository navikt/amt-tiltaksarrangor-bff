package no.nav.tiltaksarrangor.unleash

import io.getunleash.DefaultUnleash
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class UnleashService(
	private val unleash: DefaultUnleash
) {
	fun skalViseKurs(deltakerlisteId: UUID): Boolean {
		return if (erPilot(deltakerlisteId)) {
			true
		} else {
			unleash.isEnabled("amt.eksponer-kurs")
		}
	}

	private fun erPilot(deltakerlisteId: UUID): Boolean {
		return deltakerlisteId.toString() in listOf("69afc1b8-50b9-472a-8b92-254dec821c3a", "e41ef5c5-2c2e-41f6-97a2-36fca4902b86", "67b63927-3c6f-494b-ad9b-5fff08b8d196")
	}
}
