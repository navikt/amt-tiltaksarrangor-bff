package no.nav.tiltaksarrangor.endringsmelding.service

import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import no.nav.tiltaksarrangor.client.amtarrangor.AmtArrangorClient
import no.nav.tiltaksarrangor.client.amttiltak.AmtTiltakClient
import no.nav.tiltaksarrangor.ingest.model.AnsattRolle
import no.nav.tiltaksarrangor.model.exceptions.UnauthorizedException
import no.nav.tiltaksarrangor.repositories.AnsattRepository
import no.nav.tiltaksarrangor.repositories.DeltakerRepository
import no.nav.tiltaksarrangor.repositories.DeltakerlisteRepository
import no.nav.tiltaksarrangor.repositories.EndringsmeldingRepository
import no.nav.tiltaksarrangor.repositories.model.AnsattDbo
import no.nav.tiltaksarrangor.repositories.model.AnsattRolleDbo
import no.nav.tiltaksarrangor.repositories.model.KoordinatorDeltakerlisteDbo
import no.nav.tiltaksarrangor.service.AnsattService
import no.nav.tiltaksarrangor.service.MetricsService
import no.nav.tiltaksarrangor.testutils.DbTestDataUtils
import no.nav.tiltaksarrangor.testutils.SingletonPostgresContainer
import no.nav.tiltaksarrangor.testutils.getDeltaker
import no.nav.tiltaksarrangor.testutils.getDeltakerliste
import no.nav.tiltaksarrangor.testutils.getEndringsmelding
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.util.UUID

class EndringsmeldingServiceTest {
	private val amtTiltakClient = mockk<AmtTiltakClient>()
	private val amtArrangorClient = mockk<AmtArrangorClient>()
	private val metricsService = mockk<MetricsService>(relaxed = true)
	private val dataSource = SingletonPostgresContainer.getDataSource()
	private val template = NamedParameterJdbcTemplate(dataSource)
	private val ansattRepository = AnsattRepository(template)
	private val ansattService = AnsattService(amtArrangorClient, ansattRepository)
	private val deltakerRepository = DeltakerRepository(template)
	private val deltakerlisteRepository = DeltakerlisteRepository(template, deltakerRepository)
	private val endringsmeldingRepository = EndringsmeldingRepository(template)
	private val endringsmeldingService = EndringsmeldingService(
		amtTiltakClient,
		ansattService,
		endringsmeldingRepository,
		metricsService
	)

	@AfterEach
	internal fun tearDown() {
		DbTestDataUtils.cleanDatabase(dataSource)
		clearMocks(amtTiltakClient)
	}

	@Test
	fun `slettEndringsmelding - ansatt har ikke rolle hos arrangor - returnerer unauthorized`() {
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
					AnsattRolleDbo(UUID.randomUUID(), AnsattRolle.KOORDINATOR)
				),
				deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerliste.id)),
				veilederDeltakere = emptyList()
			)
		)
		val endringsmelding = getEndringsmelding(deltakerId)
		endringsmeldingRepository.insertOrUpdateEndringsmelding(endringsmelding)

		assertThrows<UnauthorizedException> {
			endringsmeldingService.slettEndringsmelding(endringsmelding.id, personIdent)
		}
	}

	@Test
	fun `slettEndringsmelding - ansatt har tilgang - sletter endringsmelding`() {
		coEvery { amtTiltakClient.tilbakekallEndringsmelding(any()) } just Runs
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
		val endringsmelding = getEndringsmelding(deltakerId)
		endringsmeldingRepository.insertOrUpdateEndringsmelding(endringsmelding)

		endringsmeldingService.slettEndringsmelding(endringsmelding.id, personIdent)

		endringsmeldingRepository.getEndringsmelding(endringsmelding.id) shouldBe null
		coVerify { amtTiltakClient.tilbakekallEndringsmelding(endringsmelding.id) }
	}
}
