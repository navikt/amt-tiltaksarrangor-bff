package no.nav.tiltaksarrangor.unleash

import io.getunleash.Unleash
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode
import org.springframework.stereotype.Component

@Component
class UnleashToggle(
	private val unleashClient: Unleash,
) {
	fun erKometMasterForTiltakstype(tiltakskode: String): Boolean = tiltakstyperKometErMasterFor.any { it.name == tiltakskode }

	fun erKometMasterForTiltakstype(tiltakskode: Tiltakskode): Boolean = erKometMasterForTiltakstype(tiltakskode.name)

	fun getFeaturetoggles(features: List<String>): Map<String, Boolean> = features.associateWith { unleashClient.isEnabled(it) }

	fun skalLeseGjennomforingerV2(): Boolean = unleashClient.isEnabled(LES_GJENNOMFORINGER_V2)

	companion object {
		const val ENABLE_KOMET_DELTAKERE = "amt.enable-komet-deltakere"
		const val LES_GJENNOMFORINGER_V2 = "amt.les-gjennomforing-v2"

		private val tiltakstyperKometErMasterFor = setOf(
			Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
			Tiltakskode.OPPFOLGING,
			Tiltakskode.AVKLARING,
			Tiltakskode.ARBEIDSRETTET_REHABILITERING,
			Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK,
			Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
			Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
			Tiltakskode.JOBBKLUBB,
			Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
		)
	}
}
