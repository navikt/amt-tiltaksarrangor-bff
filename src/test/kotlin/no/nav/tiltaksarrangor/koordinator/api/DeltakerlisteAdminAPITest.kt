package no.nav.tiltaksarrangor.koordinator.api

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltaksarrangor.IntegrationTest
import no.nav.tiltaksarrangor.consumer.model.AnsattRolle
import no.nav.tiltaksarrangor.model.DeltakerlisteStatus
import no.nav.tiltaksarrangor.repositories.AnsattRepository
import no.nav.tiltaksarrangor.repositories.ArrangorRepository
import no.nav.tiltaksarrangor.repositories.DeltakerRepository
import no.nav.tiltaksarrangor.repositories.DeltakerlisteRepository
import no.nav.tiltaksarrangor.repositories.model.AnsattDbo
import no.nav.tiltaksarrangor.repositories.model.AnsattRolleDbo
import no.nav.tiltaksarrangor.repositories.model.ArrangorDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerlisteDbo
import no.nav.tiltaksarrangor.repositories.model.KoordinatorDeltakerlisteDbo
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDate
import java.util.UUID

class DeltakerlisteAdminAPITest : IntegrationTest() {
	private val template = NamedParameterJdbcTemplate(postgresDataSource)
	private val ansattRepository = AnsattRepository(template)
	private val arrangorRepository = ArrangorRepository(template)
	private val deltakerRepository = DeltakerRepository(template)
	private val deltakerlisteRepository = DeltakerlisteRepository(template, deltakerRepository)

	@AfterEach
	internal fun tearDown() {
		cleanDatabase()
	}

	@Test
	fun `getAlleDeltakerlister - ikke autentisert - returnerer 401`() {
		val response =
			sendRequest(
				method = "GET",
				path = "/tiltaksarrangor/koordinator/admin/deltakerlister",
			)

		response.code shouldBe 401
	}

