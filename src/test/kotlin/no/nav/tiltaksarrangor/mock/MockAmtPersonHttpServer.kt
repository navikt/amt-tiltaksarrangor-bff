package no.nav.tiltaksarrangor.mock

import no.nav.tiltaksarrangor.client.amtperson.NavAnsattResponse
import no.nav.tiltaksarrangor.client.amtperson.NavEnhetDto
import no.nav.tiltaksarrangor.utils.JsonUtils
import okhttp3.mockwebserver.MockResponse
import java.util.UUID

class MockAmtPersonHttpServer : MockHttpServer(name = "amt-person-server") {
	fun addEnhetResponse(id: UUID = UUID.randomUUID()) {
		val enhetResponse = NavEnhetDto(
			id,
			"EnhetId",
			"Nav Oslo",
		)
		addResponseHandler(
			path = "/api/nav-enhet/$id",
			MockResponse()
				.setResponseCode(200)
				.setBody(JsonUtils.objectMapper.writeValueAsString(enhetResponse)),
		)
	}

	fun addAnsattResponse(id: UUID = UUID.randomUUID()) {
		val ansattResponse = NavAnsattResponse(
			id = id,
			navIdent = "NAVident",
			navn = "Navn Navnsen",
			epost = "navn.navsen@test.no",
			telefon = "123",
		)
		addResponseHandler(
			path = "/api/nav-ansatt/$id",
			MockResponse()
				.setResponseCode(200)
				.setBody(JsonUtils.objectMapper.writeValueAsString(ansattResponse)),
		)
	}
}
