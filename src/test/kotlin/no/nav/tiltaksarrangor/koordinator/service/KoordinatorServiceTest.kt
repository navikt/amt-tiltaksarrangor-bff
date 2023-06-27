package no.nav.tiltaksarrangor.koordinator.service

import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.mockk
import no.nav.tiltaksarrangor.client.amtarrangor.AmtArrangorClient
import no.nav.tiltaksarrangor.client.amttiltak.AmtTiltakClient
import no.nav.tiltaksarrangor.ingest.model.AnsattRolle
import no.nav.tiltaksarrangor.model.Veiledertype
import no.nav.tiltaksarrangor.model.exceptions.UnauthorizedException
import no.nav.tiltaksarrangor.repositories.AnsattRepository
import no.nav.tiltaksarrangor.repositories.DeltakerRepository
import no.nav.tiltaksarrangor.repositories.DeltakerlisteRepository
import no.nav.tiltaksarrangor.repositories.model.AnsattDbo
import no.nav.tiltaksarrangor.repositories.model.AnsattRolleDbo
import no.nav.tiltaksarrangor.repositories.model.KoordinatorDeltakerlisteDbo
import no.nav.tiltaksarrangor.repositories.model.VeilederDeltakerDbo
import no.nav.tiltaksarrangor.service.AnsattService
import no.nav.tiltaksarrangor.testutils.DbTestDataUtils
import no.nav.tiltaksarrangor.testutils.SingletonPostgresContainer
import no.nav.tiltaksarrangor.testutils.getDeltaker
import no.nav.tiltaksarrangor.testutils.getDeltakerliste
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
		val deltakerId = UUID.randomUUID()
		val deltakerId2 = UUID.randomUUID()
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId))
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId2))
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
					VeilederDeltakerDbo(deltakerId, Veiledertype.VEILEDER),
					VeilederDeltakerDbo(deltakerId2, Veiledertype.MEDVEILEDER)
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
				roller = listOf(AnsattRolleDbo(arrangorId, AnsattRolle.VEILEDER)),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id)),
				veilederDeltakere = listOf(
					VeilederDeltakerDbo(deltakerId1, Veiledertype.VEILEDER),
					VeilederDeltakerDbo(deltakerId2, Veiledertype.MEDVEILEDER),
					VeilederDeltakerDbo(deltakerId3, Veiledertype.VEILEDER),
					VeilederDeltakerDbo(deltakerId4, Veiledertype.MEDVEILEDER),
					VeilederDeltakerDbo(deltakerId5, Veiledertype.VEILEDER)
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
		val deltakerId = UUID.randomUUID()
		val deltakerId2 = UUID.randomUUID()
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId))
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId2))
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
					VeilederDeltakerDbo(deltakerId, Veiledertype.VEILEDER),
					VeilederDeltakerDbo(deltakerId2, Veiledertype.MEDVEILEDER)
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
				roller = listOf(
					AnsattRolleDbo(arrangorId, AnsattRolle.KOORDINATOR),
					AnsattRolleDbo(arrangorId, AnsattRolle.VEILEDER),
					AnsattRolleDbo(arrangorId2, AnsattRolle.KOORDINATOR)
				),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id), KoordinatorDeltakerlisteDbo(deltakerliste2.id)),
				veilederDeltakere = listOf(
					VeilederDeltakerDbo(deltakerId1, Veiledertype.VEILEDER),
					VeilederDeltakerDbo(deltakerId2, Veiledertype.MEDVEILEDER),
					VeilederDeltakerDbo(deltakerId3, Veiledertype.VEILEDER),
					VeilederDeltakerDbo(deltakerId4, Veiledertype.MEDVEILEDER),
					VeilederDeltakerDbo(deltakerId5, Veiledertype.VEILEDER)
				)
			)
		)

		val mineDeltakerlister = koordinatorService.getMineDeltakerlister(personIdent)

		mineDeltakerlister.veilederFor?.veilederFor shouldBe 3
		mineDeltakerlister.veilederFor?.medveilederFor shouldBe 2
		mineDeltakerlister.koordinatorFor?.deltakerlister?.size shouldBe 2
	}
}
