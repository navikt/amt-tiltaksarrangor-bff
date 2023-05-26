package no.nav.tiltaksarrangor.controller

import io.kotest.matchers.shouldBe
import no.nav.tiltaksarrangor.IntegrationTest
import no.nav.tiltaksarrangor.controller.request.EndringsmeldingRequest
import no.nav.tiltaksarrangor.model.DeltakerStatusAarsak
import no.nav.tiltaksarrangor.utils.JsonUtils
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class TiltaksarrangorControllerTest : IntegrationTest() {

	private val mediaTypeJson = "application/json".toMediaType()

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
		mockAmtTiltakServer.addVeilederResponse(deltakerId)
		mockAmtTiltakServer.addAktiveEndringsmeldingerResponse(deltakerId)

		val response = sendRequest(
			method = "GET",
			path = "/tiltaksarrangor/deltaker/$deltakerId",
			headers = mapOf("Authorization" to "Bearer ${getTokenxToken(fnr = "12345678910")}")
		)

		val expectedJson = """
			{"id":"977350f2-d6a5-49bb-a3a0-773f25f863d9","deltakerliste":{"id":"9987432c-e336-4b3b-b73e-b7c781a0823a","startDato":"2023-02-01","sluttDato":null,"erKurs":false},"fornavn":"Fornavn","mellomnavn":null,"etternavn":"Etternavn","fodselsnummer":"10987654321","telefonnummer":"90909090","epost":"mail@test.no","status":{"type":"DELTAR","endretDato":"2023-02-01T00:00:00"},"startDato":"2023-02-01","sluttDato":null,"deltakelseProsent":null,"dagerPerUke":5,"soktInnPa":"Gjennomføring 1","soktInnDato":"2023-01-15T00:00:00","tiltakskode":"ARBFORB","bestillingTekst":"Tror deltakeren vil ha nytte av dette","fjernesDato":null,"navInformasjon":{"navkontor":"Nav Oslo","navVeileder":{"navn":"Veileder Veiledersen","epost":"epost@nav.no","telefon":"56565656"}},"veiledere":[{"id":"4f2fb9a7-69c7-4524-bc52-2d00344675ab","ansattId":"2d5fc2f7-a9e6-4830-a987-4ff135a70c10","deltakerId":"977350f2-d6a5-49bb-a3a0-773f25f863d9","veiledertype":"VEILEDER","fornavn":"Fornavn","mellomnavn":null,"etternavn":"Etternavn"},{"id":"d8e1af61-0c5e-4843-8bca-ecd9b55c44bc","ansattId":"7c43b43b-43be-4d4b-8057-d907c5f1e5c5","deltakerId":"977350f2-d6a5-49bb-a3a0-773f25f863d9","veiledertype":"MEDVEILEDER","fornavn":"Per","mellomnavn":null,"etternavn":"Person"}],"aktiveEndringsmeldinger":[{"id":"27446cc8-30ad-4030-94e3-de438c2af3c6","innhold":{"sluttdato":"2023-03-30","aarsak":{"type":"SYK","beskrivelse":"har blitt syk"}},"type":"AVSLUTT_DELTAKELSE"},{"id":"5029689f-3de6-4d97-9cfa-552f75625ef2","innhold":null,"type":"DELTAKER_ER_AKTUELL"},{"id":"362c7fdd-04e7-4f43-9e56-0939585856eb","innhold":{"sluttdato":"2023-05-03"},"type":"ENDRE_SLUTTDATO"}]}
		""".trimIndent()
		response.code shouldBe 200
		response.body?.string() shouldBe expectedJson
	}

	@Test
	fun `getAktiveEndringsmeldinger - ikke autentisert - returnerer 401`() {
		val response = sendRequest(
			method = "GET",
			path = "/tiltaksarrangor/deltaker/${UUID.randomUUID()}/endringsmeldinger"
		)

		response.code shouldBe 401
	}

	@Test
	fun `getAktiveEndringsmeldinger - autentisert - returnerer 200`() {
		val deltakerId = UUID.fromString("977350f2-d6a5-49bb-a3a0-773f25f863d9")
		mockAmtTiltakServer.addAktiveEndringsmeldingerResponse(deltakerId)

		val response = sendRequest(
			method = "GET",
			path = "/tiltaksarrangor/deltaker/$deltakerId/endringsmeldinger",
			headers = mapOf("Authorization" to "Bearer ${getTokenxToken(fnr = "12345678910")}")
		)

		val expectedJson = """
			[{"id":"27446cc8-30ad-4030-94e3-de438c2af3c6","innhold":{"sluttdato":"2023-03-30","aarsak":{"type":"SYK","beskrivelse":"har blitt syk"}},"type":"AVSLUTT_DELTAKELSE"},{"id":"5029689f-3de6-4d97-9cfa-552f75625ef2","innhold":null,"type":"DELTAKER_ER_AKTUELL"},{"id":"362c7fdd-04e7-4f43-9e56-0939585856eb","innhold":{"sluttdato":"2023-05-03"},"type":"ENDRE_SLUTTDATO"}]
		""".trimIndent()
		response.code shouldBe 200
		response.body?.string() shouldBe expectedJson
	}

	@Test
	fun `opprettEndringsmelding - ikke autentisert - returnerer 401`() {
		val requestBody = EndringsmeldingRequest(
			innhold = EndringsmeldingRequest.Innhold.AvsluttDeltakelseInnhold(
				sluttdato = LocalDate.now(),
				aarsak = DeltakerStatusAarsak(DeltakerStatusAarsak.Type.FATT_JOBB, null)
			)
		)
		val response = sendRequest(
			method = "POST",
			path = "/tiltaksarrangor/deltaker/${UUID.randomUUID()}/endringsmelding",
			body = JsonUtils.objectMapper.writeValueAsString(requestBody).toRequestBody(mediaTypeJson)
		)

		response.code shouldBe 401
	}

	@Test
	fun `opprettEndringsmelding - autentisert, avslutt deltakelse - returnerer 200`() {
		val deltakerId = UUID.fromString("da4c9568-cea2-42e3-95a3-42f6b809ad08")
		mockAmtTiltakServer.addAvsluttDeltakelseResponse(deltakerId)
		val requestBody = EndringsmeldingRequest(
			innhold = EndringsmeldingRequest.Innhold.AvsluttDeltakelseInnhold(
				sluttdato = LocalDate.now(),
				aarsak = DeltakerStatusAarsak(DeltakerStatusAarsak.Type.FATT_JOBB, null)
			)

		)

		val response = sendRequest(
			method = "POST",
			path = "/tiltaksarrangor/deltaker/$deltakerId/endringsmelding",
			body = JsonUtils.objectMapper.writeValueAsString(requestBody).toRequestBody(mediaTypeJson),
			headers = mapOf("Authorization" to "Bearer ${getTokenxToken(fnr = "12345678910")}")
		)

		response.code shouldBe 200
	}

	@Test
	fun `opprettEndringsmelding - deltaker er aktuell json request - returnerer 200`() {
		val deltakerId = UUID.fromString("da4c9568-cea2-42e3-95a3-42f6b809ad08")
		mockAmtTiltakServer.addDeltakerErAktuellResponse(deltakerId)

		val requestStr = """{"innhold":{"type":"DELTAKER_ER_AKTUELL"}}"""
		val response = sendRequest(
			method = "POST",
			path = "/tiltaksarrangor/deltaker/$deltakerId/endringsmelding",
			body = requestStr.toRequestBody(mediaTypeJson),
			headers = mapOf("Authorization" to "Bearer ${getTokenxToken(fnr = "12345678910")}")
		)

		response.code shouldBe 200
	}

	@Test
	fun `opprettEndringsmelding - autentisert, deltaker er aktuell - returnerer 200`() {
		val deltakerId = UUID.fromString("da4c9568-cea2-42e3-95a3-42f6b809ad08")
		mockAmtTiltakServer.addDeltakerErAktuellResponse(deltakerId)
		val requestBody = EndringsmeldingRequest(
			innhold = EndringsmeldingRequest.Innhold.DeltakerErAktuellInnhold()
		)

		val response = sendRequest(
			method = "POST",
			path = "/tiltaksarrangor/deltaker/$deltakerId/endringsmelding",
			body = JsonUtils.objectMapper.writeValueAsString(requestBody).toRequestBody(mediaTypeJson),
			headers = mapOf("Authorization" to "Bearer ${getTokenxToken(fnr = "12345678910")}")
		)

		response.code shouldBe 200
	}

	@Test
	fun `opprettEndringsmelding - autentisert, endre sluttdato - returnerer 200`() {
		val deltakerId = UUID.fromString("da4c9568-cea2-42e3-95a3-42f6b809ad08")
		mockAmtTiltakServer.addEndreSluttdatoResponse(deltakerId)
		val requestBody = EndringsmeldingRequest(
			innhold = EndringsmeldingRequest.Innhold.EndreSluttdatoInnhold(
				sluttdato = LocalDate.now()
			)
		)

		val response = sendRequest(
			method = "POST",
			path = "/tiltaksarrangor/deltaker/$deltakerId/endringsmelding",
			body = JsonUtils.objectMapper.writeValueAsString(requestBody).toRequestBody(mediaTypeJson),
			headers = mapOf("Authorization" to "Bearer ${getTokenxToken(fnr = "12345678910")}")
		)

		response.code shouldBe 200
	}

	@Test
	fun `slettEndringsmelding - ikke autentisert - returnerer 401`() {
		val response = sendRequest(
			method = "DELETE",
			path = "/tiltaksarrangor/endringsmelding/${UUID.randomUUID()}"
		)

		response.code shouldBe 401
	}

	@Test
	fun `slettEndringsmelding - autentisert - returnerer 200`() {
		val endringsmeldingId = UUID.fromString("27446cc8-30ad-4030-94e3-de438c2af3c6")
		mockAmtTiltakServer.addTilbakekallEndringsmeldingResponse(endringsmeldingId)

		val response = sendRequest(
			method = "DELETE",
			path = "/tiltaksarrangor/endringsmelding/$endringsmeldingId",
			headers = mapOf("Authorization" to "Bearer ${getTokenxToken(fnr = "12345678910")}")
		)

		response.code shouldBe 200
	}

	@Test
	fun `fjernDeltaker - ikke autentisert - returnerer 401`() {
		val response = sendRequest(
			method = "DELETE",
			path = "/tiltaksarrangor/deltaker/${UUID.randomUUID()}"
		)

		response.code shouldBe 401
	}

	@Test
	fun `fjernDeltaker - autentisert - returnerer 200`() {
		val deltakerId = UUID.fromString("27446cc8-30ad-4030-94e3-de438c2af3c6")
		mockAmtTiltakServer.addSkjulDeltakerResponse(deltakerId)

		val response = sendRequest(
			method = "DELETE",
			path = "/tiltaksarrangor/deltaker/$deltakerId",
			headers = mapOf("Authorization" to "Bearer ${getTokenxToken(fnr = "12345678910")}")
		)

		response.code shouldBe 200
	}
}
