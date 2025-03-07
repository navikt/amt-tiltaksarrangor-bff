package no.nav.tiltaksarrangor.api.response

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tiltaksarrangor.consumer.model.NavAnsatt
import no.nav.tiltaksarrangor.consumer.model.NavEnhet
import no.nav.tiltaksarrangor.model.Oppdatering
import no.nav.tiltaksarrangor.model.UlestEndring
import java.time.LocalDate
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
	JsonSubTypes.Type(value = OppdateringResponse.NavBrukerEndringResponse::class, name = "NavBrukerEndring"),
	JsonSubTypes.Type(value = OppdateringResponse.NavEndringResponse::class, name = "NavEndring"),
	JsonSubTypes.Type(value = OppdateringResponse.NyDeltakerResponse::class, name = "NyDeltaker"),
)
sealed interface OppdateringResponse {
	data class DeltakelsesEndringResponse(
		val endring: DeltakerEndringResponse,
	) : OppdateringResponse

	data class AvvistForslagResponse(
		val forslag: ForslagHistorikkResponse,
	) : OppdateringResponse

	data class NavBrukerEndringResponse(
		val telefonnummer: String?,
		val epost: String?,
		val oppdatert: LocalDate,
	) : OppdateringResponse

	data class NavEndringResponse(
		val nyNavVeileder: Boolean,
		val navVeilederNavn: String?,
		val navVeilederEpost: String?,
		val navVeilederTelefonnummer: String?,
		val navEnhet: String?,
		val oppdatert: LocalDate,
	) : OppdateringResponse

	data class NyDeltakerResponse(
		val opprettetAvNavn: String?,
		val opprettetAvEnhet: String?,
		val opprettet: LocalDate,
	) : OppdateringResponse
}

fun List<UlestEndring>.toResponse(
	ansatte: Map<UUID, NavAnsatt>,
	arrangornavn: String,
	enheter: Map<UUID, NavEnhet>,
): List<UlestEndringResponse> = this.map {
	when (it.oppdatering) {
		is Oppdatering.DeltakelsesEndring -> UlestEndringResponse(
			id = it.id,
			deltakerId = it.deltakerId,
			oppdatering = OppdateringResponse.DeltakelsesEndringResponse(
				endring = it.oppdatering.endring.toResponse(ansatte, enheter, arrangornavn),
			),
		)
		is Oppdatering.AvvistForslag -> UlestEndringResponse(
			id = it.id,
			deltakerId = it.deltakerId,
			oppdatering = OppdateringResponse.AvvistForslagResponse(
				forslag = it.oppdatering.forslag.toResponse(arrangornavn, ansatte, enheter),
			),
		)
		is Oppdatering.NavBrukerEndring -> UlestEndringResponse(
			id = it.id,
			deltakerId = it.deltakerId,
			oppdatering = OppdateringResponse.NavBrukerEndringResponse(
				telefonnummer = it.oppdatering.telefonnummer,
				epost = it.oppdatering.epost,
				oppdatert = it.oppdatert,
			),
		)
		is Oppdatering.NavEndring -> UlestEndringResponse(
			id = it.id,
			deltakerId = it.deltakerId,
			oppdatering = OppdateringResponse.NavEndringResponse(
				nyNavVeileder = it.oppdatering.nyNavVeileder,
				navVeilederNavn = it.oppdatering.navVeilederNavn,
				navVeilederEpost = it.oppdatering.navVeilederEpost,
				navVeilederTelefonnummer = it.oppdatering.navVeilederTelefonnummer,
				navEnhet = it.oppdatering.navEnhet,
				oppdatert = it.oppdatert,
			),
		)
		is Oppdatering.NyDeltaker -> UlestEndringResponse(
			id = it.id,
			deltakerId = it.deltakerId,
			oppdatering = OppdateringResponse.NyDeltakerResponse(
				opprettetAvNavn = it.oppdatering.opprettetAvNavn,
				opprettetAvEnhet = it.oppdatering.opprettetAvEnhet,
				opprettet = it.oppdatering.opprettet,
			),
		)
	}
}
