package no.nav.tiltaksarrangor.koordinator.controller

import io.kotest.matchers.shouldBe
import no.nav.tiltaksarrangor.IntegrationTest
import no.nav.tiltaksarrangor.ingest.model.AnsattRolle
import no.nav.tiltaksarrangor.ingest.model.DeltakerlisteStatus
import no.nav.tiltaksarrangor.koordinator.model.LeggTilVeiledereRequest
import no.nav.tiltaksarrangor.koordinator.model.VeilederRequest
import no.nav.tiltaksarrangor.model.StatusType
import no.nav.tiltaksarrangor.model.Veiledertype
import no.nav.tiltaksarrangor.repositories.AnsattRepository
import no.nav.tiltaksarrangor.repositories.DeltakerRepository
import no.nav.tiltaksarrangor.repositories.DeltakerlisteRepository
import no.nav.tiltaksarrangor.repositories.model.AnsattDbo
import no.nav.tiltaksarrangor.repositories.model.AnsattRolleDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerlisteDbo
import no.nav.tiltaksarrangor.repositories.model.KoordinatorDeltakerlisteDbo
import no.nav.tiltaksarrangor.repositories.model.VeilederDeltakerDbo
import no.nav.tiltaksarrangor.utils.JsonUtils
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class KoordinatorControllerTest : IntegrationTest() {
	private val mediaTypeJson = "application/json".toMediaType()
	private val template = NamedParameterJdbcTemplate(postgresDataSource)
	private val ansattRepository = AnsattRepository(template)
	private val deltakerRepository = DeltakerRepository(template)
	private val deltakerlisteRepository = DeltakerlisteRepository(template, deltakerRepository)

	@AfterEach
	internal fun tearDown() {
		mockAmtTiltakServer.resetHttpServer()
		cleanDatabase()
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
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = DeltakerlisteDbo(
			id = UUID.fromString("9987432c-e336-4b3b-b73e-b7c781a0823a"),
			navn = "Gjennomføring 1",
			status = DeltakerlisteStatus.GJENNOMFORES,
			arrangorId = arrangorId,
			tiltakNavn = "Tiltaksnavnet",
			tiltakType = "ARBFORB",
			startDato = null,
			sluttDato = null,
			erKurs = false
		)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId1 = UUID.randomUUID()
		val deltakerId2 = UUID.randomUUID()
		val deltakerId3 = UUID.randomUUID()
		val deltakerId4 = UUID.randomUUID()
		val deltakerId5 = UUID.randomUUID()
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId1))
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId2))
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId3))
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId4))
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId5))
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.randomUUID(),
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller = listOf(AnsattRolleDbo(arrangorId, AnsattRolle.KOORDINATOR), AnsattRolleDbo(arrangorId, AnsattRolle.VEILEDER)),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id)),
				veilederDeltakere = listOf(
					VeilederDeltakerDbo(deltakerId1, Veiledertype.VEILEDER),
					VeilederDeltakerDbo(deltakerId2, Veiledertype.MEDVEILEDER),
					VeilederDeltakerDbo(deltakerId3, Veiledertype.MEDVEILEDER),
					VeilederDeltakerDbo(deltakerId4, Veiledertype.VEILEDER),
					VeilederDeltakerDbo(deltakerId5, Veiledertype.MEDVEILEDER)
				)
			)
		)

		val response = sendRequest(
			method = "GET",
			path = "/tiltaksarrangor/koordinator/mine-deltakerlister",
			headers = mapOf("Authorization" to "Bearer ${getTokenxToken(fnr = personIdent)}")
		)

		val expectedJson = """
			{"veilederFor":{"veilederFor":2,"medveilederFor":3},"koordinatorFor":{"deltakerlister":[{"id":"9987432c-e336-4b3b-b73e-b7c781a0823a","type":"Tiltaksnavnet","navn":"Gjennomføring 1","startdato":null,"sluttdato":null,"erKurs":false}]}}
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
			{"id":"9987432c-e336-4b3b-b73e-b7c781a0823a","navn":"Gjennomføring 1","tiltaksnavn":"Navn på tiltak","arrangorNavn":"Arrangør AS","startDato":"2023-02-01","sluttDato":null,"status":"GJENNOMFORES","koordinatorer":[{"fornavn":"Fornavn1","mellomnavn":null,"etternavn":"Etternavn1"},{"fornavn":"Fornavn2","mellomnavn":null,"etternavn":"Etternavn2"}],"deltakere":[{"id":"252428ac-37a6-4341-bb17-5bad412c9409","fornavn":"Fornavn","mellomnavn":null,"etternavn":"Etternavn","fodselsnummer":"10987654321","soktInnDato":"2023-01-15T00:00:00","startDato":"2023-02-01","sluttDato":null,"status":{"type":"DELTAR","endretDato":"2023-02-01T00:00:00"},"veiledere":[],"navKontor":"NAV Testheim","aktiveEndringsmeldinger":[]}],"erKurs":false}
		""".trimIndent()
		response.code shouldBe 200
		response.body?.string() shouldBe expectedJson
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

	private fun getDeltaker(deltakerId: UUID): DeltakerDbo {
		return DeltakerDbo(
			id = deltakerId,
			deltakerlisteId = UUID.randomUUID(),
			personident = UUID.randomUUID().toString(),
			fornavn = "Fornavn",
			mellomnavn = null,
			etternavn = "Etternavn",
			telefonnummer = null,
			epost = null,
			erSkjermet = false,
			status = StatusType.DELTAR,
			statusOpprettetDato = LocalDateTime.now(),
			statusGyldigFraDato = LocalDate.of(2023, 2, 1).atStartOfDay(),
			dagerPerUke = null,
			prosentStilling = null,
			startdato = LocalDate.of(2023, 2, 15),
			sluttdato = null,
			innsoktDato = LocalDate.now(),
			bestillingstekst = "tekst",
			navKontor = null,
			navVeilederId = null,
			navVeilederEpost = null,
			navVeilederNavn = null,
			navVeilederTelefon = null,
			skjultAvAnsattId = null,
			skjultDato = null
		)
	}
}

private fun emptyRequest(): RequestBody {
	val mediaTypeHtml = "text/html".toMediaType()
	return "".toRequestBody(mediaTypeHtml)
}
