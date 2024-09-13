package no.nav.tiltaksarrangor.unleash

import io.getunleash.Unleash
import org.springframework.stereotype.Component

@Component
class UnleashService(
	private val unleash: Unleash,
) {
	private val forslagstiltak = listOf(
		"ARBFORB",
	)

	fun getFeaturetoggles(features: List<String>): Map<String, Boolean> {
		return features.associateWith { unleash.isEnabled(it) }
	}

	fun erForslagSkruddPa(tiltakstype: String): Boolean {
		return unleash.isEnabled("amt.enable-komet-deltakere") && tiltakstype in forslagstiltak
	}
}
