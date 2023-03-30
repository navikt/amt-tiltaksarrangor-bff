package no.nav.tiltaksarrangor.koordinator.controller

import io.kotest.matchers.shouldBe
import no.nav.tiltaksarrangor.IntegrationTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class KoordinatorControllerTest : IntegrationTest() {
	@AfterEach
	internal fun tearDown() {
		mockAmtTiltakServer.resetHttpServer()
	}

	@Test
	fun `getMineDeltakere - ikke autentisert - returnerer 401`() {
		val response = sendRequest(
			method = "GET",
			path = "/tiltaksarrangor/koordinator/mine-deltakerlister"
		)

		response.code shouldBe 401
	}

	@Test
	fun `getMineDeltakere - autentisert - returnerer 200`() {
		mockAmtTiltakServer.addMineDeltakerlisterResponse()

		val response = sendRequest(
			method = "GET",
			path = "/tiltaksarrangor/koordinator/mine-deltakerlister",
			headers = mapOf("Authorization" to "Bearer ${getTokenxToken(fnr = "12345678910")}")
		)

		val expectedJson = """
			{"veilederFor":{"veilederFor":4,"medveilederFor":7},"koordinatorFor":{"deltakerlister":[{"id":"9987432c-e336-4b3b-b73e-b7c781a0823a","type":"ARBFORB","navn":"Gjennomføring 1"}]}}
		""".trimIndent()
		response.code shouldBe 200
		response.body?.string() shouldBe expectedJson
	}
}
