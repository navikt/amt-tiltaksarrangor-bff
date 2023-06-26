package no.nav.tiltaksarrangor.koordinator.service

import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.mockk
import no.nav.tiltaksarrangor.client.amtarrangor.AmtArrangorClient
import no.nav.tiltaksarrangor.client.amttiltak.AmtTiltakClient
import no.nav.tiltaksarrangor.ingest.model.AnsattRolle
import no.nav.tiltaksarrangor.ingest.model.DeltakerlisteStatus
import no.nav.tiltaksarrangor.model.Veiledertype
import no.nav.tiltaksarrangor.model.exceptions.UnauthorizedException
import no.nav.tiltaksarrangor.repositories.AnsattRepository
import no.nav.tiltaksarrangor.repositories.DeltakerRepository
import no.nav.tiltaksarrangor.repositories.DeltakerlisteRepository
import no.nav.tiltaksarrangor.repositories.model.AnsattDbo
import no.nav.tiltaksarrangor.repositories.model.AnsattRolleDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerlisteDbo
import no.nav.tiltaksarrangor.repositories.model.KoordinatorDeltakerlisteDbo
import no.nav.tiltaksarrangor.repositories.model.VeilederDeltakerDbo
import no.nav.tiltaksarrangor.service.AnsattService
import no.nav.tiltaksarrangor.testutils.DbTestDataUtils
import no.nav.tiltaksarrangor.testutils.SingletonPostgresContainer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.util.UUID

class KoordinatorServiceTest {
	private val amtTiltakClient = mockk<AmtTiltakClient>()
	private val amtArrangorClient = mockk<AmtArrangorClient>()
	private val dataSource = SingletonPostgresContainer.getDataSource()
	private val template = NamedParameterJdbcTemplate(dataSource)
	private val ansattRepository = AnsattRepository(template)
	private val ansattService = AnsattService(amtArrangorClient, ansattRepository)
	private val deltakerRepository = DeltakerRepository(template)
	private val deltakerlisteRepository = DeltakerlisteRepository(template, deltakerRepository)
	private val koordinatorService = KoordinatorService(amtTiltakClient, ansattService, deltakerlisteRepository)

	@AfterEach
	internal fun tearDown() {
		DbTestDataUtils.cleanDatabase(dataSource)
		clearMocks(amtArrangorClient, amtTiltakClient)
	}

	@Test
	fun `getMineDeltakerlister - ansatt har ingen roller - returnerer unauthorized`() {
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.randomUUID(),
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller = emptyList(),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id)),
				veilederDeltakere = listOf(
					VeilederDeltakerDbo(UUID.randomUUID(), Veiledertype.VEILEDER),
					VeilederDeltakerDbo(UUID.randomUUID(), Veiledertype.MEDVEILEDER)
				)
			)
		)

		assertThrows<UnauthorizedException> {
			koordinatorService.getMineDeltakerlister(personIdent)
		}
	}

	@Test
	fun `getMineDeltakerlister - ansatt er veileder - returnerer riktig respons`() {
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.randomUUID(),
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller = listOf(AnsattRolleDbo(arrangorId, AnsattRolle.VEILEDER)),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id)),
				veilederDeltakere = listOf(
					VeilederDeltakerDbo(UUID.randomUUID(), Veiledertype.VEILEDER),
					VeilederDeltakerDbo(UUID.randomUUID(), Veiledertype.MEDVEILEDER),
					VeilederDeltakerDbo(UUID.randomUUID(), Veiledertype.VEILEDER),
					VeilederDeltakerDbo(UUID.randomUUID(), Veiledertype.MEDVEILEDER),
					VeilederDeltakerDbo(UUID.randomUUID(), Veiledertype.VEILEDER)
				)
			)
		)

		val mineDeltakerlister = koordinatorService.getMineDeltakerlister(personIdent)

		mineDeltakerlister.veilederFor?.veilederFor shouldBe 3
		mineDeltakerlister.veilederFor?.medveilederFor shouldBe 2
		mineDeltakerlister.koordinatorFor shouldBe null
	}

	@Test
	fun `getMineDeltakerlister - ansatt er koordinator - returnerer riktig respons`() {
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
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
				veilederDeltakere = listOf(
					VeilederDeltakerDbo(UUID.randomUUID(), Veiledertype.VEILEDER),
					VeilederDeltakerDbo(UUID.randomUUID(), Veiledertype.MEDVEILEDER)
				)
			)
		)

		val mineDeltakerlister = koordinatorService.getMineDeltakerlister(personIdent)

		mineDeltakerlister.veilederFor shouldBe null
		mineDeltakerlister.koordinatorFor?.deltakerlister?.size shouldBe 1
	}

	@Test
	fun `getMineDeltakerlister - ansatt er veileder og koordinator - returnerer riktig respons`() {
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val arrangorId2 = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		val deltakerliste2 = getDeltakerliste(arrangorId2)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste2)
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = UUID.randomUUID(),
				personIdent = personIdent,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				roller = listOf(
					AnsattRolleDbo(arrangorId, AnsattRolle.KOORDINATOR),
					AnsattRolleDbo(arrangorId, AnsattRolle.VEILEDER),
					AnsattRolleDbo(arrangorId2, AnsattRolle.KOORDINATOR)
				),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id), KoordinatorDeltakerlisteDbo(deltakerliste2.id)),
				veilederDeltakere = listOf(
					VeilederDeltakerDbo(UUID.randomUUID(), Veiledertype.VEILEDER),
					VeilederDeltakerDbo(UUID.randomUUID(), Veiledertype.MEDVEILEDER),
					VeilederDeltakerDbo(UUID.randomUUID(), Veiledertype.VEILEDER),
					VeilederDeltakerDbo(UUID.randomUUID(), Veiledertype.MEDVEILEDER),
					VeilederDeltakerDbo(UUID.randomUUID(), Veiledertype.VEILEDER)
				)
			)
		)

		val mineDeltakerlister = koordinatorService.getMineDeltakerlister(personIdent)

		mineDeltakerlister.veilederFor?.veilederFor shouldBe 3
		mineDeltakerlister.veilederFor?.medveilederFor shouldBe 2
		mineDeltakerlister.koordinatorFor?.deltakerlister?.size shouldBe 2
	}

	private fun getDeltakerliste(arrangorId: UUID): DeltakerlisteDbo {
		return DeltakerlisteDbo(
			id = UUID.randomUUID(),
			navn = "Gjennomføring 1",
			status = DeltakerlisteStatus.GJENNOMFORES,
			arrangorId = arrangorId,
			tiltakNavn = "Tiltaksnavnet",
			tiltakType = "ARBFORB",
			startDato = null,
			sluttDato = null,
			erKurs = false
		)
	}
}
