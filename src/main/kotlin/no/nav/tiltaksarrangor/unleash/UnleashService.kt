package no.nav.tiltaksarrangor.unleash

import io.getunleash.Unleash
import org.springframework.stereotype.Component

@Component
class UnleashService(
	private val unleash: Unleash,
) {
	private val tiltakstyperKometAlltidErMasterFor = listOf(
		"ARBFORB",
		"INDOPPFAG",
		"AVKLARAG",
		"ARBRRHDAG",
		"DIGIOPPARB",
		"VASV",
		"JOBBK",
		"GRUPPEAMO",
		"GRUFAGYRKE",
	)

	// her kan vi legge inn de neste tiltakstypene vi skal ta over
	private val tiltakstyperKometKanskjeErMasterFor = emptyList<String>()

	fun erKometMasterForTiltakstype(tiltakstype: String): Boolean = tiltakstype in tiltakstyperKometAlltidErMasterFor ||
		(unleash.isEnabled("amt.enable-komet-deltakere") && tiltakstype in tiltakstyperKometKanskjeErMasterFor)

	fun getFeaturetoggles(features: List<String>): Map<String, Boolean> = features.associateWith { unleash.isEnabled(it) }
}
