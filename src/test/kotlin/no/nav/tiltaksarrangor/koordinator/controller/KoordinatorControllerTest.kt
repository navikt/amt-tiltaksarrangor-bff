package no.nav.tiltaksarrangor.koordinator.controller

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltaksarrangor.IntegrationTest
import no.nav.tiltaksarrangor.ingest.model.AnsattRolle
import no.nav.tiltaksarrangor.ingest.model.DeltakerlisteStatus
import no.nav.tiltaksarrangor.koordinator.model.LeggTilVeiledereRequest
import no.nav.tiltaksarrangor.koordinator.model.VeilederRequest
import no.nav.tiltaksarrangor.model.StatusType
import no.nav.tiltaksarrangor.model.Veiledertype
import no.nav.tiltaksarrangor.repositories.AnsattRepository
import no.nav.tiltaksarrangor.repositories.ArrangorRepository
import no.nav.tiltaksarrangor.repositories.DeltakerRepository
import no.nav.tiltaksarrangor.repositories.DeltakerlisteRepository
import no.nav.tiltaksarrangor.repositories.model.AnsattDbo
import no.nav.tiltaksarrangor.repositories.model.AnsattRolleDbo
import no.nav.tiltaksarrangor.repositories.model.ArrangorDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerlisteDbo
import no.nav.tiltaksarrangor.repositories.model.KoordinatorDeltakerlisteDbo
import no.nav.tiltaksarrangor.repositories.model.VeilederDeltakerDbo
import no.nav.tiltaksarrangor.testutils.getDeltaker
import no.nav.tiltaksarrangor.utils.JsonUtils
import okhttp3.MediaType.Companion.toMediaType
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
	private val arrangorRepository = ArrangorRepository(template)

	@AfterEach
	internal fun tearDown() {
		mockAmtArrangorServer.resetHttpServer()
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
			_tiltakNavn = "Tiltaksnavnet",
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
			{"veilederFor":{"veilederFor":2,"medveilederFor":3},"koordinatorFor":{"deltakerlister":[{"id":"9987432c-e336-4b3b-b73e-b7c781a0823a","type":"Arbeidsrettet rehabilitering","navn":"Gjennomføring 1","startdato":null,"sluttdato":null,"erKurs":false}]}}
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
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = DeltakerlisteDbo(
			id = UUID.fromString("9987432c-e336-4b3b-b73e-b7c781a0823a"),
			navn = "Gjennomføring 1",
			status = DeltakerlisteStatus.GJENNOMFORES,
			arrangorId = arrangorId,
			_tiltakNavn = "Tiltaksnavnet",
			tiltakType = "ARBFORB",
			startDato = null,
			sluttDato = null,
			erKurs = false
		)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.randomUUID(),
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller = listOf(AnsattRolleDbo(arrangorId, AnsattRolle.KOORDINATOR)),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id)),
				veilederDeltakere = emptyList()
			)
		)
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.fromString("29bf6799-bb56-4a86-857b-99b529b3dfc4"),
				personIdent = UUID.randomUUID().toString(),
				fornavn = "Fornavn1",
				mellomnavn = null,
				etternavn = "Etternavn1",
				roller = listOf(AnsattRolleDbo(arrangorId, AnsattRolle.VEILEDER)),
				deltakerlister = emptyList(),
				veilederDeltakere = emptyList()
			)
		)
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.fromString("e824dbfe-5317-491b-82ed-03b870eed963"),
				personIdent = UUID.randomUUID().toString(),
				fornavn = "Fornavn2",
				mellomnavn = null,
				etternavn = "Etternavn2",
				roller = listOf(AnsattRolleDbo(arrangorId, AnsattRolle.VEILEDER)),
				deltakerlister = emptyList(),
				veilederDeltakere = listOf(VeilederDeltakerDbo(UUID.randomUUID(), Veiledertype.MEDVEILEDER))
			)
		)

		val response = sendRequest(
			method = "GET",
			path = "/tiltaksarrangor/koordinator/${deltakerliste.id}/veiledere",
			headers = mapOf("Authorization" to "Bearer ${getTokenxToken(fnr = personIdent)}")
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
	fun `tildelVeiledereForDeltaker - autentisert, tildeler veiledere - returnerer 200`() {
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = DeltakerlisteDbo(
			id = UUID.randomUUID(),
			navn = "Gjennomføring 1",
			status = DeltakerlisteStatus.GJENNOMFORES,
			arrangorId = arrangorId,
			_tiltakNavn = "Tiltaksnavnet",
			tiltakType = "ARBFORB",
			startDato = null,
			sluttDato = null,
			erKurs = false
		)
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
				roller = listOf(AnsattRolleDbo(arrangorId, AnsattRolle.KOORDINATOR)),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id)),
				veilederDeltakere = emptyList()
			)
		)
		val veileder1Id = UUID.randomUUID()
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = veileder1Id,
				personIdent = UUID.randomUUID().toString(),
				fornavn = "Fornavn1",
				mellomnavn = null,
				etternavn = "Etternavn1",
				roller = listOf(AnsattRolleDbo(arrangorId, AnsattRolle.VEILEDER)),
				deltakerlister = emptyList(),
				veilederDeltakere = emptyList()
			)
		)
		val veileder2Id = UUID.randomUUID()
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = veileder2Id,
				personIdent = UUID.randomUUID().toString(),
				fornavn = "Fornavn2",
				mellomnavn = null,
				etternavn = "Etternavn2",
				roller = listOf(AnsattRolleDbo(arrangorId, AnsattRolle.VEILEDER)),
				deltakerlister = emptyList(),
				veilederDeltakere = emptyList()
			)
		)

		mockAmtArrangorServer.addOppdaterVeilederForDeltakerResponse(deltakerId)
		val requestBody = LeggTilVeiledereRequest(
			listOf(
				VeilederRequest(
					ansattId = veileder1Id,
					erMedveileder = false
				),
				VeilederRequest(
					ansattId = veileder2Id,
					erMedveileder = true
				)
			)
		)

		val response = sendRequest(
			method = "POST",
			path = "/tiltaksarrangor/koordinator/veiledere?deltakerId=$deltakerId",
			body = JsonUtils.objectMapper.writeValueAsString(requestBody).toRequestBody(mediaTypeJson),
			headers = mapOf("Authorization" to "Bearer ${getTokenxToken(fnr = personIdent)}")
		)

		response.code shouldBe 200

		val veileder1 = ansattRepository.getAnsatt(veileder1Id)
		veileder1?.veilederDeltakere?.size shouldBe 1
		veileder1?.veilederDeltakere?.find { it.deltakerId == deltakerId && it.veilederType == Veiledertype.VEILEDER } shouldNotBe null

		val veileder2 = ansattRepository.getAnsatt(veileder2Id)
		veileder2?.veilederDeltakere?.size shouldBe 1
		veileder2?.veilederDeltakere?.find { it.deltakerId == deltakerId && it.veilederType == Veiledertype.MEDVEILEDER } shouldNotBe null
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
		val personIdent = "12345678910"
		val deltakerlisteId = UUID.fromString("9987432c-e336-4b3b-b73e-b7c781a0823a")
		val arrangorId = UUID.randomUUID()
		arrangorRepository.insertOrUpdateArrangor(
			ArrangorDbo(
				id = arrangorId,
				navn = "Arrangør AS",
				organisasjonsnummer = "88888888",
				overordnetArrangorId = null
			)
		)
		val deltakerliste = DeltakerlisteDbo(
			id = deltakerlisteId,
			navn = "Gjennomføring 1",
			status = DeltakerlisteStatus.GJENNOMFORES,
			arrangorId = arrangorId,
			_tiltakNavn = "Navn på tiltak",
			tiltakType = "ARBFORB",
			startDato = LocalDate.of(2023, 2, 1),
			sluttDato = null,
			erKurs = false
		)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.randomUUID(),
				personIdent = personIdent,
				fornavn = "Fornavn1",
				mellomnavn = null,
				etternavn = "Etternavn1",
				roller = listOf(AnsattRolleDbo(arrangorId, AnsattRolle.KOORDINATOR)),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerlisteId)),
				veilederDeltakere = emptyList()
			)
		)
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.randomUUID(),
				personIdent = UUID.randomUUID().toString(),
				fornavn = "Fornavn2",
				mellomnavn = null,
				etternavn = "Etternavn2",
				roller = listOf(AnsattRolleDbo(arrangorId, AnsattRolle.KOORDINATOR)),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerlisteId)),
				veilederDeltakere = emptyList()
			)
		)
		val deltaker = DeltakerDbo(
			id = UUID.fromString("252428ac-37a6-4341-bb17-5bad412c9409"),
			deltakerlisteId = deltakerlisteId,
			personident = "10987654321",
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
			startdato = LocalDate.of(2023, 2, 1),
			sluttdato = null,
			innsoktDato = LocalDate.of(2023, 1, 15),
			bestillingstekst = "tekst",
			navKontor = "NAV Testheim",
			navVeilederId = null,
			navVeilederEpost = null,
			navVeilederNavn = null,
			navVeilederTelefon = null,
			skjultAvAnsattId = null,
			skjultDato = null
		)
		deltakerRepository.insertOrUpdateDeltaker(deltaker)

		val response = sendRequest(
			method = "GET",
			path = "/tiltaksarrangor/koordinator/deltakerliste/$deltakerlisteId",
			headers = mapOf("Authorization" to "Bearer ${getTokenxToken(fnr = personIdent)}")
		)

		val expectedJson = """
			{"id":"9987432c-e336-4b3b-b73e-b7c781a0823a","navn":"Gjennomføring 1","tiltaksnavn":"Arbeidsrettet rehabilitering","arrangorNavn":"Arrangør AS","startDato":"2023-02-01","sluttDato":null,"status":"GJENNOMFORES","koordinatorer":[{"fornavn":"Fornavn1","mellomnavn":null,"etternavn":"Etternavn1"},{"fornavn":"Fornavn2","mellomnavn":null,"etternavn":"Etternavn2"}],"deltakere":[{"id":"252428ac-37a6-4341-bb17-5bad412c9409","fornavn":"Fornavn","mellomnavn":null,"etternavn":"Etternavn","fodselsnummer":"10987654321","soktInnDato":"2023-01-15T00:00:00","startDato":"2023-02-01","sluttDato":null,"status":{"type":"DELTAR","endretDato":"2023-02-01T00:00:00"},"veiledere":[],"navKontor":"NAV Testheim","aktiveEndringsmeldinger":[]}],"erKurs":false}
		""".trimIndent()
		response.code shouldBe 200
		response.body?.string() shouldBe expectedJson
	}
}
