package no.nav.tiltaksarrangor.mock

import no.nav.tiltaksarrangor.utils.JsonUtils
import okhttp3.mockwebserver.MockResponse

class MockAmtTiltakHttpServer : MockHttpServer(name = "Amt-Tiltak Mock Server") {
	fun addMineRollerResponse() {
		addResponseHandler(
			path = "/api/tiltaksarrangor/ansatt/meg/roller",
			MockResponse()
				.setResponseCode(200)
				.setBody(JsonUtils.objectMapper.writeValueAsString(listOf("KOORDINATOR", "VEILEDER")))
		)
	}
}
