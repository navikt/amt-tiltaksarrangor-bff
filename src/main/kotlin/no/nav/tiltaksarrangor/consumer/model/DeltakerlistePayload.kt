package no.nav.tiltaksarrangor.consumer.model

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode
import no.nav.tiltaksarrangor.model.DeltakerlisteStatus
import no.nav.tiltaksarrangor.repositories.model.DeltakerlisteDbo
import java.time.LocalDate
import java.util.UUID

data class DeltakerlistePayload(
	val type: String? = null, // finnes kun for v2, kan fjernes etter overgang til v2
	val id: UUID,
	val tiltakskode: String? = null, // skal gjøres non-nullable
	val tiltakstype: Tiltakstype? = null, // skal fjernes
	val navn: String,
	val startDato: LocalDate,
	val sluttDato: LocalDate? = null,
	val status: Status,
	val oppstart: Oppstartstype,
	val tilgjengeligForArrangorFraOgMedDato: LocalDate?,
	val arrangor: Arrangor,
) {
	data class Tiltakstype(
		val tiltakskode: String,
	)

	data class Arrangor(
		val organisasjonsnummer: String,
	)

	enum class Status {
		GJENNOMFORES,
		AVBRUTT,
		AVLYST,
		AVSLUTTET,
	}

	// erstattes av tiltakskode senere
	@get:JsonIgnore
	val effectiveTiltakskode: String
		get() = tiltakskode ?: tiltakstype?.tiltakskode ?: throw IllegalStateException("Tiltakskode er ikke satt")

	fun erKurs(): Boolean = oppstart == Oppstartstype.FELLES

	fun toDeltakerlisteStatus(): DeltakerlisteStatus = when (status) {
		Status.GJENNOMFORES -> DeltakerlisteStatus.GJENNOMFORES
		Status.AVSLUTTET -> DeltakerlisteStatus.AVSLUTTET
		else -> throw IllegalStateException("Ukjent status: $status")
	}

	fun toDeltakerlisteDbo(arrangorId: UUID, navnTiltakstype: String): DeltakerlisteDbo = DeltakerlisteDbo(
		id = this.id,
		navn = this.navn,
		status = this.toDeltakerlisteStatus(),
		arrangorId = arrangorId,
		tiltaksnavn = mapTiltakstypeNavn(navnTiltakstype),
		tiltakskode = Tiltakskode.valueOf(this.effectiveTiltakskode),
		startDato = this.startDato,
		sluttDato = this.sluttDato,
		erKurs = this.erKurs(),
		oppstartstype = this.oppstart,
		tilgjengeligForArrangorFraOgMedDato = this.tilgjengeligForArrangorFraOgMedDato,
	)

	fun skalLagres(): Boolean = when (status) {
		Status.GJENNOMFORES -> true

		Status.AVSLUTTET if sluttDato != null &&
			LocalDate
				.now()
				.isBefore(sluttDato.plusDays(15))
		-> true

		else -> false
	}

	companion object {
		const val ENKELTPLASS_V2_TYPE = "TiltaksgjennomforingV2.Enkeltplass"
		const val GRUPPE_V2_TYPE = "TiltaksgjennomforingV2.Gruppe"

		private fun mapTiltakstypeNavn(tiltakstypeNavn: String): String = if (tiltakstypeNavn == "Jobbklubb") {
			"Jobbsøkerkurs"
		} else {
			tiltakstypeNavn
		}
	}
}
