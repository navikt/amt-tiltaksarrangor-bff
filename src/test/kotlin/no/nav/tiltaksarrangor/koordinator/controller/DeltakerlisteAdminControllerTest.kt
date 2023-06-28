package no.nav.tiltaksarrangor.koordinator.controller

import io.kotest.matchers.shouldBe
import no.nav.tiltaksarrangor.IntegrationTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.UUID

class DeltakerlisteAdminControllerTest : IntegrationTest() {
	@AfterEach
	internal fun tearDown() {
		mockAmtTiltakServer.resetHttpServer()
	}

	@Test
	fun `getAlleDeltakerlister - ikke autentisert - returnerer 401`() {
		val response = sendRequest(
			method = "GET",
			path = "/tiltaksarrangor/koordinator/admin/deltakerlister"
		)

		response.code shouldBe 401
	}

	@Test
	fun `getAlleDeltakerlister - autentisert - returnerer 200`() {
		val deltakerlisteId = UUID.fromString("9987432c-e336-4b3b-b73e-b7c781a0823a")
		mockAmtTiltakServer.addGetTilgjengeligeDeltakerlisterResponse(deltakerlisteId)
		mockAmtTiltakServer.addGetDeltakerlisterLagtTilResponse(deltakerlisteId)

		val response = sendRequest(
			method = "GET",
			path = "/tiltaksarrangor/koordinator/admin/deltakerlister",
			headers = mapOf("Authorization" to "Bearer ${getTokenxToken(fnr = "12345678910")}")
		)

		val expectedJson = """
			[{"id":"9987432c-e336-4b3b-b73e-b7c781a0823a","navn":"Gjennomføring 1","tiltaksnavn":"Navn på tiltak","arrangorNavn":"Arrangør AS","arrangorOrgnummer":"88888888","arrangorParentNavn":"Arrangør AS","startDato":"2023-02-01","sluttDato":null,"lagtTil":true},{"id":"fd70758a-44c5-4868-bdcb-b1ddd26cb5e9","navn":"Gjennomføring 2","tiltaksnavn":"Annet tiltak","arrangorNavn":"Arrangør AS","arrangorOrgnummer":"88888888","arrangorParentNavn":"Arrangør AS","startDato":"2023-05-01","sluttDato":"2023-06-01","lagtTil":false}]
		""".trimIndent()
		response.code shouldBe 200
		response.body?.string() shouldBe expectedJson
	}

	@Test
	fun `leggTilDeltakerliste - ikke autentisert - returnerer 401`() {
		val response = sendRequest(
			method = "POST",
			path = "/tiltaksarrangor/koordinator/admin/deltakerliste/${UUID.randomUUID()}",
			body = emptyRequest()
		)

		response.code shouldBe 401
	}

	@Test
	fun `leggTilDeltakerliste - autentisert - returnerer 200`() {
		val deltakerlisteId = UUID.fromString("9987432c-e336-4b3b-b73e-b7c781a0823a")
		mockAmtTiltakServer.addOpprettEllerFjernTilgangTilGjennomforingResponse(deltakerlisteId)

		val response = sendRequest(
			method = "POST",
			path = "/tiltaksarrangor/koordinator/admin/deltakerliste/$deltakerlisteId",
			body = emptyRequest(),
			headers = mapOf("Authorization" to "Bearer ${getTokenxToken(fnr = "12345678910")}")
		)

		response.code shouldBe 200
	}

	@Test
	fun `fjernDeltakerliste - ikke autentisert - returnerer 401`() {
		val response = sendRequest(
			method = "DELETE",
			path = "/tiltaksarrangor/koordinator/admin/deltakerliste/${UUID.randomUUID()}"
		)

		response.code shouldBe 401
	}

	@Test
	fun `fjernDeltakerliste - autentisert - returnerer 200`() {
		val deltakerlisteId = UUID.fromString("9987432c-e336-4b3b-b73e-b7c781a0823a")
		mockAmtTiltakServer.addOpprettEllerFjernTilgangTilGjennomforingResponse(deltakerlisteId)

		val response = sendRequest(
			method = "DELETE",
			path = "/tiltaksarrangor/koordinator/admin/deltakerliste/$deltakerlisteId",
			headers = mapOf("Authorization" to "Bearer ${getTokenxToken(fnr = "12345678910")}")
		)

		response.code shouldBe 200
	}
}

private fun emptyRequest(): RequestBody {
	val mediaTypeHtml = "text/html".toMediaType()
	return "".toRequestBody(mediaTypeHtml)
}
