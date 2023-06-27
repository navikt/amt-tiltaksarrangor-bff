package no.nav.tiltaksarrangor.veileder.controller

import io.kotest.matchers.shouldBe
import no.nav.tiltaksarrangor.IntegrationTest
import no.nav.tiltaksarrangor.ingest.model.AnsattRolle
import no.nav.tiltaksarrangor.ingest.model.DeltakerlisteStatus
import no.nav.tiltaksarrangor.model.StatusType
import no.nav.tiltaksarrangor.model.Veiledertype
import no.nav.tiltaksarrangor.repositories.AnsattRepository
import no.nav.tiltaksarrangor.repositories.DeltakerRepository
import no.nav.tiltaksarrangor.repositories.DeltakerlisteRepository
import no.nav.tiltaksarrangor.repositories.model.AnsattDbo
import no.nav.tiltaksarrangor.repositories.model.AnsattRolleDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerlisteDbo
import no.nav.tiltaksarrangor.repositories.model.VeilederDeltakerDbo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class VeilederControllerTest : IntegrationTest() {
	private val template = NamedParameterJdbcTemplate(postgresDataSource)
	private val ansattRepository = AnsattRepository(template)
	private val deltakerRepository = DeltakerRepository(template)
	private val deltakerlisteRepository = DeltakerlisteRepository(template, deltakerRepository)

	@AfterEach
	internal fun tearDown() {
		cleanDatabase()
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
		val deltakerId = UUID.fromString("977350f2-d6a5-49bb-a3a0-773f25f863d9")
		val deltaker = DeltakerDbo(
			id = deltakerId,
			deltakerlisteId = deltakerliste.id,
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
		deltakerRepository.insertOrUpdateDeltaker(deltaker)
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.randomUUID(),
				personIdent = personIdent,
				fornavn = "Ansatt",
				mellomnavn = null,
				etternavn = "Ansattsen",
				roller = listOf(AnsattRolleDbo(arrangorId, AnsattRolle.VEILEDER)),
				deltakerlister = emptyList(),
				veilederDeltakere = listOf(
					VeilederDeltakerDbo(deltakerId, Veiledertype.VEILEDER)
				)
			)
		)

		val response = sendRequest(
			method = "GET",
			path = "/tiltaksarrangor/veileder/mine-deltakere",
			headers = mapOf("Authorization" to "Bearer ${getTokenxToken(fnr = personIdent)}")
		)

		val expectedJson = """
			[{"id":"977350f2-d6a5-49bb-a3a0-773f25f863d9","fornavn":"Fornavn","mellomnavn":null,"etternavn":"Etternavn","fodselsnummer":"10987654321","startDato":"2023-02-15","sluttDato":null,"status":{"type":"DELTAR","endretDato":"2023-02-01T00:00:00"},"deltakerliste":{"id":"9987432c-e336-4b3b-b73e-b7c781a0823a","type":"Tiltaksnavnet","navn":"Gjennomføring 1"},"veiledertype":"VEILEDER","aktiveEndringsmeldinger":[]}]
		""".trimIndent()
		response.code shouldBe 200
		response.body?.string() shouldBe expectedJson
	}
}
