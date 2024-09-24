package no.nav.tiltaksarrangor.controller.response

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.amt.lib.models.arrangor.melding.EndringFraArrangor
import no.nav.amt.lib.models.arrangor.melding.Forslag
import no.nav.amt.lib.models.deltaker.Deltakelsesinnhold
import no.nav.amt.lib.models.deltaker.DeltakerEndring
import no.nav.amt.lib.models.deltaker.DeltakerHistorikk
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import no.nav.amt.lib.models.deltaker.ImportertFraArena
import no.nav.amt.lib.models.deltaker.Vedtak
import no.nav.tiltaksarrangor.ingest.model.NavAnsatt
import no.nav.tiltaksarrangor.ingest.model.NavEnhet
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
	JsonSubTypes.Type(value = DeltakerEndringResponse::class, name = "Endring"),
	JsonSubTypes.Type(value = VedtakResponse::class, name = "Vedtak"),
	JsonSubTypes.Type(value = ForslagHistorikkResponse::class, name = "Forslag"),
	JsonSubTypes.Type(value = EndringFraArrangorResponse::class, name = "EndringFraArrangor"),
	JsonSubTypes.Type(value = ImportertFraArenaResponse::class, name = "ImportertFraArena"),
)
sealed interface DeltakerHistorikkResponse

data class DeltakerEndringResponse(
	val endring: DeltakerEndring.Endring,
	val endretAv: String,
	val endretAvEnhet: String,
	val endret: LocalDateTime,
	val forslag: ForslagHistorikkResponse?,
) : DeltakerHistorikkResponse

data class VedtakResponse(
	val fattet: LocalDateTime?,
	val bakgrunnsinformasjon: String?,
	val fattetAvNav: Boolean,
	val deltakelsesinnhold: Deltakelsesinnhold?,
	val dagerPerUke: Float?,
	val deltakelsesprosent: Float?,
	val opprettetAv: String,
	val opprettetAvEnhet: String,
	val opprettet: LocalDateTime,
) : DeltakerHistorikkResponse

data class EndringFraArrangorResponse(
	val id: UUID,
	val opprettet: LocalDateTime,
	val arrangorNavn: String,
	val endring: EndringFraArrangor.Endring,
) : DeltakerHistorikkResponse

data class ForslagHistorikkResponse(
	val id: UUID,
	val opprettet: LocalDateTime,
	val begrunnelse: String?,
	val arrangorNavn: String,
	val endring: Forslag.Endring,
	val status: ForslagHistorikkResponseStatus,
) : DeltakerHistorikkResponse

data class ImportertFraArenaResponse(
	val importertDato: LocalDateTime,
	val startdato: LocalDate?,
	val sluttdato: LocalDate?,
	val dagerPerUke: Float?,
	val deltakelsesprosent: Float?,
	val status: DeltakerStatus,
) : DeltakerHistorikkResponse

@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed interface ForslagHistorikkResponseStatus {
	data object VenterPaSvar : ForslagHistorikkResponseStatus

	data class Godkjent(
		val godkjent: LocalDateTime,
	) : ForslagHistorikkResponseStatus

	data class Avvist(
		val avvistAv: String,
		val avvistAvEnhet: String,
		val avvist: LocalDateTime,
		val begrunnelseFraNav: String,
	) : ForslagHistorikkResponseStatus

	data class Tilbakekalt(
		val tilbakekalt: LocalDateTime,
	) : ForslagHistorikkResponseStatus

	data class Erstattet(
		val erstattet: LocalDateTime,
	) : ForslagHistorikkResponseStatus
}

