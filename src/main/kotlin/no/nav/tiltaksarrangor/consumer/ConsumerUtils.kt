package no.nav.tiltaksarrangor.consumer

import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode
import no.nav.amt.lib.utils.objectMapper

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
		?.asText()
		?: FALLBACK_TILTAKSKODE

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

	fun tiltakskodeErStottet(tiltakskode: String): Boolean = tiltakstyperKometErMasterFor.any { it.name == tiltakskode }
}
