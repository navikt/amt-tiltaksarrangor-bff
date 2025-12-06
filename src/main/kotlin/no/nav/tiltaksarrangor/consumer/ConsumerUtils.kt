package no.nav.tiltaksarrangor.consumer

import no.nav.tiltaksarrangor.unleash.UnleashToggle.Companion.tiltakstyperKometAlltidErMasterFor
import no.nav.tiltaksarrangor.utils.objectMapper

object ConsumerUtils {
	private const val DELTAKERLISTE_KEY = "deltakerliste"
	private const val TILTAKSTYPE_KEY = "tiltak"
	private const val TILTAKSKODE_KEY = "tiltakskode"
	private const val FALLBACK_TILTAKSKODE = "UKJENT"

	fun getTiltakskodeFromDeltakerJsonPayload(messageJson: String): String = objectMapper
		.readTree(messageJson)
		.get(DELTAKERLISTE_KEY)
		?.get(TILTAKSTYPE_KEY)
		?.get(TILTAKSKODE_KEY)
		?.asString()
		?: FALLBACK_TILTAKSKODE

	fun tiltakskodeErStottet(tiltakskode: String): Boolean = tiltakstyperKometAlltidErMasterFor.any { it.name == tiltakskode }
}