fun List<DeltakerHistorikk>.toResponse(
	ansatte: Map<UUID, NavAnsatt>,
	arrangornavn: String,
	enheter: Map<UUID, NavEnhet>,
): List<DeltakerHistorikkResponse> = this.map {
	when (it) {
		is DeltakerHistorikk.Endring -> it.endring.toResponse(ansatte, enheter, arrangornavn)
		is DeltakerHistorikk.Vedtak -> it.vedtak.toResponse(ansatte, enheter)
		is DeltakerHistorikk.Forslag -> it.forslag.toResponse(arrangornavn, ansatte, enheter)
		is DeltakerHistorikk.EndringFraArrangor -> it.endringFraArrangor.toResponse(arrangornavn)
		is DeltakerHistorikk.ImportertFraArena -> it.importertFraArena.toResponse()
	}
}

fun DeltakerEndring.toResponse(
	ansatte: Map<UUID, NavAnsatt>,
	enheter: Map<UUID, NavEnhet>,
	arrangornavn: String,
) = DeltakerEndringResponse(
	endring = endring,
	endretAv = ansatte[endretAv]!!.navn,
	endretAvEnhet = enheter[endretAvEnhet]!!.navn,
	endret = endret,
	forslag = forslag?.toResponse(arrangornavn),
)

fun Vedtak.toResponse(ansatte: Map<UUID, NavAnsatt>, enheter: Map<UUID, NavEnhet>) = VedtakResponse(
	fattet = fattet,
	bakgrunnsinformasjon = deltakerVedVedtak.bakgrunnsinformasjon,
	deltakelsesinnhold = deltakerVedVedtak.deltakelsesinnhold,
	dagerPerUke = deltakerVedVedtak.dagerPerUke,
	deltakelsesprosent = deltakerVedVedtak.deltakelsesprosent,
	fattetAvNav = fattetAvNav,
	opprettetAv = ansatte[opprettetAv]!!.navn,
	opprettetAvEnhet = enheter[opprettetAvEnhet]!!.navn,
	opprettet = opprettet,
)

fun EndringFraArrangor.toResponse(arrangornavn: String) = EndringFraArrangorResponse(
	id = id,
	opprettet = opprettet,
	arrangorNavn = arrangornavn,
	endring = endring,
)

fun ImportertFraArena.toResponse() = ImportertFraArenaResponse(
	importertDato = importertDato,
	startdato = deltakerVedImport.startdato,
	sluttdato = deltakerVedImport.sluttdato,
	dagerPerUke = deltakerVedImport.dagerPerUke,
	deltakelsesprosent = deltakerVedImport.deltakelsesprosent,
	status = deltakerVedImport.status,
)

fun Forslag.toResponse(arrangornavn: String) = this.toResponse(arrangornavn, emptyMap(), emptyMap())

fun Forslag.toResponse(
	arrangornavn: String,
	ansatte: Map<UUID, NavAnsatt>,
	enheter: Map<UUID, NavEnhet>,
): ForslagHistorikkResponse = ForslagHistorikkResponse(
	id = id,
	opprettet = opprettet,
	begrunnelse = begrunnelse ?: "",
	arrangorNavn = arrangornavn,
	endring = endring,
	status = getForslagResponseStatus(ansatte, enheter),
)

private fun Forslag.getForslagResponseStatus(ansatte: Map<UUID, NavAnsatt>, enheter: Map<UUID, NavEnhet>): ForslagHistorikkResponseStatus =
	when (val status = status) {
		is Forslag.Status.VenterPaSvar -> ForslagHistorikkResponseStatus.VenterPaSvar
		is Forslag.Status.Godkjent -> ForslagHistorikkResponseStatus.Godkjent(status.godkjent)
		is Forslag.Status.Avvist -> {
			val avvist = status
			ForslagHistorikkResponseStatus.Avvist(
				avvistAv = ansatte[avvist.avvistAv.id]!!.navn,
				avvistAvEnhet = enheter[avvist.avvistAv.enhetId]!!.navn,
				avvist = avvist.avvist,
				begrunnelseFraNav = avvist.begrunnelseFraNav,
			)
		}
		is Forslag.Status.Tilbakekalt -> ForslagHistorikkResponseStatus.Tilbakekalt(status.tilbakekalt)
		is Forslag.Status.Erstattet -> ForslagHistorikkResponseStatus.Erstattet(status.erstattet)
	}
