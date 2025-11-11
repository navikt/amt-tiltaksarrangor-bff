package no.nav.tiltaksarrangor.unleash

import io.getunleash.Unleash
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode
import org.springframework.stereotype.Component

@Component
class UnleashService(
	private val unleash: Unleash,
) {
	private val tiltakstyperKometAlltidErMasterFor = setOf(
		Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
		Tiltakskode.OPPFOLGING,
		Tiltakskode.AVKLARING,
		Tiltakskode.ARBEIDSRETTET_REHABILITERING,
		Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK,
		Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
		Tiltakskode.JOBBKLUBB,
		Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
		Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
	)

	// Enkelplasser skal ikke inn i tiltaksarrangor
	private val tiltakstyperKometKanskjeErMasterFor = emptySet<Tiltakskode>()

	fun erKometMasterForTiltakstype(tiltakstype: Tiltakskode): Boolean = tiltakstype in tiltakstyperKometAlltidErMasterFor ||
		(unleash.isEnabled("amt.enable-komet-deltakere") && tiltakstype in tiltakstyperKometKanskjeErMasterFor)

	fun getFeaturetoggles(features: List<String>): Map<String, Boolean> = features.associateWith { unleash.isEnabled(it) }
}
