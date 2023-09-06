package no.nav.tiltaksarrangor.mock

import no.nav.tiltaksarrangor.client.amttiltak.response.OpprettEndringsmeldingResponse
import no.nav.tiltaksarrangor.ingest.model.VurderingDto
import no.nav.tiltaksarrangor.utils.JsonUtils
import okhttp3.mockwebserver.MockResponse
import java.util.UUID

class MockAmtTiltakHttpServer : MockHttpServer(name = "Amt-Tiltak Mock Server") {
	fun addAvsluttDeltakelseResponse(deltakerId: UUID) {
		addResponseHandler(
			path = "/api/tiltaksarrangor/deltaker/$deltakerId/avslutt-deltakelse",
			MockResponse()
				.setResponseCode(200)
				.setBody(JsonUtils.objectMapper.writeValueAsString(OpprettEndringsmeldingResponse(UUID.randomUUID())))
		)
	}

	fun addDeltakerErAktuellResponse(deltakerId: UUID) {
		addResponseHandler(
			path = "/api/tiltaksarrangor/deltaker/$deltakerId/er-aktuell",
			MockResponse()
				.setResponseCode(200)
				.setBody(JsonUtils.objectMapper.writeValueAsString(OpprettEndringsmeldingResponse(UUID.randomUUID())))
		)
	}

	fun addEndreOppstartsdatoResponse(deltakerId: UUID) {
		addResponseHandler(
			path = "/api/tiltaksarrangor/deltaker/$deltakerId/oppstartsdato",
			MockResponse()
				.setResponseCode(200)
				.setBody(JsonUtils.objectMapper.writeValueAsString(OpprettEndringsmeldingResponse(UUID.randomUUID())))
		)
	}

	fun addEndreSluttdatoResponse(deltakerId: UUID) {
		addResponseHandler(
			path = "/api/tiltaksarrangor/deltaker/$deltakerId/endre-sluttdato",
			MockResponse()
				.setResponseCode(200)
				.setBody(JsonUtils.objectMapper.writeValueAsString(OpprettEndringsmeldingResponse(UUID.randomUUID())))
		)
	}

	fun addTilbakekallEndringsmeldingResponse(endringsmeldingId: UUID) {
		addResponseHandler(
			path = "/api/tiltaksarrangor/endringsmelding/$endringsmeldingId/tilbakekall",
			MockResponse()
				.setResponseCode(200)
		)
	}

	fun addSkjulDeltakerResponse(deltakerId: UUID) {
		addResponseHandler(
			path = "/api/tiltaksarrangor/deltaker/$deltakerId/skjul",
			MockResponse()
				.setResponseCode(200)
		)
	}

	fun addRegistrerVurderingResponse(deltakerId: UUID, vurderinger: List<VurderingDto>) {
		addResponseHandler(
			path = "/api/tiltaksarrangor/deltaker/$deltakerId/vurdering",
			MockResponse()
				.setResponseCode(200)
				.setBody(JsonUtils.objectMapper.writeValueAsString(vurderinger))
		)
	}
}
