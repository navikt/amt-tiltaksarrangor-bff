package no.nav.tiltaksarrangor.controller

import io.kotest.matchers.shouldBe
import no.nav.tiltaksarrangor.IntegrationTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class TiltaksarrangorControllerTest : IntegrationTest() {
	@AfterEach
	internal fun tearDown() {
		mockAmtTiltakServer.resetHttpServer()
	}

	@Test
	fun `getMineRoller - ikke autentisert - returnerer 401`() {
		val response = sendRequest(
			method = "GET",
			path = "/tiltaksarrangor/meg/roller"
		)

		response.code shouldBe 401
	}

	@Test
	fun `getMineRoller - autentisert - returnerer 200`() {
		mockAmtTiltakServer.addMineRollerResponse()

		val response = sendRequest(
			method = "GET",
			path = "/tiltaksarrangor/meg/roller",
			headers = mapOf("Authorization" to "Bearer ${getTokenxToken(fnr = "12345678910")}")
		)

		response.code shouldBe 200
		response.body?.string() shouldBe "[\"KOORDINATOR\",\"VEILEDER\"]"
	}
}
