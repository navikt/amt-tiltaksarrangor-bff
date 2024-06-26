package no.nav.tiltaksarrangor.unleash

import io.getunleash.Unleash
import org.springframework.stereotype.Component

@Component
class UnleashService(
	private val unleash: Unleash,
) {
	fun getFeaturetoggles(features: List<String>): Map<String, Boolean> {
		return features.associateWith { unleash.isEnabled(it) }
	}

	fun skalLagreAdressebeskyttedeDeltakere(): Boolean {
		return unleash.isEnabled("amt.enable-adressebeskyttede-deltakere")
	}

	fun erForslagSkruddPa(): Boolean {
		return unleash.isEnabled("amt.enable-arrangor-melding")
	}
}
