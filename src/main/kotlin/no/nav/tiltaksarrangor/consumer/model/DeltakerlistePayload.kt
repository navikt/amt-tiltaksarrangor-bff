package no.nav.tiltaksarrangor.consumer.model

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.amt.lib.models.deltakerliste.tiltakstype.ArenaKode
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
	val arrangor: ArrangorDto? = null, // finnes kun for v2
) {
	data class Tiltakstype(
		val id: UUID,
		val navn: String,
		// i v1: arenaKode
		// i v2: arenakode
		@field:JsonProperty("arenaKode")
		@field:JsonAlias("arenakode")
		val arenaKode: String, // String tar høyde for andre tiltakstyper enn det vi støtter
		val tiltakskode: String,
	) {
		fun erStottet() = this.tiltakskode in Tiltakskode.entries
			.filterNot { it.erEnkeltplass() }
			.toTypedArray()
			.map { it.name }
	}

	data class ArrangorDto(
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

	fun toDeltakerlisteDbo(arrangorId: UUID): DeltakerlisteDbo = DeltakerlisteDbo(
		id = this.id,
		navn = this.navn,
		status = this.toDeltakerlisteStatus(),
		arrangorId = arrangorId,
		tiltakNavn = getTiltakstypeNavn(this.tiltakstype),
		tiltakType = ArenaKode.valueOf(this.tiltakstype.arenaKode),
		startDato = this.startDato,
		sluttDato = this.sluttDato,
		erKurs = this.erKurs(),
		oppstartstype = this.oppstart,
		tilgjengeligForArrangorFraOgMedDato = this.tilgjengeligForArrangorFraOgMedDato,
	)

	fun skalLagres(): Boolean {
		if (!tiltakstype.erStottet()) return false

		return when (status) {
			Status.GJENNOMFORES -> true

			Status.AVSLUTTET if sluttDato != null &&
				LocalDate
					.now()
					.isBefore(sluttDato.plusDays(15))
			-> true

			else -> false
		}
	}

	val organisasjonsnummer: String
		get() = when (
			type in setOf(
				ENKELTPLASS_V2_TYPE,
				GRUPPE_V2_TYPE,
			)
		) {
			true -> arrangor?.organisasjonsnummer
			false -> virksomhetsnummer
		} ?: throw IllegalStateException("Virksomhetsnummer mangler")

	companion object {
		const val ENKELTPLASS_V2_TYPE = "TiltaksgjennomforingV2.Enkeltplass"
		const val GRUPPE_V2_TYPE = "TiltaksgjennomforingV2.Gruppe"

		private fun getTiltakstypeNavn(tiltakstype: Tiltakstype): String = if (tiltakstype.navn == "Jobbklubb") {
			"Jobbsøkerkurs"
		} else {
			tiltakstype.navn
		}
	}
}
