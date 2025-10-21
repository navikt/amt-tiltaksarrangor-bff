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
	val tiltakstype: Tiltakstype,
	val navn: String,
	val startDato: LocalDate,
	val sluttDato: LocalDate? = null,
	val status: Status,
	val oppstart: Oppstartstype,
	val tilgjengeligForArrangorFraOgMedDato: LocalDate?,
	val virksomhetsnummer: String? = null, // finnes kun for v1
	val arrangor: Arrangor? = null, // finnes kun for v2
) {
	@get:JsonIgnore
	val organisasjonsnummer: String
		get() = setOfNotNull(arrangor?.organisasjonsnummer, virksomhetsnummer)
			.firstOrNull()
			?: throw IllegalStateException("Virksomhetsnummer mangler")

	data class Tiltakstype(
		val id: UUID,
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
		tiltakNavn = mapTiltakstypeNavn(navnTiltakstype),
		tiltakType = Tiltakskode.valueOf(this.tiltakstype.tiltakskode).toArenaKode(),
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
