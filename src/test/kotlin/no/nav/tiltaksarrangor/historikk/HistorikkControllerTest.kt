package no.nav.tiltaksarrangor.historikk

import io.kotest.matchers.shouldBe
import no.nav.tiltaksarrangor.IntegrationTest
import no.nav.tiltaksarrangor.ingest.model.AnsattRolle
import no.nav.tiltaksarrangor.model.StatusType
import no.nav.tiltaksarrangor.model.Veiledertype
import no.nav.tiltaksarrangor.repositories.AnsattRepository
import no.nav.tiltaksarrangor.repositories.ArrangorRepository
import no.nav.tiltaksarrangor.repositories.DeltakerRepository
import no.nav.tiltaksarrangor.repositories.DeltakerlisteRepository
import no.nav.tiltaksarrangor.repositories.model.AnsattDbo
import no.nav.tiltaksarrangor.repositories.model.AnsattRolleDbo
import no.nav.tiltaksarrangor.repositories.model.ArrangorDbo
import no.nav.tiltaksarrangor.repositories.model.VeilederDeltakerDbo
import no.nav.tiltaksarrangor.testutils.getDeltaker
import no.nav.tiltaksarrangor.testutils.getDeltakerliste
import no.nav.tiltaksarrangor.testutils.getVurderinger
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class HistorikkControllerTest : IntegrationTest() {
	private val template = NamedParameterJdbcTemplate(postgresDataSource)
	private val deltakerRepository = DeltakerRepository(template)
	private val deltakerlisteRepository = DeltakerlisteRepository(template, deltakerRepository)
	private val ansattRepository = AnsattRepository(template)
	private val arrangorRepository = ArrangorRepository(template)

	@AfterEach
	internal fun tearDown() {
		mockAmtArrangorServer.resetHttpServer()
		cleanDatabase()
	}

	@Test
	fun `getHistorikk - ikke autentisert - returnerer 401`() {
		val response =
			sendRequest(
				method = "GET",
				path = "/tiltaksarrangor/deltaker/${UUID.randomUUID()}/historikk",
			)

		response.code shouldBe 401
	}

	@Test
	fun `getDeltakerHistorikk - har tilgang, deltaker finnes - returnerer historikk`() {
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		arrangorRepository.insertOrUpdateArrangor(
			ArrangorDbo(
				id = arrangorId,
				navn = "Orgnavn",
				organisasjonsnummer = "orgnummer",
				overordnetArrangorId = null,
			),
		)
		val deltakerliste =
			getDeltakerliste(arrangorId).copy(
				id = UUID.fromString("9987432c-e336-4b3b-b73e-b7c781a0823a"),
				startDato = LocalDate.of(2023, 2, 1),
			)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.randomUUID()
		val gyldigFra = LocalDateTime.now()
		val deltaker =
			getDeltaker(deltakerId, deltakerliste.id).copy(
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
				vurderingerFraArrangor = getVurderinger(deltakerId, gyldigFra),
			)
		deltakerRepository.insertOrUpdateDeltaker(deltaker)

		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.fromString("2d5fc2f7-a9e6-4830-a987-4ff135a70c10"),
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller =
					listOf(
						AnsattRolleDbo(arrangorId, AnsattRolle.VEILEDER),
					),
				deltakerlister = emptyList(),
				veilederDeltakere = listOf(VeilederDeltakerDbo(deltakerId, Veiledertype.VEILEDER)),
			),
		)

		val response =
			sendRequest(
				method = "GET",
				path = "/tiltaksarrangor/deltaker/$deltakerId/historikk",
				headers = mapOf("Authorization" to "Bearer ${getTokenxToken(fnr = personIdent)}"),
			)
		val expectedJson =
			"""
			[]
			""".trimIndent().format()
		response.code shouldBe 200
		response.body?.string() shouldBe expectedJson
	}
}
