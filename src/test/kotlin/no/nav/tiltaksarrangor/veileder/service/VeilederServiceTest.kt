package no.nav.tiltaksarrangor.veileder.service

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearMocks
import io.mockk.mockk
import no.nav.tiltaksarrangor.client.amtarrangor.AmtArrangorClient
import no.nav.tiltaksarrangor.ingest.model.AnsattRolle
import no.nav.tiltaksarrangor.ingest.model.EndringsmeldingType
import no.nav.tiltaksarrangor.ingest.model.Innhold
import no.nav.tiltaksarrangor.model.Veiledertype
import no.nav.tiltaksarrangor.model.exceptions.UnauthorizedException
import no.nav.tiltaksarrangor.repositories.AnsattRepository
import no.nav.tiltaksarrangor.repositories.DeltakerRepository
import no.nav.tiltaksarrangor.repositories.DeltakerlisteRepository
import no.nav.tiltaksarrangor.repositories.EndringsmeldingRepository
import no.nav.tiltaksarrangor.repositories.model.AnsattDbo
import no.nav.tiltaksarrangor.repositories.model.AnsattRolleDbo
import no.nav.tiltaksarrangor.repositories.model.VeilederDeltakerDbo
import no.nav.tiltaksarrangor.service.AnsattService
import no.nav.tiltaksarrangor.testutils.DbTestDataUtils
import no.nav.tiltaksarrangor.testutils.SingletonPostgresContainer
import no.nav.tiltaksarrangor.testutils.getDeltaker
import no.nav.tiltaksarrangor.testutils.getDeltakerliste
import no.nav.tiltaksarrangor.testutils.getEndringsmelding
import no.nav.tiltaksarrangor.unleash.UnleashService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class VeilederServiceTest {
	private val amtArrangorClient = mockk<AmtArrangorClient>()
	private val unleashService = mockk<UnleashService>()
	private val dataSource = SingletonPostgresContainer.getDataSource()
	private val template = NamedParameterJdbcTemplate(dataSource)
	private val ansattRepository = AnsattRepository(template)
	private val ansattService = AnsattService(amtArrangorClient, ansattRepository)
	private val deltakerRepository = DeltakerRepository(template)
	private val deltakerlisteRepository = DeltakerlisteRepository(template, deltakerRepository)
	private val endringsmeldingRepository = EndringsmeldingRepository(template)
	private val veilederService = VeilederService(ansattService, deltakerRepository, endringsmeldingRepository, unleashService)

	@AfterEach
	internal fun tearDown() {
		DbTestDataUtils.cleanDatabase(dataSource)
		clearMocks(amtArrangorClient)
	}

	@Test
	fun `getMineDeltakere - ansatt har ikke veileder-rolle - returnerer unauthorized`() {
		val personIdent = "12345678910"
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.randomUUID(),
				personIdent = personIdent,
				fornavn = "Ansatt",
				mellomnavn = null,
				etternavn = "Ansattsen",
				roller = listOf(AnsattRolleDbo(UUID.randomUUID(), AnsattRolle.KOORDINATOR)),
				deltakerlister = emptyList(),
				veilederDeltakere = listOf(VeilederDeltakerDbo(UUID.randomUUID(), Veiledertype.VEILEDER))
			)
		)

		assertThrows<UnauthorizedException> {
			veilederService.getMineDeltakere(personIdent)
		}
	}

	@Test
	fun `getMineDeltakere - ansatt er veileder for tre deltakere hos to arrangorer - returnerer riktige deltakere med endringsmeldinger`() {
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val arrangorId2 = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		val deltakerliste2 = deltakerliste.copy(id = UUID.randomUUID(), arrangorId = arrangorId2, navn = "Deltakerliste 2")
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste2)
		val deltaker = getDeltaker(UUID.randomUUID(), deltakerliste.id).copy(personident = "12345")
		val deltaker2 = getDeltaker(UUID.randomUUID(), deltakerliste.id).copy(personident = "23456")
		val deltaker3 = getDeltaker(UUID.randomUUID(), deltakerliste2.id).copy(personident = "34567")
		deltakerRepository.insertOrUpdateDeltaker(deltaker)
		deltakerRepository.insertOrUpdateDeltaker(deltaker2)
		deltakerRepository.insertOrUpdateDeltaker(deltaker3)
		val endringsmelding = getEndringsmelding(deltaker.id)
		val endringsmelding2 = endringsmelding.copy(
			id = UUID.randomUUID(),
			type = EndringsmeldingType.ENDRE_OPPSTARTSDATO,
			innhold = Innhold.EndreOppstartsdatoInnhold(
				LocalDate.now().minusMonths(2)
			)
		)
		endringsmeldingRepository.insertOrUpdateEndringsmelding(endringsmelding)
		endringsmeldingRepository.insertOrUpdateEndringsmelding(endringsmelding2)
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.randomUUID(),
				personIdent = personIdent,
				fornavn = "Ansatt",
				mellomnavn = null,
				etternavn = "Ansattsen",
				roller = listOf(AnsattRolleDbo(arrangorId, AnsattRolle.VEILEDER), AnsattRolleDbo(arrangorId2, AnsattRolle.VEILEDER)),
				deltakerlister = emptyList(),
				veilederDeltakere = listOf(
					VeilederDeltakerDbo(deltaker.id, Veiledertype.VEILEDER),
					VeilederDeltakerDbo(deltaker2.id, Veiledertype.MEDVEILEDER),
					VeilederDeltakerDbo(deltaker3.id, Veiledertype.VEILEDER)
				)
			)
		)

		val mineDeltakere = veilederService.getMineDeltakere(personIdent)

		mineDeltakere.size shouldBe 3

		val minDeltaker1 = mineDeltakere.find { it.id == deltaker.id }
		minDeltaker1?.fodselsnummer shouldBe deltaker.personident
		minDeltaker1?.deltakerliste?.id shouldBe deltakerliste.id
		minDeltaker1?.veiledertype shouldBe Veiledertype.VEILEDER
		minDeltaker1?.aktiveEndringsmeldinger?.size shouldBe 2

		val minDeltaker2 = mineDeltakere.find { it.id == deltaker2.id }
		minDeltaker2?.fodselsnummer shouldBe deltaker2.personident
		minDeltaker2?.deltakerliste?.id shouldBe deltakerliste.id
		minDeltaker2?.veiledertype shouldBe Veiledertype.MEDVEILEDER
		minDeltaker2?.aktiveEndringsmeldinger?.size shouldBe 0

		val minDeltaker3 = mineDeltakere.find { it.id == deltaker3.id }
		minDeltaker3?.fodselsnummer shouldBe deltaker3.personident
		minDeltaker3?.deltakerliste?.id shouldBe deltakerliste2.id
		minDeltaker3?.veiledertype shouldBe Veiledertype.VEILEDER
		minDeltaker3?.aktiveEndringsmeldinger?.size shouldBe 0
	}

	@Test
	fun `getMineDeltakere - ansatt har veilederrolle hos en arrangor og er veileder for deltakere - returnerer riktige deltakere`() {
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val arrangorId2 = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		val deltakerliste2 = deltakerliste.copy(id = UUID.randomUUID(), arrangorId = arrangorId2, navn = "Deltakerliste 2")
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste2)
		val deltaker = getDeltaker(UUID.randomUUID(), deltakerliste.id).copy(personident = "12345")
		val deltaker2 = getDeltaker(UUID.randomUUID(), deltakerliste.id).copy(personident = "23456")
		val deltaker3 = getDeltaker(UUID.randomUUID(), deltakerliste2.id).copy(personident = "34567")
		deltakerRepository.insertOrUpdateDeltaker(deltaker)
		deltakerRepository.insertOrUpdateDeltaker(deltaker2)
		deltakerRepository.insertOrUpdateDeltaker(deltaker3)
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
					VeilederDeltakerDbo(deltaker.id, Veiledertype.VEILEDER),
					VeilederDeltakerDbo(deltaker2.id, Veiledertype.MEDVEILEDER),
					VeilederDeltakerDbo(deltaker3.id, Veiledertype.VEILEDER)
				)
			)
		)

		val mineDeltakere = veilederService.getMineDeltakere(personIdent)

		mineDeltakere.size shouldBe 2
		mineDeltakere.find { it.id == deltaker.id } shouldNotBe null
		mineDeltakere.find { it.id == deltaker2.id } shouldNotBe null
		mineDeltakere.find { it.id == deltaker3.id } shouldBe null
	}

	@Test
	fun `getMineDeltakere - ansatt er veileder for deltakere som er skjult - filtrerer bort skjulte deltakere`() {
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltaker = getDeltaker(UUID.randomUUID(), deltakerliste.id).copy(personident = "12345")
		val deltaker2 = deltaker.copy(id = UUID.randomUUID(), personident = "23456", skjultDato = LocalDateTime.now())
		deltakerRepository.insertOrUpdateDeltaker(deltaker)
		deltakerRepository.insertOrUpdateDeltaker(deltaker2)
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
					VeilederDeltakerDbo(deltaker.id, Veiledertype.VEILEDER),
					VeilederDeltakerDbo(deltaker2.id, Veiledertype.MEDVEILEDER)
				)
			)
		)

		val mineDeltakere = veilederService.getMineDeltakere(personIdent)

		mineDeltakere.size shouldBe 1
		mineDeltakere.find { it.id == deltaker.id } shouldNotBe null
		mineDeltakere.find { it.id == deltaker2.id } shouldBe null
	}
}
