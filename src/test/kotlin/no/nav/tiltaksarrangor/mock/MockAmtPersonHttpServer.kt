package no.nav.tiltaksarrangor.mock

import no.nav.tiltaksarrangor.testutils.getNavEnhet
import no.nav.tiltaksarrangor.utils.JsonUtils
import okhttp3.mockwebserver.MockResponse
import java.util.UUID

class MockAmtPersonHttpServer : MockHttpServer(name = "amt-person-server") {
	fun addEnhetResponse(id: UUID = UUID.randomUUID()) {
		addResponseHandler(
			path = "/api/nav-enhet/$id",
			MockResponse()
				.setResponseCode(200)
				.setBody(JsonUtils.objectMapper.writeValueAsString(getNavEnhet(id))),
		)
	}
}
