package no.nav.tiltaksarrangor.consumer

import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.amt.lib.models.deltakerliste.tiltakstype.ArenaKode
import no.nav.amt.lib.utils.objectMapper
import no.nav.tiltaksarrangor.client.amtarrangor.AmtArrangorClient
import no.nav.tiltaksarrangor.client.amtarrangor.dto.ArrangorMedOverordnetArrangor
import no.nav.tiltaksarrangor.consumer.model.DeltakerlistePayload
import no.nav.tiltaksarrangor.consumer.model.Oppstartstype
import no.nav.tiltaksarrangor.repositories.ArrangorRepository
import no.nav.tiltaksarrangor.repositories.DeltakerlisteRepository
import no.nav.tiltaksarrangor.testutils.getDeltakerliste
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class KafkaConsumerDeltakerlisteHandlerTest {
	private val arrangorRepository = mockk<ArrangorRepository>()
	private val deltakerlisteRepository = mockk<DeltakerlisteRepository>()
	private val amtArrangorClient = mockk<AmtArrangorClient>()
	private val sut =
		KafkaConsumerDeltakerlisteHandler(
			arrangorRepository,
			deltakerlisteRepository,
			amtArrangorClient,
		)

	@BeforeEach
	fun resetMocks() {
		clearAllMocks()

		every { deltakerlisteRepository.insertOrUpdateDeltakerliste(any()) } just Runs
		every { deltakerlisteRepository.getDeltakerliste(any()) } returns getDeltakerliste(arrangor.id)
		every { deltakerlisteRepository.deleteDeltakerlisteOgDeltakere(any()) } returns 1
		every { arrangorRepository.getArrangor("88888888") } returns null
		every { arrangorRepository.insertOrUpdateArrangor(any()) } just Runs
		coEvery { amtArrangorClient.getArrangor("88888888") } returns arrangor
	}

	@Test
	fun `lagreDeltakerliste - status GJENNOMFORES - lagres i db `() {
		sut.lagreDeltakerliste(
			deltakerlisteId = deltakerlisteId,
			value = objectMapper.writeValueAsString(deltakerlisteDtoBase),
		)

		verify(exactly = 1) { deltakerlisteRepository.insertOrUpdateDeltakerliste(any()) }
	}

	@Test
	fun `lagreDeltakerliste - status AVSLUTTET for 6 mnd siden - lagres ikke`() {
		val deltakerlisteDto = deltakerlisteDtoBase.copy(
			navn = "Avsluttet tiltak",
			sluttDato = LocalDate.now().minusMonths(6),
			status = DeltakerlistePayload.Status.AVSLUTTET,
			tiltakstype = deltakerlisteDtoBase.tiltakstype.copy(
				navn = "Endret tiltakstype",
			),
		)

		sut.lagreDeltakerliste(
			deltakerlisteId = deltakerlisteId,
			value = objectMapper.writeValueAsString(deltakerlisteDto),
		)

		verify(exactly = 0) { deltakerlisteRepository.insertOrUpdateDeltakerliste(any()) }
		verify(exactly = 1) { deltakerlisteRepository.deleteDeltakerlisteOgDeltakere(deltakerlisteId) }
	}

	@Test
	fun `lagreDeltakerliste - status AVSLUTTET for 1 uke siden - lagres i db`() {
		val deltakerlisteDto = deltakerlisteDtoBase.copy(
			navn = "Avsluttet tiltak",
			sluttDato = LocalDate.now().minusWeeks(1),
			status = DeltakerlistePayload.Status.AVSLUTTET,
		)

		sut.lagreDeltakerliste(
			deltakerlisteId = deltakerlisteId,
			value = objectMapper.writeValueAsString(deltakerlisteDto),
		)

		verify(exactly = 1) { deltakerlisteRepository.insertOrUpdateDeltakerliste(any()) }
	}

	@Test
	fun `lagreDeltakerliste - ikke stottet tiltakstype - lagres ikke i db `() {
		val deltakerlisteDto = deltakerlisteDtoBase.copy(
			tiltakstype = deltakerlisteDtoBase.tiltakstype.copy(
				arenaKode = "KODE_FINNES_IKKE",
				tiltakskode = "KODE_FINNES_IKKE",
			),
		)

		sut.lagreDeltakerliste(
			deltakerlisteId = deltakerlisteId,
			value = objectMapper.writeValueAsString(deltakerlisteDto),
		)

		verify(exactly = 0) { deltakerlisteRepository.insertOrUpdateDeltakerliste(any()) }
	}

	@Test
	fun `lagreDeltakerliste - enkeltplass tiltak - lagres ikke i db `() {
		val deltakerlisteDto = deltakerlisteDtoBase.copy(
			tiltakstype = deltakerlisteDtoBase.tiltakstype.copy(
				arenaKode = ArenaKode.ENKELAMO.name,
				tiltakskode = ArenaKode.ENKELAMO.toTiltaksKode().toString(),
			),
		)

		sut.lagreDeltakerliste(
			deltakerlisteId = deltakerlisteId,
			value = objectMapper.writeValueAsString(deltakerlisteDto),
		)

		verify(exactly = 0) { deltakerlisteRepository.insertOrUpdateDeltakerliste(any()) }
	}

	companion object {
		private val arrangor =
			ArrangorMedOverordnetArrangor(
				id = UUID.randomUUID(),
				navn = "Arrangør AS",
				organisasjonsnummer = "88888888",
				overordnetArrangor = null,
			)

		private val deltakerlisteId = UUID.randomUUID()

		private val deltakerlisteDtoBase =
			DeltakerlistePayload(
				id = deltakerlisteId,
				tiltakstype =
					DeltakerlistePayload.Tiltakstype(
						id = UUID.randomUUID(),
						navn = "Det flotte tiltaket",
						arenaKode = "DIGIOPPARB",
						tiltakskode = "DIGITALT_OPPFOLGINGSTILTAK",
					),
				navn = "Gjennomføring av tiltak",
				startDato = LocalDate.now().minusYears(2),
				sluttDato = null,
				status = DeltakerlistePayload.Status.GJENNOMFORES,
				virksomhetsnummer = "88888888",
				oppstart = Oppstartstype.LOPENDE,
				tilgjengeligForArrangorFraOgMedDato = null,
			)
	}
}
