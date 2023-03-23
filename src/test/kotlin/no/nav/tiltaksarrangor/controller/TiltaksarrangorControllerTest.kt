package no.nav.tiltaksarrangor.controller

import io.kotest.matchers.shouldBe
import no.nav.tiltaksarrangor.IntegrationTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.UUID

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

	@Test
	fun `getDeltaker - ikke autentisert - returnerer 401`() {
		val response = sendRequest(
			method = "GET",
			path = "/tiltaksarrangor/deltaker/${UUID.randomUUID()}"
		)

		response.code shouldBe 401
	}

	@Test
	fun `getDeltaker - autentisert, har ikke tilgang - returnerer 403`() {
		val deltakerId = UUID.randomUUID()
		mockAmtTiltakServer.addDeltakerFailureResponse(deltakerId)

		val response = sendRequest(
			method = "GET",
			path = "/tiltaksarrangor/deltaker/$deltakerId",
			headers = mapOf("Authorization" to "Bearer ${getTokenxToken(fnr = "12345678910")}")
		)

		response.code shouldBe 403
	}

	@Test
	fun `getDeltaker - autentisert, har tilgang - returnerer 200`() {
		val deltakerId = UUID.fromString("977350f2-d6a5-49bb-a3a0-773f25f863d9")
		mockAmtTiltakServer.addDeltakerResponse(deltakerId)
		mockAmtTiltakServer.addAktiveEndringsmeldingerResponse(deltakerId)

		val response = sendRequest(
			method = "GET",
			path = "/tiltaksarrangor/deltaker/$deltakerId",
			headers = mapOf("Authorization" to "Bearer ${getTokenxToken(fnr = "12345678910")}")
		)

		val expectedJson = """
			{"id":"977350f2-d6a5-49bb-a3a0-773f25f863d9","deltakerlisteId":"9987432c-e336-4b3b-b73e-b7c781a0823a","fornavn":"Fornavn","mellomnavn":null,"etternavn":"Etternavn","fodselsnummer":"10987654321","telefonnummer":"90909090","epost":"mail@test.no","status":{"type":"DELTAR","endretDato":"2023-02-01T00:00:00"},"startDato":"2023-02-01","sluttDato":null,"deltakelseProsent":null,"soktInnPa":"Gjennomføring 1","soktInnDato":"2023-01-15T00:00:00","tiltakskode":"ARBFORB","bestillingTekst":"Tror deltakeren vil ha nytte av dette","fjernesDato":null,"navInformasjon":{"navkontor":"Nav Oslo","navVeileder":{"navn":"Veileder Veiledersen","epostadresse":"epost@nav.no","telefonnummer":"56565656"}},"aktiveEndringsmeldinger":[{"id":"27446cc8-30ad-4030-94e3-de438c2af3c6","innhold":{"sluttdato":"2023-03-30","aarsak":"SYK","beskrivelse":"har blitt syk"},"type":"AVSLUTT_DELTAKELSE"}]}
		""".trimIndent()
		response.code shouldBe 200
		response.body?.string() shouldBe expectedJson
	}
}
