package no.nav.tiltaksarrangor.koordinator.controller

import io.kotest.matchers.shouldBe
import no.nav.tiltaksarrangor.IntegrationTest
import no.nav.tiltaksarrangor.koordinator.model.LeggTilVeiledereRequest
import no.nav.tiltaksarrangor.koordinator.model.VeilederRequest
import no.nav.tiltaksarrangor.utils.JsonUtils
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.UUID

class KoordinatorControllerTest : IntegrationTest() {
	private val mediaTypeJson = "application/json".toMediaType()

	@AfterEach
	internal fun tearDown() {
		mockAmtTiltakServer.resetHttpServer()
	}

	@Test
	fun `getMineDeltakerlister - ikke autentisert - returnerer 401`() {
		val response = sendRequest(
			method = "GET",
			path = "/tiltaksarrangor/koordinator/mine-deltakerlister"
		)

		response.code shouldBe 401
	}

	@Test
	fun `getMineDeltakerlister - autentisert - returnerer 200`() {
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

	@Test
	fun `getTilgjengeligeVeiledere - ikke autentisert - returnerer 401`() {
		val response = sendRequest(
			method = "GET",
			path = "/tiltaksarrangor/koordinator/${UUID.randomUUID()}/veiledere"
		)

		response.code shouldBe 401
	}

	@Test
	fun `getTilgjengeligeVeiledere - autentisert - returnerer 200`() {
		val deltakerlisteId = UUID.fromString("9987432c-e336-4b3b-b73e-b7c781a0823a")
		mockAmtTiltakServer.addTilgjengeligeVeiledereResponse(deltakerlisteId)

		val response = sendRequest(
			method = "GET",
			path = "/tiltaksarrangor/koordinator/$deltakerlisteId/veiledere",
			headers = mapOf("Authorization" to "Bearer ${getTokenxToken(fnr = "12345678910")}")
		)

		val expectedJson = """
			[{"ansattId":"29bf6799-bb56-4a86-857b-99b529b3dfc4","fornavn":"Fornavn1","mellomnavn":null,"etternavn":"Etternavn1"},{"ansattId":"e824dbfe-5317-491b-82ed-03b870eed963","fornavn":"Fornavn2","mellomnavn":null,"etternavn":"Etternavn2"}]
		""".trimIndent()
		response.code shouldBe 200
		response.body?.string() shouldBe expectedJson
	}

	@Test
	fun `tildelVeiledereForDeltaker - ikke autentisert - returnerer 401`() {
		val requestBody = LeggTilVeiledereRequest(
			listOf(
				VeilederRequest(
					ansattId = UUID.randomUUID(),
					erMedveileder = false
				),
				VeilederRequest(
					ansattId = UUID.randomUUID(),
					erMedveileder = true
				)
			)
		)
		val response = sendRequest(
			method = "POST",
			path = "/tiltaksarrangor/koordinator/veiledere?deltakerId=${UUID.randomUUID()}",
			body = JsonUtils.objectMapper.writeValueAsString(requestBody).toRequestBody(mediaTypeJson)
		)

		response.code shouldBe 401
	}

	@Test
	fun `tildelVeiledereForDeltaker - autentisert, avslutt deltakelse - returnerer 200`() {
		val deltakerId = UUID.fromString("da4c9568-cea2-42e3-95a3-42f6b809ad08")
		mockAmtTiltakServer.addTildelVeiledereForDeltakerResponse(deltakerId)
		val requestBody = LeggTilVeiledereRequest(
			listOf(
				VeilederRequest(
					ansattId = UUID.randomUUID(),
					erMedveileder = false
				),
				VeilederRequest(
					ansattId = UUID.randomUUID(),
					erMedveileder = true
				)
			)
		)

		val response = sendRequest(
			method = "POST",
			path = "/tiltaksarrangor/koordinator/veiledere?deltakerId=$deltakerId",
			body = JsonUtils.objectMapper.writeValueAsString(requestBody).toRequestBody(mediaTypeJson),
			headers = mapOf("Authorization" to "Bearer ${getTokenxToken(fnr = "12345678910")}")
		)

		response.code shouldBe 200
	}

	@Test
	fun `getDeltakerliste - ikke autentisert - returnerer 401`() {
		val response = sendRequest(
			method = "GET",
			path = "/tiltaksarrangor/koordinator/deltakerliste/${UUID.randomUUID()}"
		)

		response.code shouldBe 401
	}

	@Test
	fun `getDeltakerliste - autentisert - returnerer 200`() {
		val deltakerlisteId = UUID.fromString("9987432c-e336-4b3b-b73e-b7c781a0823a")
		mockAmtTiltakServer.addKoordinatorerResponse(deltakerlisteId)
		mockAmtTiltakServer.addGjennomforingResponse(deltakerlisteId)
		mockAmtTiltakServer.addDeltakerePaGjennomforingResponse(deltakerlisteId)

		val response = sendRequest(
			method = "GET",
			path = "/tiltaksarrangor/koordinator/deltakerliste/$deltakerlisteId",
			headers = mapOf("Authorization" to "Bearer ${getTokenxToken(fnr = "12345678910")}")
		)

		val expectedJson = """
			{"id":"9987432c-e336-4b3b-b73e-b7c781a0823a","navn":"Gjennomføring 1","tiltaksnavn":"Navn på tiltak","arrangorNavn":"Arrangør AS","startDato":"2023-02-01","sluttDato":null,"status":"GJENNOMFORES","koordinatorer":[{"fornavn":"Fornavn1","mellomnavn":null,"etternavn":"Etternavn1"},{"fornavn":"Fornavn2","mellomnavn":null,"etternavn":"Etternavn2"}],"deltakere":[{"id":"252428ac-37a6-4341-bb17-5bad412c9409","fornavn":"Fornavn","mellomnavn":null,"etternavn":"Etternavn","fodselsnummer":"10987654321","soktInnDato":"2023-01-15T00:00:00","startDato":"2023-02-01","sluttDato":null,"status":{"type":"DELTAR","endretDato":"2023-02-01T00:00:00"},"veiledere":[],"aktiveEndringsmeldinger":[]}]}
		""".trimIndent()
		response.code shouldBe 200
		response.body?.string() shouldBe expectedJson
	}
}
