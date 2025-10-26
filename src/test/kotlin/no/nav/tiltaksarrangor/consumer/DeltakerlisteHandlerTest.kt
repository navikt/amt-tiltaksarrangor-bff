package no.nav.tiltaksarrangor.consumer

import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.amt.lib.utils.objectMapper
import no.nav.tiltaksarrangor.client.amtarrangor.AmtArrangorClient
import no.nav.tiltaksarrangor.consumer.ConsumerTestUtils.arrangorInTest
import no.nav.tiltaksarrangor.consumer.ConsumerTestUtils.deltakerlisteIdInTest
import no.nav.tiltaksarrangor.consumer.ConsumerTestUtils.deltakerlistePayloadInTest
import no.nav.tiltaksarrangor.consumer.ConsumerTestUtils.tiltakstypePayloadInTest
import no.nav.tiltaksarrangor.consumer.KafkaConsumer.Companion.DELTAKERLISTE_V1_TOPIC
import no.nav.tiltaksarrangor.consumer.model.DeltakerlistePayload
import no.nav.tiltaksarrangor.repositories.ArrangorRepository
import no.nav.tiltaksarrangor.repositories.DeltakerlisteRepository
import no.nav.tiltaksarrangor.repositories.TiltakstypeRepository
import no.nav.tiltaksarrangor.testutils.getDeltakerliste
import no.nav.tiltaksarrangor.unleash.UnleashToggle
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DeltakerlisteHandlerTest {
	private val arrangorRepository = mockk<ArrangorRepository>()
	private val deltakerlisteRepository = mockk<DeltakerlisteRepository>()
	private val tiltakstypeRepository = mockk<TiltakstypeRepository>()
	private val amtArrangorClient = mockk<AmtArrangorClient>()
	private val unleashToggle: UnleashToggle = mockk()

	private val sut =
		DeltakerlisteHandler(
			arrangorRepository = arrangorRepository,
			deltakerlisteRepository = deltakerlisteRepository,
			tiltakstypeRepository = tiltakstypeRepository,
			amtArrangorClient = amtArrangorClient,
			unleashToggle = unleashToggle,
		)

	@BeforeEach
	fun resetMocks() {
		clearAllMocks()

		every { deltakerlisteRepository.insertOrUpdateDeltakerliste(any()) } just Runs
		every { deltakerlisteRepository.getDeltakerliste(any()) } returns getDeltakerliste(arrangorInTest.id)
		every { deltakerlisteRepository.deleteDeltakerlisteOgDeltakere(any()) } returns 1
		every { tiltakstypeRepository.getById(any()) } returns tiltakstypePayloadInTest.toModel()

		every { arrangorRepository.getArrangor(arrangorInTest.organisasjonsnummer) } returns null
		every { arrangorRepository.insertOrUpdateArrangor(any()) } just Runs
		coEvery { amtArrangorClient.getArrangor(arrangorInTest.organisasjonsnummer) } returns arrangorInTest
		every { unleashToggle.erKometMasterForTiltakstype(any<String>()) } returns true
	}

	@Test
	fun `lagreDeltakerliste - status GJENNOMFORES - lagres i db `() {
		sut.lagreDeltakerliste(
			topic = DELTAKERLISTE_V1_TOPIC,
			deltakerlisteId = deltakerlisteIdInTest,
			value = objectMapper.writeValueAsString(deltakerlistePayloadInTest),
		)

		verify(exactly = 1) { deltakerlisteRepository.insertOrUpdateDeltakerliste(any()) }
	}

	@Test
	fun `lagreDeltakerliste - status AVSLUTTET for 6 mnd siden - lagres ikke`() {
		val deltakerlisteDto = deltakerlistePayloadInTest.copy(
			navn = "Avsluttet tiltak",
			sluttDato = LocalDate.now().minusMonths(6),
			status = DeltakerlistePayload.Status.AVSLUTTET,
			tiltakstype = deltakerlistePayloadInTest.tiltakstype,
		)

		sut.lagreDeltakerliste(
			topic = DELTAKERLISTE_V1_TOPIC,
			deltakerlisteId = deltakerlisteIdInTest,
			value = objectMapper.writeValueAsString(deltakerlisteDto),
		)

		verify(exactly = 0) { deltakerlisteRepository.insertOrUpdateDeltakerliste(any()) }
		verify(exactly = 1) { deltakerlisteRepository.deleteDeltakerlisteOgDeltakere(deltakerlisteIdInTest) }
	}

	@Test
	fun `lagreDeltakerliste - status AVSLUTTET for 1 uke siden - lagres i db`() {
		val deltakerlisteDto = deltakerlistePayloadInTest.copy(
			navn = "Avsluttet tiltak",
			sluttDato = LocalDate.now().minusWeeks(1),
			status = DeltakerlistePayload.Status.AVSLUTTET,
		)

		sut.lagreDeltakerliste(
			topic = DELTAKERLISTE_V1_TOPIC,
			deltakerlisteId = deltakerlisteIdInTest,
			value = objectMapper.writeValueAsString(deltakerlisteDto),
		)

		verify(exactly = 1) { deltakerlisteRepository.insertOrUpdateDeltakerliste(any()) }
	}

	@Test
	fun `lagreDeltakerliste - ikke stottet tiltakstype - lagres ikke i db `() {
		every { unleashToggle.erKometMasterForTiltakstype("KODE_FINNES_IKKE") } returns false

		val deltakerlisteDto = deltakerlistePayloadInTest.copy(
			tiltakstype = deltakerlistePayloadInTest.tiltakstype.copy(
				tiltakskode = "KODE_FINNES_IKKE",
			),
		)

		sut.lagreDeltakerliste(
			topic = DELTAKERLISTE_V1_TOPIC,
			deltakerlisteId = deltakerlisteIdInTest,
			value = objectMapper.writeValueAsString(deltakerlisteDto),
		)

		verify(exactly = 0) { deltakerlisteRepository.insertOrUpdateDeltakerliste(any()) }
	}
}
