package no.nav.tiltaksarrangor.endringsmelding.controller

import io.kotest.matchers.shouldBe
import no.nav.tiltaksarrangor.IntegrationTest
import no.nav.tiltaksarrangor.endringsmelding.controller.request.EndringsmeldingRequest
import no.nav.tiltaksarrangor.ingest.model.AnsattRolle
import no.nav.tiltaksarrangor.ingest.model.EndringsmeldingType
import no.nav.tiltaksarrangor.ingest.model.Innhold
import no.nav.tiltaksarrangor.model.DeltakerStatusAarsak
import no.nav.tiltaksarrangor.repositories.AnsattRepository
import no.nav.tiltaksarrangor.repositories.DeltakerRepository
import no.nav.tiltaksarrangor.repositories.DeltakerlisteRepository
import no.nav.tiltaksarrangor.repositories.EndringsmeldingRepository
import no.nav.tiltaksarrangor.repositories.model.AnsattDbo
import no.nav.tiltaksarrangor.repositories.model.AnsattRolleDbo
import no.nav.tiltaksarrangor.repositories.model.EndringsmeldingDbo
import no.nav.tiltaksarrangor.repositories.model.KoordinatorDeltakerlisteDbo
import no.nav.tiltaksarrangor.testutils.getDeltaker
import no.nav.tiltaksarrangor.testutils.getDeltakerliste
import no.nav.tiltaksarrangor.testutils.getEndringsmelding
import no.nav.tiltaksarrangor.utils.JsonUtils
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDate
import java.util.UUID

class EndringsmeldingControllerTest : IntegrationTest() {
	private val mediaTypeJson = "application/json".toMediaType()
	private val template = NamedParameterJdbcTemplate(postgresDataSource)
	private val ansattRepository = AnsattRepository(template)
	private val deltakerRepository = DeltakerRepository(template)
	private val deltakerlisteRepository = DeltakerlisteRepository(template, deltakerRepository)
	private val endringsmeldingRepository = EndringsmeldingRepository(template)

