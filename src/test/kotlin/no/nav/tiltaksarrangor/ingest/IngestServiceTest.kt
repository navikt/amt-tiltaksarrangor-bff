package no.nav.tiltaksarrangor.ingest

import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.tiltaksarrangor.ingest.model.DeltakerlisteArrangorDto
import no.nav.tiltaksarrangor.ingest.model.DeltakerlisteDto
import no.nav.tiltaksarrangor.ingest.model.DeltakerlisteStatus
import no.nav.tiltaksarrangor.ingest.model.TiltakDto
import no.nav.tiltaksarrangor.ingest.repositories.AnsattRepository
import no.nav.tiltaksarrangor.ingest.repositories.ArrangorRepository
import no.nav.tiltaksarrangor.ingest.repositories.DeltakerlisteRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class IngestServiceTest {
	private val arrangorRepository = mockk<ArrangorRepository>()
	private val ansattRepository = mockk<AnsattRepository>()
	private val deltakerlisteRepository = mockk<DeltakerlisteRepository>()
	private val ingestService = IngestService(arrangorRepository, ansattRepository, deltakerlisteRepository)

	@BeforeEach
	internal fun resetMocks() {
		clearMocks(arrangorRepository, ansattRepository, deltakerlisteRepository)
		every { deltakerlisteRepository.insertOrUpdateDeltakerliste(any()) } just Runs
		every { deltakerlisteRepository.deleteDeltakerliste(any()) } returns 1
	}

	@Test
	internal fun `lagreDeltakerliste - status GJENNOMFORES - lagres i db `() {
		val deltakerlisteId = UUID.randomUUID()
		val deltakerlisteDto = DeltakerlisteDto(
			id = deltakerlisteId,
			navn = "Gjennomføring av tiltak",
			status = DeltakerlisteStatus.GJENNOMFORES,
			arrangor = DeltakerlisteArrangorDto(
				id = UUID.randomUUID(),
				organisasjonsnummer = "88888888",
				navn = "Arrangør AS"
			),
			tiltak = TiltakDto(
				navn = "Avsluttet tiltak",
				type = "AMO"
			),
			startDato = LocalDate.now().minusYears(2),
			sluttDato = null,
			erKurs = false
		)

		ingestService.lagreDeltakerliste(deltakerlisteId, deltakerlisteDto)

		verify(exactly = 1) { deltakerlisteRepository.insertOrUpdateDeltakerliste(any()) }
	}

	@Test
	internal fun `lagreDeltakerliste - status APENT_FOR_INNSOK - lagres ikke`() {
		val deltakerlisteId = UUID.randomUUID()
		val deltakerlisteDto = DeltakerlisteDto(
			id = deltakerlisteId,
			navn = "Gjennomføring av tiltak",
			status = DeltakerlisteStatus.APENT_FOR_INNSOK,
			arrangor = DeltakerlisteArrangorDto(
				id = UUID.randomUUID(),
				organisasjonsnummer = "88888888",
				navn = "Arrangør AS"
			),
			tiltak = TiltakDto(
				navn = "Avsluttet tiltak",
				type = "AMO"
			),
			startDato = LocalDate.now().minusYears(2),
			sluttDato = null,
			erKurs = false
		)

		ingestService.lagreDeltakerliste(deltakerlisteId, deltakerlisteDto)

		verify(exactly = 0) { deltakerlisteRepository.insertOrUpdateDeltakerliste(any()) }
	}

	@Test
	internal fun `lagreDeltakerliste - status AVSLUTTET for 6 mnd siden - lagres ikke`() {
		val deltakerlisteId = UUID.randomUUID()
		val deltakerlisteDto = DeltakerlisteDto(
			id = deltakerlisteId,
			navn = "Gjennomføring av tiltak",
			status = DeltakerlisteStatus.AVSLUTTET,
			arrangor = DeltakerlisteArrangorDto(
				id = UUID.randomUUID(),
				organisasjonsnummer = "88888888",
				navn = "Arrangør AS"
			),
			tiltak = TiltakDto(
				navn = "Avsluttet tiltak",
				type = "AMO"
			),
			startDato = LocalDate.now().minusYears(2),
			sluttDato = LocalDate.now().minusMonths(6),
			erKurs = false
		)

		ingestService.lagreDeltakerliste(deltakerlisteId, deltakerlisteDto)

		verify(exactly = 0) { deltakerlisteRepository.insertOrUpdateDeltakerliste(any()) }
	}

	@Test
	internal fun `lagreDeltakerliste - status AVSLUTTET for 1 uke siden - lagres i db`() {
		val deltakerlisteId = UUID.randomUUID()
		val deltakerlisteDto = DeltakerlisteDto(
			id = deltakerlisteId,
			navn = "Gjennomføring av tiltak",
			status = DeltakerlisteStatus.AVSLUTTET,
			arrangor = DeltakerlisteArrangorDto(
				id = UUID.randomUUID(),
				organisasjonsnummer = "88888888",
				navn = "Arrangør AS"
			),
			tiltak = TiltakDto(
				navn = "Avsluttet tiltak",
				type = "AMO"
			),
			startDato = LocalDate.now().minusYears(2),
			sluttDato = LocalDate.now().minusWeeks(1),
			erKurs = false
		)

		ingestService.lagreDeltakerliste(deltakerlisteId, deltakerlisteDto)

		verify(exactly = 1) { deltakerlisteRepository.insertOrUpdateDeltakerliste(any()) }
	}
}
