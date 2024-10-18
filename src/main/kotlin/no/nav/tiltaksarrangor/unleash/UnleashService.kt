package no.nav.tiltaksarrangor.unleash

import io.getunleash.Unleash
import org.springframework.stereotype.Component

@Component
class UnleashService(
	private val unleash: Unleash,
) {
	private val tiltakstyperKometAlltidErMasterFor = listOf(
		"ARBFORB",
	)

	// her kan vi legge inn de neste tiltakstypene vi skal ta over
	private val tiltakstyperKometKanskjeErMasterFor = emptyList<String>()

	fun erKometMasterForTiltakstype(tiltakstype: String): Boolean {
		return tiltakstype in tiltakstyperKometAlltidErMasterFor ||
			(unleash.isEnabled("amt.enable-komet-deltakere") && tiltakstype in tiltakstyperKometKanskjeErMasterFor)
	}

	fun getFeaturetoggles(features: List<String>): Map<String, Boolean> {
		return features.associateWith { unleash.isEnabled(it) }
	}
}