	@AfterEach
	internal fun tearDown() {
		mockAmtTiltakServer.resetHttpServer()
		cleanDatabase()
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
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.fromString("977350f2-d6a5-49bb-a3a0-773f25f863d9")
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId, deltakerliste.id))
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.randomUUID(),
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller = listOf(
					AnsattRolleDbo(arrangorId, AnsattRolle.KOORDINATOR)
				),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id)),
				veilederDeltakere = emptyList()
			)
		)
		val endringsmelding1 = EndringsmeldingDbo(
			id = UUID.fromString("27446cc8-30ad-4030-94e3-de438c2af3c6"),
			deltakerId = deltakerId,
			type = EndringsmeldingType.AVSLUTT_DELTAKELSE,
			innhold = Innhold.AvsluttDeltakelseInnhold(
				sluttdato = LocalDate.of(2023, 3, 30),
				aarsak = DeltakerStatusAarsak(
					type = DeltakerStatusAarsak.Type.SYK,
					beskrivelse = "har blitt syk"
				)
			)
		)
		val endringsmelding2 = EndringsmeldingDbo(
			id = UUID.fromString("5029689f-3de6-4d97-9cfa-552f75625ef2"),
			deltakerId = deltakerId,
			type = EndringsmeldingType.DELTAKER_ER_AKTUELL,
			innhold = null
		)
		val endringsmelding3 = EndringsmeldingDbo(
			id = UUID.fromString("362c7fdd-04e7-4f43-9e56-0939585856eb"),
			deltakerId = deltakerId,
			type = EndringsmeldingType.ENDRE_SLUTTDATO,
			innhold = Innhold.EndreSluttdatoInnhold(
				sluttdato = LocalDate.of(2023, 5, 3)
			)
		)
		endringsmeldingRepository.insertOrUpdateEndringsmelding(endringsmelding1)
		endringsmeldingRepository.insertOrUpdateEndringsmelding(endringsmelding2)
		endringsmeldingRepository.insertOrUpdateEndringsmelding(endringsmelding3)

		val response = sendRequest(
			method = "GET",
			path = "/tiltaksarrangor/deltaker/$deltakerId/endringsmeldinger",
			headers = mapOf("Authorization" to "Bearer ${getTokenxToken(fnr = personIdent)}")
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
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.fromString("da4c9568-cea2-42e3-95a3-42f6b809ad08")
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId, deltakerliste.id))
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.randomUUID(),
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller = listOf(
					AnsattRolleDbo(arrangorId, AnsattRolle.KOORDINATOR)
				),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id)),
				veilederDeltakere = emptyList()
			)
		)
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
			headers = mapOf("Authorization" to "Bearer ${getTokenxToken(fnr = personIdent)}")
		)

		response.code shouldBe 200

		val endringsmeldinger = endringsmeldingRepository.getEndringsmeldingerForDeltaker(deltakerId)
		endringsmeldinger.size shouldBe 1
		val endringsmelding = endringsmeldinger.first()
		endringsmelding.type shouldBe EndringsmeldingType.AVSLUTT_DELTAKELSE
		(endringsmelding.innhold as Innhold.AvsluttDeltakelseInnhold).sluttdato shouldBe LocalDate.now()
		(endringsmelding.innhold as Innhold.AvsluttDeltakelseInnhold).aarsak.type shouldBe DeltakerStatusAarsak.Type.FATT_JOBB
		(endringsmelding.innhold as Innhold.AvsluttDeltakelseInnhold).aarsak.beskrivelse shouldBe null
	}

	@Test
	fun `opprettEndringsmelding - deltaker er aktuell json request - returnerer 200`() {
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.fromString("da4c9568-cea2-42e3-95a3-42f6b809ad08")
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId, deltakerliste.id))
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.randomUUID(),
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller = listOf(
					AnsattRolleDbo(arrangorId, AnsattRolle.KOORDINATOR)
				),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id)),
				veilederDeltakere = emptyList()
			)
		)
		mockAmtTiltakServer.addDeltakerErAktuellResponse(deltakerId)

		val requestStr = """{"innhold":{"type":"DELTAKER_ER_AKTUELL"}}"""
		val response = sendRequest(
			method = "POST",
			path = "/tiltaksarrangor/deltaker/$deltakerId/endringsmelding",
			body = requestStr.toRequestBody(mediaTypeJson),
			headers = mapOf("Authorization" to "Bearer ${getTokenxToken(fnr = personIdent)}")
		)

		response.code shouldBe 200

		val endringsmeldinger = endringsmeldingRepository.getEndringsmeldingerForDeltaker(deltakerId)
		endringsmeldinger.size shouldBe 1
		val endringsmelding = endringsmeldinger.first()
		endringsmelding.type shouldBe EndringsmeldingType.DELTAKER_ER_AKTUELL
		endringsmelding.innhold shouldBe null
	}

	@Test
	fun `opprettEndringsmelding - autentisert, deltaker er aktuell - returnerer 200`() {
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.fromString("da4c9568-cea2-42e3-95a3-42f6b809ad08")
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId, deltakerliste.id))
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.randomUUID(),
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller = listOf(
					AnsattRolleDbo(arrangorId, AnsattRolle.KOORDINATOR)
				),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id)),
				veilederDeltakere = emptyList()
			)
		)
		mockAmtTiltakServer.addDeltakerErAktuellResponse(deltakerId)
		val requestBody = EndringsmeldingRequest(
			innhold = EndringsmeldingRequest.Innhold.DeltakerErAktuellInnhold()
		)

		val response = sendRequest(
			method = "POST",
			path = "/tiltaksarrangor/deltaker/$deltakerId/endringsmelding",
			body = JsonUtils.objectMapper.writeValueAsString(requestBody).toRequestBody(mediaTypeJson),
			headers = mapOf("Authorization" to "Bearer ${getTokenxToken(fnr = personIdent)}")
		)

		response.code shouldBe 200

		val endringsmeldinger = endringsmeldingRepository.getEndringsmeldingerForDeltaker(deltakerId)
		endringsmeldinger.size shouldBe 1
		val endringsmelding = endringsmeldinger.first()
		endringsmelding.type shouldBe EndringsmeldingType.DELTAKER_ER_AKTUELL
		endringsmelding.innhold shouldBe null
	}

	@Test
	fun `opprettEndringsmelding - autentisert, endre sluttdato - returnerer 200`() {
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.fromString("da4c9568-cea2-42e3-95a3-42f6b809ad08")
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId, deltakerliste.id))
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.randomUUID(),
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller = listOf(
					AnsattRolleDbo(arrangorId, AnsattRolle.KOORDINATOR)
				),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id)),
				veilederDeltakere = emptyList()
			)
		)
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
			headers = mapOf("Authorization" to "Bearer ${getTokenxToken(fnr = personIdent)}")
		)

		response.code shouldBe 200

		val endringsmeldinger = endringsmeldingRepository.getEndringsmeldingerForDeltaker(deltakerId)
		endringsmeldinger.size shouldBe 1
		val endringsmelding = endringsmeldinger.first()
		endringsmelding.type shouldBe EndringsmeldingType.ENDRE_SLUTTDATO
		(endringsmelding.innhold as Innhold.EndreSluttdatoInnhold).sluttdato shouldBe LocalDate.now()
	}

	@Test
	fun `opprettEndringsmelding - autentisert, endre oppstartsdato til null - returnerer 200`() {
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.randomUUID()
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId, deltakerliste.id))
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.randomUUID(),
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller = listOf(
					AnsattRolleDbo(arrangorId, AnsattRolle.KOORDINATOR)
				),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id)),
				veilederDeltakere = emptyList()
			)
		)
		mockAmtTiltakServer.addEndreOppstartsdatoResponse(deltakerId)
		val requestBody = EndringsmeldingRequest(
			innhold = EndringsmeldingRequest.Innhold.EndreOppstartsdatoInnhold(null)
		)

		val response = sendRequest(
			method = "POST",
			path = "/tiltaksarrangor/deltaker/$deltakerId/endringsmelding",
			body = JsonUtils.objectMapper.writeValueAsString(requestBody).toRequestBody(mediaTypeJson),
			headers = mapOf("Authorization" to "Bearer ${getTokenxToken(fnr = personIdent)}")
		)

		response.code shouldBe 200

		val endringsmeldinger = endringsmeldingRepository.getEndringsmeldingerForDeltaker(deltakerId)
		endringsmeldinger.size shouldBe 1
		val endringsmelding = endringsmeldinger.first()
		endringsmelding.type shouldBe EndringsmeldingType.ENDRE_OPPSTARTSDATO
		(endringsmelding.innhold as Innhold.EndreOppstartsdatoInnhold).oppstartsdato shouldBe null
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
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.randomUUID()
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId, deltakerliste.id))
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.randomUUID(),
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller = listOf(
					AnsattRolleDbo(arrangorId, AnsattRolle.KOORDINATOR)
				),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id)),
				veilederDeltakere = emptyList()
			)
		)
		val endringsmeldingId = UUID.fromString("27446cc8-30ad-4030-94e3-de438c2af3c6")
		val endringsmelding = getEndringsmelding(deltakerId).copy(id = endringsmeldingId)
		endringsmeldingRepository.insertOrUpdateEndringsmelding(endringsmelding)
		mockAmtTiltakServer.addTilbakekallEndringsmeldingResponse(endringsmeldingId)

		val response = sendRequest(
			method = "DELETE",
			path = "/tiltaksarrangor/endringsmelding/$endringsmeldingId",
			headers = mapOf("Authorization" to "Bearer ${getTokenxToken(fnr = personIdent)}")
		)

		response.code shouldBe 200
	}
}
