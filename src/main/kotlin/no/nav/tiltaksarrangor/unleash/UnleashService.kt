package no.nav.tiltaksarrangor.unleash

import io.getunleash.DefaultUnleash
import org.springframework.stereotype.Component

@Component
class UnleashService(
	private val unleash: DefaultUnleash,
) {
	fun getFeaturetoggles(features: List<String>): Map<String, Boolean> {
		return features.associateWith { unleash.isEnabled(it) }
	}
}
