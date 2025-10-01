package no.nav.tiltaksarrangor.unleash

import io.getunleash.Unleash
import no.nav.amt.lib.models.deltakerliste.tiltakstype.ArenaKode
import org.springframework.stereotype.Component

@Component
class UnleashService(
	private val unleash: Unleash,
) {
	private val tiltakstyperKometAlltidErMasterFor = setOf(
		ArenaKode.ARBFORB,
		ArenaKode.INDOPPFAG,
		ArenaKode.AVKLARAG,
		ArenaKode.ARBRRHDAG,
		ArenaKode.DIGIOPPARB,
		ArenaKode.VASV,
		ArenaKode.JOBBK,
		ArenaKode.GRUPPEAMO,
		ArenaKode.GRUFAGYRKE,
	)

	// her kan vi legge inn de neste tiltakstypene vi skal ta over
	private val tiltakstyperKometKanskjeErMasterFor = emptyList<ArenaKode>()

	fun erKometMasterForTiltakstype(tiltakstype: ArenaKode): Boolean = tiltakstype in tiltakstyperKometAlltidErMasterFor ||
		(unleash.isEnabled("amt.enable-komet-deltakere") && tiltakstype in tiltakstyperKometKanskjeErMasterFor)

	fun getFeaturetoggles(features: List<String>): Map<String, Boolean> = features.associateWith { unleash.isEnabled(it) }
}
