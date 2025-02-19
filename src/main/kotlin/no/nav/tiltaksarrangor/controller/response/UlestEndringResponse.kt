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


sealed interface OppdateringResponse

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
	JsonSubTypes.Type(value = DeltakelsesEndringResponse::class, name = "DeltakelsesEndring"),
	JsonSubTypes.Type(value = AvvistForslagResponse::class, name = "AvvistForslag"),
)
data class DeltakelsesEndringResponse(
	val endring: DeltakerEndringResponse,
) : OppdateringResponse

data class AvvistForslagResponse(
	val forslag: ForslagHistorikkResponse,
) : OppdateringResponse

fun List<UlestEndring>.toResponse(
	ansatte: Map<UUID, NavAnsatt>,
	arrangornavn: String,
	enheter: Map<UUID, NavEnhet>,
): List<UlestEndringResponse> = this.map {
	when (it.oppdatering) {
		is Oppdatering.DeltakelsesEndring -> UlestEndringResponse(
			it.id,
			it.deltakerId,
			DeltakelsesEndringResponse(
				it.oppdatering.endring.toResponse(ansatte, enheter, arrangornavn),
			),
		)
		is Oppdatering.AvvistForslag -> UlestEndringResponse(
			it.id,
			it.deltakerId,
			AvvistForslagResponse(
				it.oppdatering.forslag.toResponse(arrangornavn, ansatte, enheter),
			),
		)
	}
}
