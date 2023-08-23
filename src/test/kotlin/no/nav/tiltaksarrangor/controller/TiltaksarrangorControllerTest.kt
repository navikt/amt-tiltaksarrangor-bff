package no.nav.tiltaksarrangor.controller

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltaksarrangor.IntegrationTest
import no.nav.tiltaksarrangor.ingest.model.AnsattRolle
import no.nav.tiltaksarrangor.ingest.model.EndringsmeldingType
import no.nav.tiltaksarrangor.ingest.model.Innhold
import no.nav.tiltaksarrangor.model.DeltakerStatusAarsak
import no.nav.tiltaksarrangor.model.StatusType
import no.nav.tiltaksarrangor.model.Veiledertype
import no.nav.tiltaksarrangor.repositories.AnsattRepository
import no.nav.tiltaksarrangor.repositories.ArrangorRepository
import no.nav.tiltaksarrangor.repositories.DeltakerRepository
import no.nav.tiltaksarrangor.repositories.DeltakerlisteRepository
import no.nav.tiltaksarrangor.repositories.EndringsmeldingRepository
import no.nav.tiltaksarrangor.repositories.model.AnsattDbo
import no.nav.tiltaksarrangor.repositories.model.AnsattRolleDbo
import no.nav.tiltaksarrangor.repositories.model.ArrangorDbo
import no.nav.tiltaksarrangor.repositories.model.EndringsmeldingDbo
import no.nav.tiltaksarrangor.repositories.model.VeilederDeltakerDbo
import no.nav.tiltaksarrangor.testutils.getDeltaker
import no.nav.tiltaksarrangor.testutils.getDeltakerliste
import no.nav.tiltaksarrangor.testutils.getVurderinger
import no.nav.tiltaksarrangor.utils.JsonUtils.objectMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class TiltaksarrangorControllerTest : IntegrationTest() {
	private val template = NamedParameterJdbcTemplate(postgresDataSource)
	private val ansattRepository = AnsattRepository(template)
	private val deltakerRepository = DeltakerRepository(template)
	private val deltakerlisteRepository = DeltakerlisteRepository(template, deltakerRepository)
	private val endringsmeldingRepository = EndringsmeldingRepository(template)
	private val arrangorRepository = ArrangorRepository(template)

	@AfterEach
	internal fun tearDown() {
		mockAmtTiltakServer.resetHttpServer()
		mockAmtArrangorServer.resetHttpServer()
		cleanDatabase()
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
		val personIdent = "12345678910"
		mockAmtArrangorServer.addAnsattResponse(personIdent = personIdent)

		val response = sendRequest(
			method = "GET",
			path = "/tiltaksarrangor/meg/roller",
			headers = mapOf("Authorization" to "Bearer ${getTokenxToken(fnr = personIdent)}")
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
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		arrangorRepository.insertOrUpdateArrangor(
			ArrangorDbo(
				id = arrangorId,
				navn = "Orgnavn",
				organisasjonsnummer = "orgnummer",
				overordnetArrangorId = null
			)
		)
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
					AnsattRolleDbo(UUID.randomUUID(), AnsattRolle.KOORDINATOR)
				),
				deltakerlister = emptyList(),
				veilederDeltakere = emptyList()
			)
		)

		val response = sendRequest(
			method = "GET",
			path = "/tiltaksarrangor/deltaker/$deltakerId",
			headers = mapOf("Authorization" to "Bearer ${getTokenxToken(fnr = personIdent)}")
		)

		response.code shouldBe 403
	}

	@Test
	fun `getDeltaker - autentisert, har tilgang - returnerer 200`() {
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		arrangorRepository.insertOrUpdateArrangor(
			ArrangorDbo(
				id = arrangorId,
				navn = "Orgnavn",
				organisasjonsnummer = "orgnummer",
				overordnetArrangorId = null
			)
		)
		val deltakerliste = getDeltakerliste(arrangorId).copy(
			id = UUID.fromString("9987432c-e336-4b3b-b73e-b7c781a0823a"),
			startDato = LocalDate.of(2023, 2, 1)
		)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.fromString("977350f2-d6a5-49bb-a3a0-773f25f863d9")
		val gyldigFra = LocalDateTime.now()
		val deltaker = getDeltaker(deltakerId, deltakerliste.id).copy(
			personident = "10987654321",
			telefonnummer = "90909090",
			epost = "mail@test.no",
			status = StatusType.DELTAR,
			statusOpprettetDato = LocalDate.of(2023, 2, 1).atStartOfDay(),
			startdato = LocalDate.of(2023, 2, 1),
			dagerPerUke = 2.5f,
			innsoktDato = LocalDate.of(2023, 1, 15),
			bestillingstekst = "Tror deltakeren vil ha nytte av dette",
			navKontor = "Nav Oslo",
			navVeilederId = UUID.randomUUID(),
			navVeilederNavn = "Veileder Veiledersen",
			navVeilederTelefon = "56565656",
			navVeilederEpost = "epost@nav.no",
			vurderingerFraArrangor = getVurderinger(deltakerId, gyldigFra)
		)
		deltakerRepository.insertOrUpdateDeltaker(deltaker)
		val endringsmeldinger = getAktiveEndringsmeldinger(deltakerId)
		endringsmeldinger.forEach { endringsmeldingRepository.insertOrUpdateEndringsmelding(it) }
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.fromString("2d5fc2f7-a9e6-4830-a987-4ff135a70c10"),
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller = listOf(
					AnsattRolleDbo(arrangorId, AnsattRolle.VEILEDER)
				),
				deltakerlister = emptyList(),
				veilederDeltakere = listOf(VeilederDeltakerDbo(deltakerId, Veiledertype.VEILEDER))
			)
		)
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.fromString("7c43b43b-43be-4d4b-8057-d907c5f1e5c5"),
				personIdent = UUID.randomUUID().toString(),
				fornavn = "Per",
				mellomnavn = null,
				etternavn = "Person",
				roller = listOf(
					AnsattRolleDbo(arrangorId, AnsattRolle.VEILEDER)
				),
				deltakerlister = emptyList(),
				veilederDeltakere = listOf(VeilederDeltakerDbo(deltakerId, Veiledertype.MEDVEILEDER))
			)
		)

		val response = sendRequest(
			method = "GET",
			path = "/tiltaksarrangor/deltaker/$deltakerId",
			headers = mapOf("Authorization" to "Bearer ${getTokenxToken(fnr = personIdent)}")
		)

		val expectedJson = """
			{"id":"977350f2-d6a5-49bb-a3a0-773f25f863d9","deltakerliste":{"id":"9987432c-e336-4b3b-b73e-b7c781a0823a","startDato":"2023-02-01","sluttDato":null,"erKurs":false},"fornavn":"Fornavn","mellomnavn":null,"etternavn":"Etternavn","fodselsnummer":"10987654321","telefonnummer":"90909090","epost":"mail@test.no","status":{"type":"DELTAR","endretDato":"2023-02-01T00:00:00"},"startDato":"2023-02-01","sluttDato":null,"deltakelseProsent":null,"dagerPerUke":2.5,"soktInnPa":"Gjennomføring 1","soktInnDato":"2023-01-15T00:00:00","tiltakskode":"ARBFORB","bestillingTekst":"Tror deltakeren vil ha nytte av dette","fjernesDato":null,"navInformasjon":{"navkontor":"Nav Oslo","navVeileder":{"navn":"Veileder Veiledersen","epost":"epost@nav.no","telefon":"56565656"}},"veiledere":[{"ansattId":"2d5fc2f7-a9e6-4830-a987-4ff135a70c10","deltakerId":"977350f2-d6a5-49bb-a3a0-773f25f863d9","veiledertype":"VEILEDER","fornavn":"Fornavn","mellomnavn":null,"etternavn":"Etternavn"},{"ansattId":"7c43b43b-43be-4d4b-8057-d907c5f1e5c5","deltakerId":"977350f2-d6a5-49bb-a3a0-773f25f863d9","veiledertype":"MEDVEILEDER","fornavn":"Per","mellomnavn":null,"etternavn":"Person"}],"aktiveEndringsmeldinger":[{"id":"27446cc8-30ad-4030-94e3-de438c2af3c6","innhold":{"sluttdato":"2023-03-30","aarsak":{"type":"SYK","beskrivelse":"har blitt syk"}},"type":"AVSLUTT_DELTAKELSE"},{"id":"5029689f-3de6-4d97-9cfa-552f75625ef2","innhold":null,"type":"DELTAKER_ER_AKTUELL"},{"id":"362c7fdd-04e7-4f43-9e56-0939585856eb","innhold":{"sluttdato":"2023-05-03"},"type":"ENDRE_SLUTTDATO"}],"adresse":{"adressetype":"KONTAKTADRESSE","postnummer":"1234","poststed":"MOSS","tilleggsnavn":null,"adressenavn":"Gate 1"},"gjeldendeVurderingFraArrangor":{"vurderingstype":"OPPFYLLER_IKKE_KRAVENE","begrunnelse":"Mangler førerkort","gyldigFra":${objectMapper.writeValueAsString(gyldigFra)},"gyldigTil":null},"historiskeVurderingerFraArrangor":[]}
		""".trimIndent()
		response.code shouldBe 200
		response.body?.string() shouldBe expectedJson
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
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		arrangorRepository.insertOrUpdateArrangor(
			ArrangorDbo(
				id = arrangorId,
				navn = "Orgnavn",
				organisasjonsnummer = "orgnummer",
				overordnetArrangorId = null
			)
		)
		val deltakerliste = getDeltakerliste(arrangorId).copy(
			id = UUID.fromString("9987432c-e336-4b3b-b73e-b7c781a0823a"),
			startDato = LocalDate.of(2023, 2, 1)
		)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltaker = getDeltaker(deltakerId, deltakerliste.id).copy(
			personident = "10987654321",
			status = StatusType.HAR_SLUTTET,
			statusOpprettetDato = LocalDate.of(2023, 2, 1).atStartOfDay()
		)
		deltakerRepository.insertOrUpdateDeltaker(deltaker)
		val ansattId = UUID.randomUUID()
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = ansattId,
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller = listOf(
					AnsattRolleDbo(arrangorId, AnsattRolle.VEILEDER)
				),
				deltakerlister = emptyList(),
				veilederDeltakere = listOf(VeilederDeltakerDbo(deltakerId, Veiledertype.VEILEDER))
			)
		)
		mockAmtTiltakServer.addSkjulDeltakerResponse(deltakerId)

		val response = sendRequest(
			method = "DELETE",
			path = "/tiltaksarrangor/deltaker/$deltakerId",
			headers = mapOf("Authorization" to "Bearer ${getTokenxToken(fnr = personIdent)}")
		)

		response.code shouldBe 200
		val deltakerFraDb = deltakerRepository.getDeltaker(deltakerId)
		deltakerFraDb?.skjultAvAnsattId shouldBe ansattId
		deltakerFraDb?.skjultDato shouldNotBe null
	}

	private fun getAktiveEndringsmeldinger(deltakerId: UUID): List<EndringsmeldingDbo> {
		return listOf(
			EndringsmeldingDbo(
				id = UUID.fromString("27446cc8-30ad-4030-94e3-de438c2af3c6"),
				deltakerId = deltakerId,
				innhold = Innhold.AvsluttDeltakelseInnhold(
					sluttdato = LocalDate.of(2023, 3, 30),
					aarsak = DeltakerStatusAarsak(
						type = DeltakerStatusAarsak.Type.SYK,
						beskrivelse = "har blitt syk"
					)
				),
				type = EndringsmeldingType.AVSLUTT_DELTAKELSE
			),
			EndringsmeldingDbo(
				id = UUID.fromString("5029689f-3de6-4d97-9cfa-552f75625ef2"),
				deltakerId = deltakerId,
				innhold = Innhold.DeltakerErAktuellInnhold(),
				type = EndringsmeldingType.DELTAKER_ER_AKTUELL
			),
			EndringsmeldingDbo(
				id = UUID.fromString("362c7fdd-04e7-4f43-9e56-0939585856eb"),
				deltakerId = deltakerId,
				innhold = Innhold.EndreSluttdatoInnhold(
					sluttdato = LocalDate.of(2023, 5, 3)
				),
				type = EndringsmeldingType.ENDRE_SLUTTDATO
			)
		)
	}
}
