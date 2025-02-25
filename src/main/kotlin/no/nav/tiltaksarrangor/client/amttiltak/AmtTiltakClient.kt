package no.nav.tiltaksarrangor.client.amttiltak

import no.nav.amt.lib.models.arrangor.melding.Vurdering
import no.nav.tiltaksarrangor.client.amttiltak.request.AvsluttDeltakelseRequest
import no.nav.tiltaksarrangor.client.amttiltak.request.DeltakerIkkeAktuellRequest
import no.nav.tiltaksarrangor.client.amttiltak.request.EndreDeltakelsesprosentRequest
import no.nav.tiltaksarrangor.client.amttiltak.request.EndreOppstartsdatoRequest
import no.nav.tiltaksarrangor.client.amttiltak.request.EndreSluttaarsakRequest
import no.nav.tiltaksarrangor.client.amttiltak.request.EndreSluttdatoRequest
import no.nav.tiltaksarrangor.client.amttiltak.request.ForlengDeltakelseRequest
import no.nav.tiltaksarrangor.client.amttiltak.request.LeggTilOppstartsdatoRequest
import no.nav.tiltaksarrangor.client.amttiltak.request.RegistrerVurderingRequest
import no.nav.tiltaksarrangor.client.amttiltak.response.OpprettEndringsmeldingResponse
import no.nav.tiltaksarrangor.model.exceptions.UnauthorizedException
import no.nav.tiltaksarrangor.utils.JsonUtils
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
	private val amtTiltakHttpClient: OkHttpClient,
) {
	private val log = LoggerFactory.getLogger(javaClass)
	private val mediaTypeJson = "application/json".toMediaType()

	fun leggTilOppstartsdato(deltakerId: UUID, leggTilOppstartsdatoRequest: LeggTilOppstartsdatoRequest): UUID {
		val request =
			Request
				.Builder()
				.url("$amtTiltakUrl/api/tiltaksarrangor/deltaker/$deltakerId/oppstartsdato")
				.post(objectMapper.writeValueAsString(leggTilOppstartsdatoRequest).toRequestBody(mediaTypeJson))
				.build()

		amtTiltakHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				handleUnsuccessfulUpdateResponse(response.code, "legge til oppstartdato for deltaker med id $deltakerId")
			}
			val body = response.body?.string() ?: throw RuntimeException("Tom responsbody")

			return JsonUtils.fromJsonString<OpprettEndringsmeldingResponse>(body).id
		}
	}

	fun endreOppstartsdato(deltakerId: UUID, endreOppstartsdatoRequest: EndreOppstartsdatoRequest): UUID {
		val request =
			Request
				.Builder()
				.url("$amtTiltakUrl/api/tiltaksarrangor/deltaker/$deltakerId/oppstartsdato")
				.patch(objectMapper.writeValueAsString(endreOppstartsdatoRequest).toRequestBody(mediaTypeJson))
				.build()

		amtTiltakHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				handleUnsuccessfulUpdateResponse(response.code, "endre oppstartdato for deltaker med id $deltakerId")
			}
			val body = response.body?.string() ?: throw RuntimeException("Tom responsbody")

			return JsonUtils.fromJsonString<OpprettEndringsmeldingResponse>(body).id
		}
	}

	fun avsluttDeltakelse(deltakerId: UUID, avsluttDeltakelseRequest: AvsluttDeltakelseRequest): UUID {
		val request =
			Request
				.Builder()
				.url("$amtTiltakUrl/api/tiltaksarrangor/deltaker/$deltakerId/avslutt-deltakelse")
				.patch(objectMapper.writeValueAsString(avsluttDeltakelseRequest).toRequestBody(mediaTypeJson))
				.build()

		amtTiltakHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				handleUnsuccessfulUpdateResponse(response.code, "avslutte deltakelse for deltaker med id $deltakerId")
			}
			val body = response.body?.string() ?: throw RuntimeException("Tom responsbody")

			return JsonUtils.fromJsonString<OpprettEndringsmeldingResponse>(body).id
		}
	}

	fun forlengDeltakelse(deltakerId: UUID, forlengDeltakelseRequest: ForlengDeltakelseRequest): UUID {
		val request =
			Request
				.Builder()
				.url("$amtTiltakUrl/api/tiltaksarrangor/deltaker/$deltakerId/forleng-deltakelse")
				.patch(objectMapper.writeValueAsString(forlengDeltakelseRequest).toRequestBody(mediaTypeJson))
				.build()

		amtTiltakHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				handleUnsuccessfulUpdateResponse(response.code, "forlenge deltakelse for deltaker med id $deltakerId")
			}
			val body = response.body?.string() ?: throw RuntimeException("Tom responsbody")

			return JsonUtils.fromJsonString<OpprettEndringsmeldingResponse>(body).id
		}
	}

	fun endreDeltakelsesprosent(deltakerId: UUID, endreDeltakelsesprosentRequest: EndreDeltakelsesprosentRequest): UUID {
		val request =
			Request
				.Builder()
				.url("$amtTiltakUrl/api/tiltaksarrangor/deltaker/$deltakerId/deltakelse-prosent")
				.patch(objectMapper.writeValueAsString(endreDeltakelsesprosentRequest).toRequestBody(mediaTypeJson))
				.build()

		amtTiltakHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				handleUnsuccessfulUpdateResponse(response.code, "endre deltakelsesprosent for deltaker med id $deltakerId")
			}
			val body = response.body?.string() ?: throw RuntimeException("Tom responsbody")

			return JsonUtils.fromJsonString<OpprettEndringsmeldingResponse>(body).id
		}
	}

	fun deltakerIkkeAktuell(deltakerId: UUID, deltakerIkkeAktuellRequest: DeltakerIkkeAktuellRequest): UUID {
		val request =
			Request
				.Builder()
				.url("$amtTiltakUrl/api/tiltaksarrangor/deltaker/$deltakerId/ikke-aktuell")
				.patch(objectMapper.writeValueAsString(deltakerIkkeAktuellRequest).toRequestBody(mediaTypeJson))
				.build()

		amtTiltakHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				handleUnsuccessfulUpdateResponse(response.code, "sette deltaker med id $deltakerId som ikke aktuell")
			}
			val body = response.body?.string() ?: throw RuntimeException("Tom responsbody")

			return JsonUtils.fromJsonString<OpprettEndringsmeldingResponse>(body).id
		}
	}

	fun endreSluttdato(deltakerId: UUID, endreSluttdatoRequest: EndreSluttdatoRequest): UUID {
		val request =
			Request
				.Builder()
				.url("$amtTiltakUrl/api/tiltaksarrangor/deltaker/$deltakerId/endre-sluttdato")
				.patch(objectMapper.writeValueAsString(endreSluttdatoRequest).toRequestBody(mediaTypeJson))
				.build()

		amtTiltakHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				handleUnsuccessfulUpdateResponse(
					response.code,
					"opprett ENDRE_SLUTTDATO endringsmelding på deltaker med id $deltakerId sluttdato: ${endreSluttdatoRequest.sluttdato}",
				)
			}
			val body = response.body?.string() ?: throw RuntimeException("Tom responsbody")

			return JsonUtils.fromJsonString<OpprettEndringsmeldingResponse>(body).id
		}
	}

	fun endreSluttaarsak(deltakerId: UUID, endreSluttaarsakRequest: EndreSluttaarsakRequest): UUID {
		val request =
			Request
				.Builder()
				.url("$amtTiltakUrl/api/tiltaksarrangor/deltaker/$deltakerId/sluttaarsak")
				.patch(objectMapper.writeValueAsString(endreSluttaarsakRequest).toRequestBody(mediaTypeJson))
				.build()

		amtTiltakHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				handleUnsuccessfulUpdateResponse(response.code, "endre sluttårsak på deltaker $deltakerId")
			}
			val body = response.body?.string() ?: throw RuntimeException("Tom responsbody")

			return JsonUtils.fromJsonString<OpprettEndringsmeldingResponse>(body).id
		}
	}

	fun tilbakekallEndringsmelding(endringsmeldingId: UUID) {
		val request =
			Request
				.Builder()
				.url("$amtTiltakUrl/api/tiltaksarrangor/endringsmelding/$endringsmeldingId/tilbakekall")
				.patch(emptyRequest())
				.build()

		amtTiltakHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				handleUnsuccessfulUpdateResponse(response.code, "tilbakekalle endringsmelding med id $endringsmeldingId")
			}
		}
	}

	fun registrerVurdering(deltakerId: UUID, registrerVurderingRequest: RegistrerVurderingRequest): List<Vurdering> {
		val request =
			Request
				.Builder()
				.url("$amtTiltakUrl/api/tiltaksarrangor/deltaker/$deltakerId/vurdering")
				.post(objectMapper.writeValueAsString(registrerVurderingRequest).toRequestBody(mediaTypeJson))
				.build()

		amtTiltakHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				handleUnsuccessfulUpdateResponse(response.code, "registrere vurdering for deltaker med id $deltakerId")
			}
			val body = response.body?.string() ?: throw RuntimeException("Tom responsbody")

			return JsonUtils.fromJsonString<List<Vurdering>>(body)
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
