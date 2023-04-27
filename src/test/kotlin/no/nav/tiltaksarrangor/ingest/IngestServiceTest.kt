package no.nav.tiltaksarrangor.ingest

import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.tiltaksarrangor.ingest.model.DeltakerDto
import no.nav.tiltaksarrangor.ingest.model.DeltakerKontaktinformasjonDto
import no.nav.tiltaksarrangor.ingest.model.DeltakerNavVeilederDto
import no.nav.tiltaksarrangor.ingest.model.DeltakerPersonaliaDto
import no.nav.tiltaksarrangor.ingest.model.DeltakerStatus
import no.nav.tiltaksarrangor.ingest.model.DeltakerStatusDto
import no.nav.tiltaksarrangor.ingest.model.DeltakerlisteArrangorDto
import no.nav.tiltaksarrangor.ingest.model.DeltakerlisteDto
import no.nav.tiltaksarrangor.ingest.model.DeltakerlisteStatus
import no.nav.tiltaksarrangor.ingest.model.NavnDto
import no.nav.tiltaksarrangor.ingest.model.TiltakDto
import no.nav.tiltaksarrangor.ingest.repositories.AnsattRepository
import no.nav.tiltaksarrangor.ingest.repositories.ArrangorRepository
import no.nav.tiltaksarrangor.ingest.repositories.DeltakerRepository
import no.nav.tiltaksarrangor.ingest.repositories.DeltakerlisteRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class IngestServiceTest {
	private val arrangorRepository = mockk<ArrangorRepository>()
	private val ansattRepository = mockk<AnsattRepository>()
	private val deltakerlisteRepository = mockk<DeltakerlisteRepository>()
	private val deltakerRepository = mockk<DeltakerRepository>()
	private val ingestService = IngestService(arrangorRepository, ansattRepository, deltakerlisteRepository, deltakerRepository)

	@BeforeEach
	internal fun resetMocks() {
		clearMocks(arrangorRepository, ansattRepository, deltakerlisteRepository, deltakerRepository)
		every { deltakerlisteRepository.insertOrUpdateDeltakerliste(any()) } just Runs
		every { deltakerlisteRepository.deleteDeltakerliste(any()) } returns 1
		every { deltakerRepository.insertOrUpdateDeltaker(any()) } just Runs
		every { deltakerRepository.deleteDeltaker(any()) } returns 1
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
		verify(exactly = 1) { deltakerlisteRepository.deleteDeltakerliste(deltakerlisteId) }
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
		verify(exactly = 1) { deltakerlisteRepository.deleteDeltakerliste(deltakerlisteId) }
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

	@Test
	internal fun `lagreDeltaker - status DELTAR - lagres i db `() {
		val deltakerId = UUID.randomUUID()
		val deltakerDto = DeltakerDto(
			id = deltakerId,
			deltakerlisteId = UUID.randomUUID(),
			personalia = DeltakerPersonaliaDto(
				personident = "10987654321",
				navn = NavnDto("Fornavn", null, "Etternavn"),
				kontaktinformasjon = DeltakerKontaktinformasjonDto("98989898", "epost@nav.no"),
				skjermet = false
			),
			status = DeltakerStatusDto(
				type = DeltakerStatus.DELTAR,
				gyldigFra = LocalDate.now().minusWeeks(5).atStartOfDay(),
				opprettetDato = LocalDateTime.now().minusWeeks(6)
			),
			dagerPerUke = null,
			prosentStilling = null,
			oppstartsdato = LocalDate.now().minusWeeks(5),
			sluttdato = null,
			innsoktDato = LocalDate.now().minusMonths(2),
			bestillingTekst = "Bestilling",
			navKontor = "NAV Oslo",
			navVeileder = DeltakerNavVeilederDto(UUID.randomUUID(), "Per Veileder", null),
			skjult = null
		)

		ingestService.lagreDeltaker(deltakerId, deltakerDto)

		verify(exactly = 1) { deltakerRepository.insertOrUpdateDeltaker(any()) }
	}

	@Test
	internal fun `lagreDeltaker - status SOKT_INN - lagres ikke i db `() {
		val deltakerId = UUID.randomUUID()
		val deltakerDto = DeltakerDto(
			id = deltakerId,
			deltakerlisteId = UUID.randomUUID(),
			personalia = DeltakerPersonaliaDto(
				personident = "10987654321",
				navn = NavnDto("Fornavn", null, "Etternavn"),
				kontaktinformasjon = DeltakerKontaktinformasjonDto("98989898", "epost@nav.no"),
				skjermet = false
			),
			status = DeltakerStatusDto(
				type = DeltakerStatus.SOKT_INN,
				gyldigFra = LocalDate.now().minusWeeks(5).atStartOfDay(),
				opprettetDato = LocalDateTime.now().minusWeeks(6)
			),
			dagerPerUke = null,
			prosentStilling = null,
			oppstartsdato = LocalDate.now().minusWeeks(5),
			sluttdato = null,
			innsoktDato = LocalDate.now().minusMonths(2),
			bestillingTekst = "Bestilling",
			navKontor = "NAV Oslo",
			navVeileder = DeltakerNavVeilederDto(UUID.randomUUID(), "Per Veileder", null),
			skjult = null
		)

		ingestService.lagreDeltaker(deltakerId, deltakerDto)

		verify(exactly = 0) { deltakerRepository.insertOrUpdateDeltaker(any()) }
		verify(exactly = 1) { deltakerRepository.deleteDeltaker(deltakerId) }
	}

	@Test
	internal fun `lagreDeltaker - status HAR_SLUTTET for mer enn to uker siden - lagres ikke i db `() {
		val deltakerId = UUID.randomUUID()
		val deltakerDto = DeltakerDto(
			id = deltakerId,
			deltakerlisteId = UUID.randomUUID(),
			personalia = DeltakerPersonaliaDto(
				personident = "10987654321",
				navn = NavnDto("Fornavn", null, "Etternavn"),
				kontaktinformasjon = DeltakerKontaktinformasjonDto("98989898", "epost@nav.no"),
				skjermet = false
			),
			status = DeltakerStatusDto(
				type = DeltakerStatus.HAR_SLUTTET,
				gyldigFra = LocalDate.now().minusWeeks(3).atStartOfDay(),
				opprettetDato = LocalDateTime.now().minusWeeks(6)
			),
			dagerPerUke = null,
			prosentStilling = null,
			oppstartsdato = LocalDate.now().minusWeeks(5),
			sluttdato = null,
			innsoktDato = LocalDate.now().minusMonths(2),
			bestillingTekst = "Bestilling",
			navKontor = "NAV Oslo",
			navVeileder = DeltakerNavVeilederDto(UUID.randomUUID(), "Per Veileder", null),
			skjult = null
		)

		ingestService.lagreDeltaker(deltakerId, deltakerDto)

		verify(exactly = 0) { deltakerRepository.insertOrUpdateDeltaker(any()) }
		verify(exactly = 1) { deltakerRepository.deleteDeltaker(deltakerId) }
	}

	@Test
	internal fun `lagreDeltaker - status HAR_SLUTTET for mindre enn to uker siden - lagres i db `() {
		val deltakerId = UUID.randomUUID()
		val deltakerDto = DeltakerDto(
			id = deltakerId,
			deltakerlisteId = UUID.randomUUID(),
			personalia = DeltakerPersonaliaDto(
				personident = "10987654321",
				navn = NavnDto("Fornavn", null, "Etternavn"),
				kontaktinformasjon = DeltakerKontaktinformasjonDto("98989898", "epost@nav.no"),
				skjermet = false
			),
			status = DeltakerStatusDto(
				type = DeltakerStatus.HAR_SLUTTET,
				gyldigFra = LocalDate.now().minusWeeks(1).atStartOfDay(),
				opprettetDato = LocalDateTime.now().minusWeeks(6)
			),
			dagerPerUke = null,
			prosentStilling = null,
			oppstartsdato = LocalDate.now().minusWeeks(5),
			sluttdato = null,
			innsoktDato = LocalDate.now().minusMonths(2),
			bestillingTekst = "Bestilling",
			navKontor = "NAV Oslo",
			navVeileder = DeltakerNavVeilederDto(UUID.randomUUID(), "Per Veileder", null),
			skjult = null
		)

		ingestService.lagreDeltaker(deltakerId, deltakerDto)

		verify(exactly = 1) { deltakerRepository.insertOrUpdateDeltaker(any()) }
	}
}
