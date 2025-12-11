package no.nav.tiltaksarrangor.consumer

import no.nav.amt.lib.models.deltakerliste.GjennomforingStatusType
import no.nav.amt.lib.models.deltakerliste.Oppstartstype
import no.nav.amt.lib.models.deltakerliste.kafka.GjennomforingV2KafkaPayload
import no.nav.amt.lib.utils.objectMapper
import no.nav.tiltaksarrangor.repositories.model.DeltakerlisteDbo
import no.nav.tiltaksarrangor.unleash.UnleashToggle.Companion.tiltakstyperKometAlltidErMasterFor
import java.time.LocalDate
import java.util.UUID

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

	fun tiltakskodeErStottet(tiltakskode: String): Boolean = tiltakstyperKometAlltidErMasterFor.any { it.name == tiltakskode }

	private fun mapTiltakstypeNavn(tiltakstypeNavn: String): String = if (tiltakstypeNavn == "Jobbklubb") {
		"Jobbsøkerkurs"
	} else {
		tiltakstypeNavn
	}

	fun GjennomforingV2KafkaPayload.Gruppe.toDeltakerlisteDbo(arrangorId: UUID, navnTiltakstype: String): DeltakerlisteDbo = DeltakerlisteDbo(
		id = id,
		navn = navn,
		status = status,
		arrangorId = arrangorId,
		tiltaksnavn = mapTiltakstypeNavn(navnTiltakstype),
		tiltakskode = tiltakskode,
		startDato = startDato,
		sluttDato = sluttDato,
		erKurs = oppstart == Oppstartstype.FELLES,
		oppstartstype = this.oppstart,
		tilgjengeligForArrangorFraOgMedDato = this.tilgjengeligForArrangorFraOgMedDato,
	)

	fun GjennomforingV2KafkaPayload.Gruppe.skalLagres(): Boolean = when (this.status) {
		GjennomforingStatusType.GJENNOMFORES -> true

		GjennomforingStatusType.AVSLUTTET ->
			sluttDato
				?.let { LocalDate.now().isBefore(it.plusDays(15)) }
				?: false

		else -> false
	}
}
