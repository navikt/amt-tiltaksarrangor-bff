package no.nav.tiltaksarrangor.client

import no.nav.tiltaksarrangor.client.dto.DeltakerDetaljerDto
import no.nav.tiltaksarrangor.client.dto.DeltakerDto
import no.nav.tiltaksarrangor.client.dto.DeltakeroversiktDto
import no.nav.tiltaksarrangor.client.dto.EndringsmeldingDto
import no.nav.tiltaksarrangor.client.dto.GjennomforingDto
import no.nav.tiltaksarrangor.client.dto.TilgjengeligVeilederDto
import no.nav.tiltaksarrangor.client.dto.VeilederDto
import no.nav.tiltaksarrangor.client.dto.VeiledersDeltakerDto
import no.nav.tiltaksarrangor.client.request.AvsluttDeltakelseRequest
import no.nav.tiltaksarrangor.client.request.DeltakerIkkeAktuellRequest
import no.nav.tiltaksarrangor.client.request.EndreDeltakelsesprosentRequest
import no.nav.tiltaksarrangor.client.request.EndreOppstartsdatoRequest
import no.nav.tiltaksarrangor.client.request.EndreSluttdatoRequest
import no.nav.tiltaksarrangor.client.request.ForlengDeltakelseRequest
import no.nav.tiltaksarrangor.client.request.LeggTilOppstartsdatoRequest
import no.nav.tiltaksarrangor.koordinator.model.Koordinator
import no.nav.tiltaksarrangor.koordinator.model.LeggTilVeiledereRequest
import no.nav.tiltaksarrangor.model.exceptions.UnauthorizedException
import no.nav.tiltaksarrangor.utils.JsonUtils.fromJsonString
import no.nav.tiltaksarrangor.utils.JsonUtils.objectMapper
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class AmtTiltakClient(
	@Value("\${amt-tiltak.url}") private val amtTiltakUrl: String,
	private val amtTiltakHttpClient: OkHttpClient
) {
	private val log = LoggerFactory.getLogger(javaClass)
	private val mediaTypeJson = "application/json".toMediaType()

	fun getMineRoller(): List<String> {
		val request = Request.Builder()
			.url("$amtTiltakUrl/api/tiltaksarrangor/ansatt/meg/roller")
			.get()
			.build()

		amtTiltakHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				log.error("Klarte ikke hente ansatts roller fra amt-tiltak, responsekode: ${response.code}")
				throw RuntimeException("Klarte ikke å hente roller for innlogget ansatt")
			}
			val body = response.body?.string() ?: throw RuntimeException("Tom responsbody")

			val roller = fromJsonString<List<String>>(body)

			return roller.distinct()
		}
	}

	fun getDeltaker(deltakerId: UUID): DeltakerDetaljerDto {
		val request = Request.Builder()
			.url("$amtTiltakUrl/api/tiltaksarrangor/deltaker/$deltakerId")
			.get()
			.build()

		amtTiltakHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				handleUnsuccessfulResponse(response.code, "deltaker med id $deltakerId")
			}
			val body = response.body?.string() ?: throw RuntimeException("Tom responsbody")

			return fromJsonString<DeltakerDetaljerDto>(body)
		}
	}

	fun getVeiledere(deltakerId: UUID): List<VeilederDto> {
		val request = Request.Builder()
			.url("$amtTiltakUrl/api/tiltaksarrangor/veiledere?deltakerId=$deltakerId")
			.get()
			.build()

		amtTiltakHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				handleUnsuccessfulResponse(response.code, "veileder for deltaker med id $deltakerId")
			}
			val body = response.body?.string() ?: throw RuntimeException("Tom responsbody")

			return fromJsonString<List<VeilederDto>>(body)
		}
	}

	fun getAktiveEndringsmeldinger(deltakerId: UUID): List<EndringsmeldingDto> {
		val request = Request.Builder()
			.url("$amtTiltakUrl/api/tiltaksarrangor/endringsmelding/aktiv?deltakerId=$deltakerId")
			.get()
			.build()

		amtTiltakHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				handleUnsuccessfulResponse(response.code, "endringsmeldinger for deltaker med id $deltakerId")
			}
			val body = response.body?.string() ?: throw RuntimeException("Tom responsbody")

			return fromJsonString<List<EndringsmeldingDto>>(body)
		}
	}

	fun leggTilOppstartsdato(deltakerId: UUID, leggTilOppstartsdatoRequest: LeggTilOppstartsdatoRequest) {
		val request = Request.Builder()
			.url("$amtTiltakUrl/api/tiltaksarrangor/deltaker/$deltakerId/oppstartsdato")
			.post(objectMapper.writeValueAsString(leggTilOppstartsdatoRequest).toRequestBody(mediaTypeJson))
			.build()

		amtTiltakHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				handleUnsuccessfulUpdateResponse(response.code, "legge til oppstartdato for deltaker med id $deltakerId")
			}
		}
	}

	fun endreOppstartsdato(deltakerId: UUID, endreOppstartsdatoRequest: EndreOppstartsdatoRequest) {
		val request = Request.Builder()
			.url("$amtTiltakUrl/api/tiltaksarrangor/deltaker/$deltakerId/oppstartsdato")
			.patch(objectMapper.writeValueAsString(endreOppstartsdatoRequest).toRequestBody(mediaTypeJson))
			.build()

		amtTiltakHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				handleUnsuccessfulUpdateResponse(response.code, "endre oppstartdato for deltaker med id $deltakerId")
			}
		}
	}

	fun avsluttDeltakelse(deltakerId: UUID, avsluttDeltakelseRequest: AvsluttDeltakelseRequest) {
		val request = Request.Builder()
			.url("$amtTiltakUrl/api/tiltaksarrangor/deltaker/$deltakerId/avslutt-deltakelse")
			.patch(objectMapper.writeValueAsString(avsluttDeltakelseRequest).toRequestBody(mediaTypeJson))
			.build()

		amtTiltakHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				handleUnsuccessfulUpdateResponse(response.code, "avslutte deltakelse for deltaker med id $deltakerId")
			}
		}
	}

	fun forlengDeltakelse(deltakerId: UUID, forlengDeltakelseRequest: ForlengDeltakelseRequest) {
		val request = Request.Builder()
			.url("$amtTiltakUrl/api/tiltaksarrangor/deltaker/$deltakerId/forleng-deltakelse")
			.patch(objectMapper.writeValueAsString(forlengDeltakelseRequest).toRequestBody(mediaTypeJson))
			.build()

		amtTiltakHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				handleUnsuccessfulUpdateResponse(response.code, "forlenge deltakelse for deltaker med id $deltakerId")
			}
		}
	}

	fun endreDeltakelsesprosent(deltakerId: UUID, endreDeltakelsesprosentRequest: EndreDeltakelsesprosentRequest) {
		val request = Request.Builder()
			.url("$amtTiltakUrl/api/tiltaksarrangor/deltaker/$deltakerId/deltakelse-prosent")
			.patch(objectMapper.writeValueAsString(endreDeltakelsesprosentRequest).toRequestBody(mediaTypeJson))
			.build()

		amtTiltakHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				handleUnsuccessfulUpdateResponse(response.code, "endre deltakelsesprosent for deltaker med id $deltakerId")
			}
		}
	}

	fun deltakerIkkeAktuell(deltakerId: UUID, deltakerIkkeAktuellRequest: DeltakerIkkeAktuellRequest) {
		val request = Request.Builder()
			.url("$amtTiltakUrl/api/tiltaksarrangor/deltaker/$deltakerId/ikke-aktuell")
			.patch(objectMapper.writeValueAsString(deltakerIkkeAktuellRequest).toRequestBody(mediaTypeJson))
			.build()

		amtTiltakHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				handleUnsuccessfulUpdateResponse(response.code, "sette deltaker med id $deltakerId som ikke aktuell")
			}
		}
	}

	fun tilbyPlass(deltakerId: UUID) {
		val request = Request.Builder()
			.url("$amtTiltakUrl/api/tiltaksarrangor/deltaker/$deltakerId/tilby-plass")
			.build()

		amtTiltakHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				handleUnsuccessfulUpdateResponse(response.code, "opprett TILBY_PLASS endringsmelding på deltaker med id $deltakerId ")
			}
		}
	}
	fun settPaaVenteliste(deltakerId: UUID) {
		val request = Request.Builder()
			.url("$amtTiltakUrl/api/tiltaksarrangor/deltaker/$deltakerId/sett-paa-venteliste")
			.build()

		amtTiltakHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				handleUnsuccessfulUpdateResponse(response.code, "opprett SETT_PAA_VENTELISTE endringsmelding på deltaker med id $deltakerId ")
			}
		}
	}
	fun endreSluttdato(deltakerId: UUID, endreSluttdatoRequest: EndreSluttdatoRequest) {
		val request = Request.Builder()
			.url("$amtTiltakUrl/api/tiltaksarrangor/deltaker/$deltakerId/endre-sluttdato")
			.patch(objectMapper.writeValueAsString(endreSluttdatoRequest).toRequestBody(mediaTypeJson))
			.build()

		amtTiltakHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				handleUnsuccessfulUpdateResponse(response.code, "opprett ENDRE_SLUTTDATO endringsmelding på deltaker med id $deltakerId sluttdato: ${endreSluttdatoRequest.sluttdato}")
			}
		}
	}

	fun getVeiledersDeltakere(): List<VeiledersDeltakerDto> {
		val request = Request.Builder()
			.url("$amtTiltakUrl/api/tiltaksarrangor/veileder/deltakerliste")
			.get()
			.build()

		amtTiltakHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				handleUnsuccessfulResponse(response.code, "mine deltakere")
			}
			val body = response.body?.string() ?: throw RuntimeException("Tom responsbody")

			return fromJsonString<List<VeiledersDeltakerDto>>(body)
		}
	}

	fun tilbakekallEndringsmelding(endringsmeldingId: UUID) {
		val request = Request.Builder()
			.url("$amtTiltakUrl/api/tiltaksarrangor/endringsmelding/$endringsmeldingId/tilbakekall")
			.patch(emptyRequest())
			.build()

		amtTiltakHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				handleUnsuccessfulUpdateResponse(response.code, "tilbakekalle endringsmelding med id $endringsmeldingId")
			}
		}
	}

	fun getMineDeltakerlister(): DeltakeroversiktDto {
		val request = Request.Builder()
			.url("$amtTiltakUrl/api/tiltaksarrangor/deltakeroversikt")
			.get()
			.build()

		amtTiltakHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				handleUnsuccessfulResponse(response.code, "mine deltakerlister")
			}
			val body = response.body?.string() ?: throw RuntimeException("Tom responsbody")

			return fromJsonString<DeltakeroversiktDto>(body)
		}
	}

	fun getTilgjengeligeVeiledere(deltakerlisteId: UUID): List<TilgjengeligVeilederDto> {
		val request = Request.Builder()
			.url("$amtTiltakUrl/api/tiltaksarrangor/veiledere/tilgjengelig?gjennomforingId=$deltakerlisteId")
			.get()
			.build()

		amtTiltakHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				handleUnsuccessfulResponse(response.code, "tilgjengelige veiledere for deltakerliste med id $deltakerlisteId")
			}
			val body = response.body?.string() ?: throw RuntimeException("Tom responsbody")

			return fromJsonString<List<TilgjengeligVeilederDto>>(body)
		}
	}

	fun tildelVeiledereForDeltaker(deltakerId: UUID, leggTilVeiledereRequest: LeggTilVeiledereRequest) {
		val request = Request.Builder()
			.url("$amtTiltakUrl/api/tiltaksarrangor/veiledere?deltakerId=$deltakerId")
			.patch(objectMapper.writeValueAsString(leggTilVeiledereRequest).toRequestBody(mediaTypeJson))
			.build()

		amtTiltakHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				handleUnsuccessfulUpdateResponse(response.code, "legge til veiledere for deltaker med id $deltakerId")
			}
		}
	}

	fun skjulDeltakerForTiltaksarrangor(deltakerId: UUID) {
		val request = Request.Builder()
			.url("$amtTiltakUrl/api/tiltaksarrangor/deltaker/$deltakerId/skjul")
			.patch(emptyRequest())
			.build()

		amtTiltakHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				handleUnsuccessfulUpdateResponse(response.code, "fjerne deltaker med id $deltakerId")
			}
		}
	}

	fun getKoordinatorer(deltakerlisteId: UUID): List<Koordinator> {
		val request = Request.Builder()
			.url("$amtTiltakUrl/api/tiltaksarrangor/gjennomforing/$deltakerlisteId/koordinatorer")
			.get()
			.build()

		amtTiltakHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				handleUnsuccessfulResponse(response.code, "koordinatorer for deltakerliste med id $deltakerlisteId")
			}
			val body = response.body?.string() ?: throw RuntimeException("Tom responsbody")

			return fromJsonString<List<Koordinator>>(body)
		}
	}

	fun getGjennomforing(deltakerlisteId: UUID): GjennomforingDto {
		val request = Request.Builder()
			.url("$amtTiltakUrl/api/tiltaksarrangor/gjennomforing/$deltakerlisteId")
			.get()
			.build()

		amtTiltakHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				handleUnsuccessfulResponse(response.code, "deltakerliste med id $deltakerlisteId")
			}
			val body = response.body?.string() ?: throw RuntimeException("Tom responsbody")

			return fromJsonString<GjennomforingDto>(body)
		}
	}

	fun getDeltakerePaGjennomforing(deltakerlisteId: UUID): List<DeltakerDto> {
		val request = Request.Builder()
			.url("$amtTiltakUrl/api/tiltaksarrangor/deltaker?gjennomforingId=$deltakerlisteId")
			.get()
			.build()

		amtTiltakHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				handleUnsuccessfulResponse(response.code, "deltakere for deltakerliste med id $deltakerlisteId")
			}
			val body = response.body?.string() ?: throw RuntimeException("Tom responsbody")

			return fromJsonString<List<DeltakerDto>>(body)
		}
	}

	fun getDeltakerlisterLagtTil(): List<GjennomforingDto> {
		val request = Request.Builder()
			.url("$amtTiltakUrl/api/tiltaksarrangor/gjennomforing")
			.get()
			.build()

		amtTiltakHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				handleUnsuccessfulResponse(response.code, "deltakerlister som er lagt til")
			}
			val body = response.body?.string() ?: throw RuntimeException("Tom responsbody")

			return fromJsonString<List<GjennomforingDto>>(body)
		}
	}

	fun getTilgjengeligeDeltakerlister(): List<GjennomforingDto> {
		val request = Request.Builder()
			.url("$amtTiltakUrl/api/tiltaksarrangor/gjennomforing/tilgjengelig")
			.get()
			.build()

		amtTiltakHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				handleUnsuccessfulResponse(response.code, "alle tilgjengelige deltakerlister")
			}
			val body = response.body?.string() ?: throw RuntimeException("Tom responsbody")

			return fromJsonString<List<GjennomforingDto>>(body)
		}
	}

	fun opprettTilgangTilGjennomforing(deltakerlisteId: UUID) {
		val request = Request.Builder()
			.url("$amtTiltakUrl/api/tiltaksarrangor/gjennomforing/$deltakerlisteId/tilgang")
			.post(emptyRequest())
			.build()

		amtTiltakHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				handleUnsuccessfulUpdateResponse(response.code, "legge til deltakerliste med id $deltakerlisteId")
			}
		}
	}

	fun fjernTilgangTilGjennomforing(deltakerlisteId: UUID) {
		val request = Request.Builder()
			.url("$amtTiltakUrl/api/tiltaksarrangor/gjennomforing/$deltakerlisteId/tilgang")
			.delete()
			.build()

		amtTiltakHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				handleUnsuccessfulUpdateResponse(response.code, "fjerne deltakerliste med id $deltakerlisteId")
			}
		}
	}

	private fun handleUnsuccessfulResponse(responseCode: Int, requestedResource: String) {
		when (responseCode) {
			401 -> throw UnauthorizedException("Ikke tilgang til $requestedResource")
			403 -> throw UnauthorizedException("Ikke tilgang til $requestedResource")
			404 -> throw NoSuchElementException("Fant ikke $requestedResource")
			else -> {
				log.error("Kunne ikke hente $requestedResource fra amt-tiltak, responsekode: $responseCode")
				throw RuntimeException("Kunne ikke hente $requestedResource")
			}
		}
	}

	private fun handleUnsuccessfulUpdateResponse(responseCode: Int, requestedResource: String) {
		when (responseCode) {
			401 -> throw UnauthorizedException("Ikke tilgang til å $requestedResource")
			403 -> throw UnauthorizedException("Ikke tilgang til å $requestedResource")
			404 -> throw NoSuchElementException("Fant ikke ressurs")
			else -> {
				log.error("Kunne ikke $requestedResource i amt-tiltak, responsekode: $responseCode")
				throw RuntimeException("Kunne ikke $requestedResource")
			}
		}
	}

	private fun emptyRequest(): RequestBody {
		val mediaTypeHtml = "text/html".toMediaType()
		return "".toRequestBody(mediaTypeHtml)
	}
}