	@Test
	fun `getAlleDeltakerlister - autentisert - returnerer 200`() {
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		arrangorRepository.insertOrUpdateArrangor(
			ArrangorDbo(
				id = arrangorId,
				navn = "Arrangør AS",
				organisasjonsnummer = "88888888",
				overordnetArrangorId = null,
			),
		)
		val deltakerliste1 =
			DeltakerlisteDbo(
				id = UUID.fromString("9987432c-e336-4b3b-b73e-b7c781a0823a"),
				navn = "Gjennomføring 1",
				status = DeltakerlisteStatus.GJENNOMFORES,
				arrangorId = arrangorId,
				tiltakNavn = "Navn på tiltak",
				tiltakType = "ARBFORB",
				startDato = LocalDate.of(2023, 2, 1),
				sluttDato = null,
				erKurs = false,
				tilgjengeligForArrangorFraOgMedDato = null,
			)
		val deltakerliste2 =
			DeltakerlisteDbo(
				id = UUID.fromString("fd70758a-44c5-4868-bdcb-b1ddd26cb5e9"),
				navn = "Gjennomføring 2",
				status = DeltakerlisteStatus.GJENNOMFORES,
				arrangorId = arrangorId,
				tiltakNavn = "Annet tiltak",
				tiltakType = "INDOPPFAG",
				startDato = LocalDate.of(2023, 5, 1),
				sluttDato = LocalDate.of(2023, 6, 1),
				erKurs = false,
				tilgjengeligForArrangorFraOgMedDato = LocalDate.now().minusYears(1),
			)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste1)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste2)
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.randomUUID(),
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller = listOf(AnsattRolleDbo(arrangorId, AnsattRolle.KOORDINATOR)),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste1.id)),
				veilederDeltakere = emptyList(),
			),
		)

		val response =
			sendRequest(
				method = "GET",
				path = "/tiltaksarrangor/koordinator/admin/deltakerlister",
				headers = mapOf("Authorization" to "Bearer ${getTokenxToken(fnr = personIdent)}"),
			)

		val expectedJson =
			"""
			[{"id":"9987432c-e336-4b3b-b73e-b7c781a0823a","navn":"Gjennomføring 1","tiltaksnavn":"Navn på tiltak","arrangorNavn":"Arrangør AS","arrangorOrgnummer":"88888888","arrangorParentNavn":"Arrangør AS","startDato":"2023-02-01","sluttDato":null,"lagtTil":true},{"id":"fd70758a-44c5-4868-bdcb-b1ddd26cb5e9","navn":"Gjennomføring 2","tiltaksnavn":"Annet tiltak","arrangorNavn":"Arrangør AS","arrangorOrgnummer":"88888888","arrangorParentNavn":"Arrangør AS","startDato":"2023-05-01","sluttDato":"2023-06-01","lagtTil":false}]
			""".trimIndent()
		response.code shouldBe 200
		response.body?.string() shouldBe expectedJson
	}

	@Test
	fun `leggTilDeltakerliste - ikke autentisert - returnerer 401`() {
		val response =
			sendRequest(
				method = "POST",
				path = "/tiltaksarrangor/koordinator/admin/deltakerliste/${UUID.randomUUID()}",
				body = emptyRequest(),
			)

		response.code shouldBe 401
	}

	@Test
	fun `leggTilDeltakerliste - autentisert - returnerer 200`() {
		val personIdent = "12345678910"
		val deltakerlisteId = UUID.fromString("9987432c-e336-4b3b-b73e-b7c781a0823a")
		val arrangorId = UUID.randomUUID()
		val deltakerliste =
			DeltakerlisteDbo(
				id = deltakerlisteId,
				navn = "Gjennomføring 1",
				status = DeltakerlisteStatus.GJENNOMFORES,
				arrangorId = arrangorId,
				tiltakNavn = "Navn på tiltak",
				tiltakType = "ARBFORB",
				startDato = LocalDate.of(2023, 2, 1),
				sluttDato = null,
				erKurs = false,
				tilgjengeligForArrangorFraOgMedDato = null,
			)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val ansattId = UUID.randomUUID()
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = ansattId,
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller = listOf(AnsattRolleDbo(arrangorId, AnsattRolle.KOORDINATOR)),
				deltakerlister = emptyList(),
				veilederDeltakere = emptyList(),
			),
		)
		mockAmtArrangorServer.addLeggTilEllerFjernDeltakerlisteResponse(arrangorId, deltakerlisteId)

		val response =
			sendRequest(
				method = "POST",
				path = "/tiltaksarrangor/koordinator/admin/deltakerliste/$deltakerlisteId",
				body = emptyRequest(),
				headers = mapOf("Authorization" to "Bearer ${getTokenxToken(fnr = personIdent)}"),
			)

		response.code shouldBe 200

		val ansattFraDb = ansattRepository.getAnsatt(ansattId)
		ansattFraDb?.deltakerlister?.size shouldBe 1
		ansattFraDb?.deltakerlister?.find { it.deltakerlisteId == deltakerlisteId } shouldNotBe null
	}

	@Test
	fun `fjernDeltakerliste - ikke autentisert - returnerer 401`() {
		val response =
			sendRequest(
				method = "DELETE",
				path = "/tiltaksarrangor/koordinator/admin/deltakerliste/${UUID.randomUUID()}",
			)

		response.code shouldBe 401
	}

	@Test
	fun `fjernDeltakerliste - autentisert - returnerer 200`() {
		val personIdent = "12345678910"
		val deltakerlisteId = UUID.fromString("9987432c-e336-4b3b-b73e-b7c781a0823a")
		val arrangorId = UUID.randomUUID()
		val deltakerliste =
			DeltakerlisteDbo(
				id = deltakerlisteId,
				navn = "Gjennomføring 1",
				status = DeltakerlisteStatus.GJENNOMFORES,
				arrangorId = arrangorId,
				tiltakNavn = "Navn på tiltak",
				tiltakType = "ARBFORB",
				startDato = LocalDate.of(2023, 2, 1),
				sluttDato = null,
				erKurs = false,
				tilgjengeligForArrangorFraOgMedDato = null,
			)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val ansattId = UUID.randomUUID()
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = ansattId,
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller = listOf(AnsattRolleDbo(arrangorId, AnsattRolle.KOORDINATOR)),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerlisteId)),
				veilederDeltakere = emptyList(),
			),
		)
		mockAmtArrangorServer.addLeggTilEllerFjernDeltakerlisteResponse(arrangorId, deltakerlisteId)

		val response =
			sendRequest(
				method = "DELETE",
				path = "/tiltaksarrangor/koordinator/admin/deltakerliste/$deltakerlisteId",
				headers = mapOf("Authorization" to "Bearer ${getTokenxToken(fnr = personIdent)}"),
			)

		response.code shouldBe 200

		val ansattFraDb = ansattRepository.getAnsatt(ansattId)
		ansattFraDb?.deltakerlister?.size shouldBe 0
	}
}

private fun emptyRequest(): RequestBody {
	val mediaTypeHtml = "text/html".toMediaType()
	return "".toRequestBody(mediaTypeHtml)
}
