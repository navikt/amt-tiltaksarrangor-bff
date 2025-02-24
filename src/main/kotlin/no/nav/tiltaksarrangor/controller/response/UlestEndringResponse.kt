package no.nav.tiltaksarrangor.controller.response

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tiltaksarrangor.ingest.model.NavAnsatt
import no.nav.tiltaksarrangor.ingest.model.NavEnhet
import no.nav.tiltaksarrangor.model.Oppdatering
import no.nav.tiltaksarrangor.model.UlestEndring
import java.util.UUID

data class UlestEndringResponse(
	val id: UUID,
	val deltakerId: UUID,
	val oppdatering: OppdateringResponse,
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
	JsonSubTypes.Type(value = OppdateringResponse.DeltakelsesEndringResponse::class, name = "DeltakelsesEndring"),
	JsonSubTypes.Type(value = OppdateringResponse.AvvistForslagResponse::class, name = "AvvistForslag"),
)
sealed interface OppdateringResponse {
	data class DeltakelsesEndringResponse(
		val endring: DeltakerEndringResponse,
	) : OppdateringResponse

	data class AvvistForslagResponse(
		val forslag: ForslagHistorikkResponse,
	) : OppdateringResponse

	data class NavBrukerEndringResponse(
		val tlf: String?,
		val epost: String?,
	) : OppdateringResponse

	data class NavEndringResponse(
		val navVeilederNavn: String?,
		val navVeilederEpost: String?,
		val navVeilederTelefonnummer: String?,
		val navEnhet: String?,
	) : OppdateringResponse
}

fun List<UlestEndring>.toResponse(
	ansatte: Map<UUID, NavAnsatt>,
	arrangornavn: String,
	enheter: Map<UUID, NavEnhet>,
): List<UlestEndringResponse> = this.map {
	when (it.oppdatering) {
		is Oppdatering.DeltakelsesEndring -> UlestEndringResponse(
			it.id,
			it.deltakerId,
			OppdateringResponse.DeltakelsesEndringResponse(
				it.oppdatering.endring.toResponse(ansatte, enheter, arrangornavn),
			),
		)
		is Oppdatering.AvvistForslag -> UlestEndringResponse(
			it.id,
			it.deltakerId,
			OppdateringResponse.AvvistForslagResponse(
				it.oppdatering.forslag.toResponse(arrangornavn, ansatte, enheter),
			),
		)
		is Oppdatering.NavBrukerEndring -> UlestEndringResponse(
			it.id,
			it.deltakerId,
			OppdateringResponse.NavBrukerEndringResponse(it.oppdatering.tlf, it.oppdatering.epost),
		)
		is Oppdatering.NavEndring -> UlestEndringResponse(
			it.id,
			it.deltakerId,
			OppdateringResponse.NavEndringResponse(
				it.oppdatering.navVeilederNavn,
				it.oppdatering.navVeilederEpost,
				it.oppdatering.navVeilederTelefonnummer,
				it.oppdatering.navEnhet,
			),
		)
	}
}
