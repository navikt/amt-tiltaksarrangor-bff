package no.nav.tiltaksarrangor.melding.forslag

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import no.nav.amt.lib.models.arrangor.melding.Forslag
import no.nav.tiltaksarrangor.IntegrationTest
import no.nav.tiltaksarrangor.melding.forslag.request.ForlengDeltakelseRequest
import no.nav.tiltaksarrangor.testutils.DbTestDataUtils.shouldBeCloseTo
import no.nav.tiltaksarrangor.testutils.DeltakerContext
import no.nav.tiltaksarrangor.utils.JsonUtils
import no.nav.tiltaksarrangor.utils.JsonUtils.objectMapper
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class ForslagControllerTest : IntegrationTest() {
	private val mediaTypeJson = "application/json".toMediaType()

	@Autowired
	private lateinit var forslagService: ForslagService

	@Test
	fun `skal teste token autentisering`() {
		val requestBuilders = listOf(
			Request.Builder().post(emptyRequest()).url("${serverUrl()}/tiltaksarrangor/deltaker/${UUID.randomUUID()}/forslag/forleng"),
			Request.Builder()
				.post(emptyRequest())
				.url("${serverUrl()}/tiltaksarrangor/deltaker/${UUID.randomUUID()}/forslag/${UUID.randomUUID()}/tilbakekall"),
		)
		testTokenAutentisering(requestBuilders)
	}

	private fun testTokenAutentisering(requestBuilders: List<Request.Builder>) {
		requestBuilders.forEach {
			val utenTokenResponse = client.newCall(it.build()).execute()
			utenTokenResponse.code shouldBe 401
			val feilTokenResponse = client.newCall(
				it.header(
					name = "Authorization",
					value = "Bearer ${mockOAuth2Server.issueToken("ikke-azuread").serialize()}",
				)
					.build(),
			).execute()
			feilTokenResponse.code shouldBe 401
		}
	}

	@Test
	fun `forleng - nytt forslag - skal returnere 200 og riktig response`() {
		with(DeltakerContext()) {
			val response = sendForlengDeltakelseRequest(deltaker.id, koordinator.personIdent)

			response.code shouldBe 200

			val aktivtForslag = objectMapper.readValue<AktivtForslagResponse>(response.body!!.string())
			aktivtForslag.status shouldBe ForslagResponse.Status.VenterPaSvar
			aktivtForslag.begrunnelse shouldBe forlengDeltakelseRequest.begrunnelse
			aktivtForslag.opprettet shouldBeCloseTo LocalDateTime.now()

			val endring = aktivtForslag.endring as Forslag.ForlengDeltakelse
			endring.sluttdato shouldBe forlengDeltakelseRequest.sluttdato
		}
	}

	@Test
	fun `forleng - har ikke tilgang til deltakerliste - skal returnere 403`() {
		testIkkeTilgangTilDeltakerliste { deltakerId, ansattPersonIdent ->
			sendForlengDeltakelseRequest(deltakerId, ansattPersonIdent)
		}
	}

	@Test
	fun `forleng - deltaker adressebeskyttet, ansatt er ikke veileder - skal returnere 403`() {
		testDeltakerAdressebeskyttet { deltakerId, ansattPersonIdent ->
			sendForlengDeltakelseRequest(deltakerId, ansattPersonIdent)
		}
	}

	@Test
	fun `forleng - deltaker skjult - skal returnere 400`() {
		testDeltakerSkjult { deltakerId, ansattPersonIdent ->
			sendForlengDeltakelseRequest(deltakerId, ansattPersonIdent)
		}
	}

	@Test
	fun `tilbakekall - aktivt forslag - skal returnere 200`() {
		with(ForslagCtx(forlengDeltakelseForslag())) {
			upsertForslag()
			val response = sendTilbakekallRequest(forslag.id, deltaker.id, koordinator.personIdent)

			response.code shouldBe 200

			forslagService.get(forslag.id).isFailure shouldBe true
		}
	}

	@Test
	fun `tilbakekall - har ikke tilgang til deltakerliste - skal returnere 403`() {
		testIkkeTilgangTilDeltakerliste { deltakerId, ansattPersonIdent ->
			sendTilbakekallRequest(UUID.randomUUID(), deltakerId, ansattPersonIdent)
		}
	}

	@Test
	fun `tilbakekall - deltaker adressebeskyttet, ansatt er ikke veileder - skal returnere 403`() {
		testDeltakerAdressebeskyttet { deltakerId, ansattPersonIdent ->
			sendTilbakekallRequest(UUID.randomUUID(), deltakerId, ansattPersonIdent)
		}
	}

	@Test
	fun `tilbakekall - deltaker skjult - skal returnere 400`() {
		testDeltakerSkjult { deltakerId, ansattPersonIdent ->
			sendTilbakekallRequest(UUID.randomUUID(), deltakerId, ansattPersonIdent)
		}
	}

	private fun testIkkeTilgangTilDeltakerliste(requestFunction: (deltakerId: UUID, ansattPersonIdent: String) -> Response) {
		with(DeltakerContext()) {
			setKoordinatorDeltakerliste(UUID.randomUUID())

			val response = requestFunction(deltaker.id, koordinator.personIdent)

			response.code shouldBe 403
		}
	}

	private fun testDeltakerAdressebeskyttet(requestFunction: (deltakerId: UUID, ansattPersonIdent: String) -> Response) {
		with(DeltakerContext()) {
			setDeltakerAdressebeskyttet()

			val response = requestFunction(deltaker.id, koordinator.personIdent)

			response.code shouldBe 403
		}
	}

	private fun testDeltakerSkjult(requestFunction: (deltakerId: UUID, ansattPersonIdent: String) -> Response) {
		with(DeltakerContext()) {
			setDeltakerSkjult()

			val response = requestFunction(deltaker.id, koordinator.personIdent)

			response.code shouldBe 400
		}
	}

	private val forlengDeltakelseRequest = ForlengDeltakelseRequest(LocalDate.now().plusWeeks(42), "Fordi")

	private fun sendForlengDeltakelseRequest(
		deltakerId: UUID,
		ansattIdent: String,
		request: ForlengDeltakelseRequest = forlengDeltakelseRequest,
	) = sendRequest(
		method = "POST",
		path = "/tiltaksarrangor/deltaker/$deltakerId/forslag/forleng",
		body = JsonUtils.objectMapper.writeValueAsString(request).toRequestBody(mediaTypeJson),
		headers = mapOf("Authorization" to "Bearer ${getTokenxToken(fnr = ansattIdent)}"),
	)

	private fun sendTilbakekallRequest(
		forslagId: UUID,
		deltakerId: UUID,
		ansattIdent: String,
	) = sendRequest(
		method = "POST",
		path = "/tiltaksarrangor/deltaker/$deltakerId/forslag/$forslagId/tilbakekall",
		body = emptyRequest(),
		headers = mapOf("Authorization" to "Bearer ${getTokenxToken(fnr = ansattIdent)}"),
	)
}
