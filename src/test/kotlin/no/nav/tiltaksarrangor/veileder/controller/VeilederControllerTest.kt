package no.nav.tiltaksarrangor.veileder.controller

import io.kotest.matchers.shouldBe
import no.nav.tiltaksarrangor.IntegrationTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.UUID

class VeilederControllerTest : IntegrationTest() {
	@AfterEach
	internal fun tearDown() {
		mockAmtTiltakServer.resetHttpServer()
	}

	@Test
	fun `getMineDeltakere - ikke autentisert - returnerer 401`() {
		val response = sendRequest(
			method = "GET",
			path = "/tiltaksarrangor/veileder/mine-deltakere"
		)

		response.code shouldBe 401
	}

	@Test
	fun `getMineDeltakere - autentisert - returnerer 200`() {
		val deltakerId = UUID.fromString("977350f2-d6a5-49bb-a3a0-773f25f863d9")
		mockAmtTiltakServer.addMineDeltakereResponse(deltakerId)

		val response = sendRequest(
			method = "GET",
			path = "/tiltaksarrangor/veileder/mine-deltakere",
			headers = mapOf("Authorization" to "Bearer ${getTokenxToken(fnr = "12345678910")}")
		)

		val expectedJson = """
			[{"id":"977350f2-d6a5-49bb-a3a0-773f25f863d9","fornavn":"Fornavn","mellomnavn":null,"etternavn":"Etternavn","fodselsnummer":"10987654321","startDato":"2023-02-15","sluttDato":null,"status":{"type":"DELTAR","endretDato":"2023-02-01T00:00:00"},"deltakerliste":{"id":"9987432c-e336-4b3b-b73e-b7c781a0823a","type":"ARBFORB","navn":"Gjennomføring 1"},"veiledertype":"VEILEDER","aktiveEndringsmeldinger":[]}]
		""".trimIndent()
		response.code shouldBe 200
		response.body?.string() shouldBe expectedJson
	}
}
