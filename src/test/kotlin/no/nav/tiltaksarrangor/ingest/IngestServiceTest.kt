package no.nav.tiltaksarrangor.ingest

import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.tiltaksarrangor.client.amtarrangor.AmtArrangorClient
import no.nav.tiltaksarrangor.client.amtarrangor.dto.ArrangorMedOverordnetArrangor
import no.nav.tiltaksarrangor.ingest.model.DeltakerDto
import no.nav.tiltaksarrangor.ingest.model.DeltakerKontaktinformasjonDto
import no.nav.tiltaksarrangor.ingest.model.DeltakerNavVeilederDto
import no.nav.tiltaksarrangor.ingest.model.DeltakerPersonaliaDto
import no.nav.tiltaksarrangor.ingest.model.DeltakerStatus
import no.nav.tiltaksarrangor.ingest.model.DeltakerStatusDto
import no.nav.tiltaksarrangor.ingest.model.DeltakerlisteDto
import no.nav.tiltaksarrangor.ingest.model.EndringsmeldingDto
import no.nav.tiltaksarrangor.ingest.model.EndringsmeldingType
import no.nav.tiltaksarrangor.ingest.model.Innhold
import no.nav.tiltaksarrangor.ingest.model.NavnDto
import no.nav.tiltaksarrangor.model.DeltakerlisteStatus
import no.nav.tiltaksarrangor.model.Endringsmelding
import no.nav.tiltaksarrangor.model.StatusType
import no.nav.tiltaksarrangor.repositories.AnsattRepository
import no.nav.tiltaksarrangor.repositories.ArrangorRepository
import no.nav.tiltaksarrangor.repositories.DeltakerRepository
import no.nav.tiltaksarrangor.repositories.DeltakerlisteRepository
import no.nav.tiltaksarrangor.repositories.EndringsmeldingRepository
import no.nav.tiltaksarrangor.testutils.getAdresse
import no.nav.tiltaksarrangor.testutils.getDeltaker
import no.nav.tiltaksarrangor.testutils.getVurderinger
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
	private val endringsmeldingRepository = mockk<EndringsmeldingRepository>()
	private val amtArrangorClient = mockk<AmtArrangorClient>()
	private val ingestService =
		IngestService(
			arrangorRepository,
			ansattRepository,
			deltakerlisteRepository,
			deltakerRepository,
			endringsmeldingRepository,
			amtArrangorClient,
		)

	private val arrangor =
		ArrangorMedOverordnetArrangor(
			id = UUID.randomUUID(),
			navn = "Arrangør AS",
			organisasjonsnummer = "88888888",
			overordnetArrangor = null,
		)

	@BeforeEach
	internal fun resetMocks() {
		clearMocks(
			arrangorRepository,
			ansattRepository,
			deltakerlisteRepository,
			deltakerRepository,
			endringsmeldingRepository,
			amtArrangorClient,
		)
		every { deltakerlisteRepository.insertOrUpdateDeltakerliste(any()) } just Runs
		every { deltakerlisteRepository.deleteDeltakerlisteOgDeltakere(any()) } returns 1
		every { deltakerRepository.insertOrUpdateDeltaker(any()) } just Runs
		every { deltakerRepository.deleteDeltaker(any()) } returns 1
		every { endringsmeldingRepository.insertOrUpdateEndringsmelding(any()) } just Runs
		every { endringsmeldingRepository.deleteEndringsmelding(any()) } returns 1
		every { arrangorRepository.getArrangor("88888888") } returns null
		every { arrangorRepository.insertOrUpdateArrangor(any()) } just Runs
		coEvery { amtArrangorClient.getArrangor("88888888") } returns arrangor
	}

	@Test
	internal fun `lagreDeltakerliste - status GJENNOMFORES - lagres i db `() {
		val deltakerlisteId = UUID.randomUUID()
		val deltakerlisteDto =
			DeltakerlisteDto(
				id = deltakerlisteId,
				tiltakstype =
					DeltakerlisteDto.Tiltakstype(
						id = UUID.randomUUID(),
						navn = "Det flotte tiltaket",
						arenaKode = "DIGIOPPARB",
					),
				navn = "Gjennomføring av tiltak",
				startDato = LocalDate.now().minusYears(2),
				sluttDato = null,
				status = DeltakerlisteDto.Status.GJENNOMFORES,
				virksomhetsnummer = "88888888",
				oppstart = DeltakerlisteDto.Oppstartstype.LOPENDE,
				tilgjengeligForArrangorFraOgMedDato = null,
			)

		ingestService.lagreDeltakerliste(deltakerlisteId, deltakerlisteDto)

		verify(exactly = 1) { deltakerlisteRepository.insertOrUpdateDeltakerliste(any()) }
	}

	@Test
	internal fun `lagreDeltakerliste - status APENT_FOR_INNSOK - lagres som PLANLAGT`() {
		val deltakerlisteId = UUID.randomUUID()
		val deltakerlisteDto =
			DeltakerlisteDto(
				id = deltakerlisteId,
				tiltakstype =
					DeltakerlisteDto.Tiltakstype(
						id = UUID.randomUUID(),
						navn = "Det flotte tiltaket",
						arenaKode = "DIGIOPPARB",
					),
				navn = "Gjennomføring av tiltak",
				startDato = LocalDate.now().minusYears(2),
				sluttDato = null,
				status = DeltakerlisteDto.Status.APENT_FOR_INNSOK,
				virksomhetsnummer = "88888888",
				oppstart = DeltakerlisteDto.Oppstartstype.LOPENDE,
				tilgjengeligForArrangorFraOgMedDato = LocalDate.now().minusYears(2),
			)

		ingestService.lagreDeltakerliste(deltakerlisteId, deltakerlisteDto)

		verify(exactly = 1) { deltakerlisteRepository.insertOrUpdateDeltakerliste(match { it.status == DeltakerlisteStatus.PLANLAGT }) }
	}

	@Test
	internal fun `lagreDeltakerliste - status PLANLAGT - lagres`() {
		val deltakerlisteId = UUID.randomUUID()
		val deltakerlisteDto =
			DeltakerlisteDto(
				id = deltakerlisteId,
				tiltakstype =
					DeltakerlisteDto.Tiltakstype(
						id = UUID.randomUUID(),
						navn = "Det flotte tiltaket",
						arenaKode = "DIGIOPPARB",
					),
				navn = "Gjennomføring av tiltak",
				startDato = LocalDate.now().minusYears(2),
				sluttDato = null,
				status = DeltakerlisteDto.Status.PLANLAGT,
				virksomhetsnummer = "88888888",
				oppstart = DeltakerlisteDto.Oppstartstype.LOPENDE,
				tilgjengeligForArrangorFraOgMedDato = null,
			)

		ingestService.lagreDeltakerliste(deltakerlisteId, deltakerlisteDto)

		verify(exactly = 1) { deltakerlisteRepository.insertOrUpdateDeltakerliste(match { it.status == DeltakerlisteStatus.PLANLAGT }) }
	}

	@Test
	internal fun `lagreDeltakerliste - status AVSLUTTET for 6 mnd siden - lagres ikke`() {
		val deltakerlisteId = UUID.randomUUID()
		val deltakerlisteDto =
			DeltakerlisteDto(
				id = deltakerlisteId,
				tiltakstype =
					DeltakerlisteDto.Tiltakstype(
						id = UUID.randomUUID(),
						navn = "Det flotte tiltaket",
						arenaKode = "DIGIOPPARB",
					),
				navn = "Avsluttet tiltak",
				startDato = LocalDate.now().minusYears(2),
				sluttDato = LocalDate.now().minusMonths(6),
				status = DeltakerlisteDto.Status.AVSLUTTET,
				virksomhetsnummer = "88888888",
				oppstart = DeltakerlisteDto.Oppstartstype.LOPENDE,
				tilgjengeligForArrangorFraOgMedDato = null,
			)

		ingestService.lagreDeltakerliste(deltakerlisteId, deltakerlisteDto)

		verify(exactly = 0) { deltakerlisteRepository.insertOrUpdateDeltakerliste(any()) }
		verify(exactly = 1) { deltakerlisteRepository.deleteDeltakerlisteOgDeltakere(deltakerlisteId) }
	}

	@Test
	internal fun `lagreDeltakerliste - status AVSLUTTET for 1 uke siden - lagres i db`() {
		val deltakerlisteId = UUID.randomUUID()
		val deltakerlisteDto =
			DeltakerlisteDto(
				id = deltakerlisteId,
				tiltakstype =
					DeltakerlisteDto.Tiltakstype(
						id = UUID.randomUUID(),
						navn = "Det flotte tiltaket",
						arenaKode = "DIGIOPPARB",
					),
				navn = "Avsluttet tiltak",
				startDato = LocalDate.now().minusYears(2),
				sluttDato = LocalDate.now().minusWeeks(1),
				status = DeltakerlisteDto.Status.AVSLUTTET,
				virksomhetsnummer = "88888888",
				oppstart = DeltakerlisteDto.Oppstartstype.LOPENDE,
				tilgjengeligForArrangorFraOgMedDato = null,
			)

		ingestService.lagreDeltakerliste(deltakerlisteId, deltakerlisteDto)

		verify(exactly = 1) { deltakerlisteRepository.insertOrUpdateDeltakerliste(any()) }
	}

	@Test
	internal fun `lagreDeltakerliste - ikke stottet tiltakstype - lagres ikke i db `() {
		val deltakerlisteId = UUID.randomUUID()
		val deltakerlisteDto =
			DeltakerlisteDto(
				id = deltakerlisteId,
				tiltakstype =
					DeltakerlisteDto.Tiltakstype(
						id = UUID.randomUUID(),
						navn = "Det flotte tiltaket",
						arenaKode = "UTD",
					),
				navn = "Gjennomføring av tiltak",
				startDato = LocalDate.now().minusYears(2),
				sluttDato = null,
				status = DeltakerlisteDto.Status.GJENNOMFORES,
				virksomhetsnummer = "88888888",
				oppstart = DeltakerlisteDto.Oppstartstype.LOPENDE,
				tilgjengeligForArrangorFraOgMedDato = null,
			)

		ingestService.lagreDeltakerliste(deltakerlisteId, deltakerlisteDto)

		verify(exactly = 0) { deltakerlisteRepository.insertOrUpdateDeltakerliste(any()) }
	}

	@Test
	internal fun `lagreDeltaker - status DELTAR - lagres i db `() {
		every { deltakerRepository.getDeltaker(any()) } returns null
		val deltakerId = UUID.randomUUID()
		val deltakerDto =
			DeltakerDto(
				id = deltakerId,
				deltakerlisteId = UUID.randomUUID(),
				personalia =
					DeltakerPersonaliaDto(
						personident = "10987654321",
						navn = NavnDto("Fornavn", null, "Etternavn"),
						kontaktinformasjon = DeltakerKontaktinformasjonDto("98989898", "epost@nav.no"),
						skjermet = false,
						adresse = getAdresse(),
						adressebeskyttelse = null,
					),
				status =
					DeltakerStatusDto(
						type = DeltakerStatus.DELTAR,
						gyldigFra = LocalDate.now().minusWeeks(5).atStartOfDay(),
						opprettetDato = LocalDateTime.now().minusWeeks(6),
					),
				dagerPerUke = null,
				prosentStilling = null,
				oppstartsdato = LocalDate.now().minusWeeks(5),
				sluttdato = null,
				innsoktDato = LocalDate.now().minusMonths(2),
				bestillingTekst = "Bestilling",
				navKontor = "NAV Oslo",
				navVeileder = DeltakerNavVeilederDto(UUID.randomUUID(), "Per Veileder", null, null),
				deltarPaKurs = false,
				vurderingerFraArrangor = null,
			)

		ingestService.lagreDeltaker(deltakerId, deltakerDto)

		verify(exactly = 1) { deltakerRepository.insertOrUpdateDeltaker(any()) }
	}

	@Test
	internal fun `lagreDeltaker - status SOKT_INN - lagres ikke i db `() {
		every { deltakerRepository.getDeltaker(any()) } returns null
		val deltakerId = UUID.randomUUID()
		val deltakerDto =
			DeltakerDto(
				id = deltakerId,
				deltakerlisteId = UUID.randomUUID(),
				personalia =
					DeltakerPersonaliaDto(
						personident = "10987654321",
						navn = NavnDto("Fornavn", null, "Etternavn"),
						kontaktinformasjon = DeltakerKontaktinformasjonDto("98989898", "epost@nav.no"),
						skjermet = false,
						adresse = getAdresse(),
						adressebeskyttelse = null,
					),
				status =
					DeltakerStatusDto(
						type = DeltakerStatus.SOKT_INN,
						gyldigFra = LocalDate.now().minusWeeks(5).atStartOfDay(),
						opprettetDato = LocalDateTime.now().minusWeeks(6),
					),
				dagerPerUke = null,
				prosentStilling = null,
				oppstartsdato = LocalDate.now().minusWeeks(5),
				sluttdato = null,
				innsoktDato = LocalDate.now().minusMonths(2),
				bestillingTekst = "Bestilling",
				navKontor = "NAV Oslo",
				navVeileder = DeltakerNavVeilederDto(UUID.randomUUID(), "Per Veileder", null, null),
				deltarPaKurs = false,
				vurderingerFraArrangor = null,
			)

		ingestService.lagreDeltaker(deltakerId, deltakerDto)

		verify(exactly = 0) { deltakerRepository.insertOrUpdateDeltaker(any()) }
		verify(exactly = 1) { deltakerRepository.deleteDeltaker(deltakerId) }
	}

	@Test
	internal fun `lagreDeltaker - status HAR_SLUTTET for mer enn 40 dager siden - lagres ikke i db `() {
		every { deltakerRepository.getDeltaker(any()) } returns null
		val deltakerId = UUID.randomUUID()
		val deltakerDto =
			DeltakerDto(
				id = deltakerId,
				deltakerlisteId = UUID.randomUUID(),
				personalia =
					DeltakerPersonaliaDto(
						personident = "10987654321",
						navn = NavnDto("Fornavn", null, "Etternavn"),
						kontaktinformasjon = DeltakerKontaktinformasjonDto("98989898", "epost@nav.no"),
						skjermet = false,
						adresse = getAdresse(),
						adressebeskyttelse = null,
					),
				status =
					DeltakerStatusDto(
						type = DeltakerStatus.HAR_SLUTTET,
						gyldigFra = LocalDate.now().minusDays(43).atStartOfDay(),
						opprettetDato = LocalDateTime.now().minusWeeks(6),
					),
				dagerPerUke = null,
				prosentStilling = null,
				oppstartsdato = LocalDate.now().minusWeeks(5),
				sluttdato = null,
				innsoktDato = LocalDate.now().minusMonths(2),
				bestillingTekst = "Bestilling",
				navKontor = "NAV Oslo",
				navVeileder = DeltakerNavVeilederDto(UUID.randomUUID(), "Per Veileder", null, null),
				deltarPaKurs = false,
				vurderingerFraArrangor = null,
			)

		ingestService.lagreDeltaker(deltakerId, deltakerDto)

		verify(exactly = 0) { deltakerRepository.insertOrUpdateDeltaker(any()) }
		verify(exactly = 1) { deltakerRepository.deleteDeltaker(deltakerId) }
	}

	@Test
	internal fun `lagreDeltaker - status HAR_SLUTTET for mindre enn 40 dager siden - lagres i db `() {
		every { deltakerRepository.getDeltaker(any()) } returns null
		val deltakerId = UUID.randomUUID()
		val deltakerDto =
			DeltakerDto(
				id = deltakerId,
				deltakerlisteId = UUID.randomUUID(),
				personalia =
					DeltakerPersonaliaDto(
						personident = "10987654321",
						navn = NavnDto("Fornavn", null, "Etternavn"),
						kontaktinformasjon = DeltakerKontaktinformasjonDto("98989898", "epost@nav.no"),
						skjermet = false,
						adresse = getAdresse(),
						adressebeskyttelse = null,
					),
				status =
					DeltakerStatusDto(
						type = DeltakerStatus.HAR_SLUTTET,
						gyldigFra = LocalDate.now().minusDays(30).atStartOfDay(),
						opprettetDato = LocalDateTime.now().minusWeeks(6),
					),
				dagerPerUke = null,
				prosentStilling = null,
				oppstartsdato = LocalDate.now().minusWeeks(5),
				sluttdato = null,
				innsoktDato = LocalDate.now().minusMonths(2),
				bestillingTekst = "Bestilling",
				navKontor = "NAV Oslo",
				navVeileder = DeltakerNavVeilederDto(UUID.randomUUID(), "Per Veileder", null, null),
				deltarPaKurs = false,
				vurderingerFraArrangor = getVurderinger(deltakerId),
			)

		ingestService.lagreDeltaker(deltakerId, deltakerDto)

		verify(exactly = 1) { deltakerRepository.insertOrUpdateDeltaker(any()) }
	}

	@Test
	internal fun `lagreDeltaker - status IKKE_AKTUELL og deltar pa kurs og finnes ikke i db fra for - lagres ikke i db `() {
		every { deltakerRepository.getDeltaker(any()) } returns null
		val deltakerId = UUID.randomUUID()
		val deltakerDto =
			DeltakerDto(
				id = deltakerId,
				deltakerlisteId = UUID.randomUUID(),
				personalia =
					DeltakerPersonaliaDto(
						personident = "10987654321",
						navn = NavnDto("Fornavn", null, "Etternavn"),
						kontaktinformasjon = DeltakerKontaktinformasjonDto("98989898", "epost@nav.no"),
						skjermet = false,
						adresse = getAdresse(),
						adressebeskyttelse = null,
					),
				status =
					DeltakerStatusDto(
						type = DeltakerStatus.IKKE_AKTUELL,
						gyldigFra = LocalDate.now().minusWeeks(1).atStartOfDay(),
						opprettetDato = LocalDateTime.now().minusWeeks(6),
					),
				dagerPerUke = null,
				prosentStilling = null,
				oppstartsdato = LocalDate.now().minusWeeks(5),
				sluttdato = null,
				innsoktDato = LocalDate.now().minusMonths(2),
				bestillingTekst = "Bestilling",
				navKontor = "NAV Oslo",
				navVeileder = DeltakerNavVeilederDto(UUID.randomUUID(), "Per Veileder", null, null),
				deltarPaKurs = true,
				vurderingerFraArrangor = null,
			)

		ingestService.lagreDeltaker(deltakerId, deltakerDto)

		verify(exactly = 0) { deltakerRepository.insertOrUpdateDeltaker(any()) }
		verify(exactly = 1) { deltakerRepository.deleteDeltaker(deltakerId) }
	}

	@Test
	internal fun `lagreDeltaker - status IKKE_AKTUELL og deltar pa kurs og finnes i db fra for - lagres i db `() {
		val deltakerId = UUID.randomUUID()
		every { deltakerRepository.getDeltaker(any()) } returns getDeltaker(deltakerId)
		val deltakerDto =
			DeltakerDto(
				id = deltakerId,
				deltakerlisteId = UUID.randomUUID(),
				personalia =
					DeltakerPersonaliaDto(
						personident = "10987654321",
						navn = NavnDto("Fornavn", null, "Etternavn"),
						kontaktinformasjon = DeltakerKontaktinformasjonDto("98989898", "epost@nav.no"),
						skjermet = false,
						adresse = getAdresse(),
						adressebeskyttelse = null,
					),
				status =
					DeltakerStatusDto(
						type = DeltakerStatus.IKKE_AKTUELL,
						gyldigFra = LocalDate.now().minusWeeks(1).atStartOfDay(),
						opprettetDato = LocalDateTime.now().minusWeeks(6),
					),
				dagerPerUke = null,
				prosentStilling = null,
				oppstartsdato = LocalDate.now().minusWeeks(5),
				sluttdato = null,
				innsoktDato = LocalDate.now().minusMonths(2),
				bestillingTekst = "Bestilling",
				navKontor = "NAV Oslo",
				navVeileder = DeltakerNavVeilederDto(UUID.randomUUID(), "Per Veileder", null, null),
				deltarPaKurs = true,
				vurderingerFraArrangor = null,
			)

		ingestService.lagreDeltaker(deltakerId, deltakerDto)

		verify(exactly = 1) { deltakerRepository.insertOrUpdateDeltaker(any()) }
		verify(exactly = 0) { deltakerRepository.deleteDeltaker(deltakerId) }
	}

	@Test
	internal fun `lagreDeltaker - status IKKE_AKTUELL for mer enn 40 dager siden, deltar pa kurs, finnes i db fra for - lagres ikke i db `() {
		val deltakerId = UUID.randomUUID()
		every { deltakerRepository.getDeltaker(any()) } returns getDeltaker(deltakerId)
		val deltakerDto =
			DeltakerDto(
				id = deltakerId,
				deltakerlisteId = UUID.randomUUID(),
				personalia =
					DeltakerPersonaliaDto(
						personident = "10987654321",
						navn = NavnDto("Fornavn", null, "Etternavn"),
						kontaktinformasjon = DeltakerKontaktinformasjonDto("98989898", "epost@nav.no"),
						skjermet = false,
						adresse = getAdresse(),
						adressebeskyttelse = null,
					),
				status =
					DeltakerStatusDto(
						type = DeltakerStatus.IKKE_AKTUELL,
						gyldigFra = LocalDate.now().minusDays(43).atStartOfDay(),
						opprettetDato = LocalDateTime.now().minusWeeks(6),
					),
				dagerPerUke = null,
				prosentStilling = null,
				oppstartsdato = LocalDate.now().minusWeeks(5),
				sluttdato = null,
				innsoktDato = LocalDate.now().minusMonths(2),
				bestillingTekst = "Bestilling",
				navKontor = "NAV Oslo",
				navVeileder = DeltakerNavVeilederDto(UUID.randomUUID(), "Per Veileder", null, null),
				deltarPaKurs = true,
				vurderingerFraArrangor = null,
			)

		ingestService.lagreDeltaker(deltakerId, deltakerDto)

		verify(exactly = 0) { deltakerRepository.insertOrUpdateDeltaker(any()) }
		verify(exactly = 1) { deltakerRepository.deleteDeltaker(deltakerId) }
	}

	@Test
	internal fun `lagreDeltaker - status HAR_SLUTTET mindre enn 40 dager siden, sluttdato mer enn 40 dager siden - lagres ikke i db `() {
		every { deltakerRepository.getDeltaker(any()) } returns null
		val deltakerId = UUID.randomUUID()
		val deltakerDto =
			DeltakerDto(
				id = deltakerId,
				deltakerlisteId = UUID.randomUUID(),
				personalia =
					DeltakerPersonaliaDto(
						personident = "10987654321",
						navn = NavnDto("Fornavn", null, "Etternavn"),
						kontaktinformasjon = DeltakerKontaktinformasjonDto("98989898", "epost@nav.no"),
						skjermet = false,
						adresse = getAdresse(),
						adressebeskyttelse = null,
					),
				status =
					DeltakerStatusDto(
						type = DeltakerStatus.HAR_SLUTTET,
						gyldigFra = LocalDate.now().minusWeeks(1).atStartOfDay(),
						opprettetDato = LocalDateTime.now().minusWeeks(6),
					),
				dagerPerUke = null,
				prosentStilling = null,
				oppstartsdato = LocalDate.now().minusWeeks(5),
				sluttdato = LocalDate.now().minusDays(50),
				innsoktDato = LocalDate.now().minusMonths(2),
				bestillingTekst = "Bestilling",
				navKontor = "NAV Oslo",
				navVeileder = DeltakerNavVeilederDto(UUID.randomUUID(), "Per Veileder", null, null),
				deltarPaKurs = false,
				vurderingerFraArrangor = null,
			)

		ingestService.lagreDeltaker(deltakerId, deltakerDto)

		verify(exactly = 0) { deltakerRepository.insertOrUpdateDeltaker(any()) }
		verify(exactly = 1) { deltakerRepository.deleteDeltaker(deltakerId) }
	}

	@Test
	internal fun `lagreDeltaker - har adressebeskyttelse - lagres ikke i db `() {
		every { deltakerRepository.getDeltaker(any()) } returns null
		val deltakerId = UUID.randomUUID()
		val deltakerDto =
			DeltakerDto(
				id = deltakerId,
				deltakerlisteId = UUID.randomUUID(),
				personalia =
					DeltakerPersonaliaDto(
						personident = "10987654321",
						navn = NavnDto("Fornavn", null, "Etternavn"),
						kontaktinformasjon = DeltakerKontaktinformasjonDto("98989898", "epost@nav.no"),
						skjermet = false,
						adresse = getAdresse(),
						adressebeskyttelse = "STRENGT_FORTROLIG",
					),
				status =
					DeltakerStatusDto(
						type = DeltakerStatus.DELTAR,
						gyldigFra = LocalDate.now().minusWeeks(5).atStartOfDay(),
						opprettetDato = LocalDateTime.now().minusWeeks(6),
					),
				dagerPerUke = null,
				prosentStilling = null,
				oppstartsdato = LocalDate.now().minusWeeks(5),
				sluttdato = null,
				innsoktDato = LocalDate.now().minusMonths(2),
				bestillingTekst = "Bestilling",
				navKontor = "NAV Oslo",
				navVeileder = DeltakerNavVeilederDto(UUID.randomUUID(), "Per Veileder", null, null),
				deltarPaKurs = false,
				vurderingerFraArrangor = null,
			)

		ingestService.lagreDeltaker(deltakerId, deltakerDto)

		verify(exactly = 0) { deltakerRepository.insertOrUpdateDeltaker(any()) }
		verify(exactly = 1) { deltakerRepository.deleteDeltaker(deltakerId) }
	}

	@Test
	internal fun `lagreDeltaker - skjult, ny status DELTAR - fjerner skjuling i db `() {
		val deltakerId = UUID.randomUUID()
		val opprinneligDeltaker =
			getDeltaker(deltakerId).copy(
				status = StatusType.HAR_SLUTTET,
				skjultDato = LocalDateTime.now(),
				skjultAvAnsattId = UUID.randomUUID(),
			)
		every { deltakerRepository.getDeltaker(any()) } returns opprinneligDeltaker
		val deltakerDto =
			DeltakerDto(
				id = deltakerId,
				deltakerlisteId = UUID.randomUUID(),
				personalia =
					DeltakerPersonaliaDto(
						personident = "10987654321",
						navn = NavnDto("Fornavn", null, "Etternavn"),
						kontaktinformasjon = DeltakerKontaktinformasjonDto("98989898", "epost@nav.no"),
						skjermet = false,
						adresse = getAdresse(),
						adressebeskyttelse = null,
					),
				status =
					DeltakerStatusDto(
						type = DeltakerStatus.DELTAR,
						gyldigFra = LocalDate.now().minusWeeks(3).atStartOfDay(),
						opprettetDato = LocalDateTime.now().minusWeeks(6),
					),
				dagerPerUke = null,
				prosentStilling = null,
				oppstartsdato = LocalDate.now().minusWeeks(5),
				sluttdato = null,
				innsoktDato = LocalDate.now().minusMonths(2),
				bestillingTekst = "Bestilling",
				navKontor = "NAV Oslo",
				navVeileder = DeltakerNavVeilederDto(UUID.randomUUID(), "Per Veileder", null, null),
				deltarPaKurs = true,
				vurderingerFraArrangor = null,
			)

		ingestService.lagreDeltaker(deltakerId, deltakerDto)

		verify(exactly = 1) { deltakerRepository.insertOrUpdateDeltaker(match { it.skjultDato == null && it.skjultAvAnsattId == null }) }
	}

	@Test
	internal fun `lagreDeltaker - skjult, samme status - beholder skjuling i db `() {
		val deltakerId = UUID.randomUUID()
		val skjultDato = LocalDateTime.now().minusDays(2)
		val skjultAvAnsattId = UUID.randomUUID()
		val opprinneligDeltaker =
			getDeltaker(deltakerId).copy(
				status = StatusType.HAR_SLUTTET,
				skjultDato = skjultDato,
				skjultAvAnsattId = skjultAvAnsattId,
			)
		every { deltakerRepository.getDeltaker(any()) } returns opprinneligDeltaker
		val deltakerDto =
			DeltakerDto(
				id = deltakerId,
				deltakerlisteId = UUID.randomUUID(),
				personalia =
					DeltakerPersonaliaDto(
						personident = "10987654321",
						navn = NavnDto("Fornavn", null, "Etternavn"),
						kontaktinformasjon = DeltakerKontaktinformasjonDto("98989898", "epost@nav.no"),
						skjermet = false,
						adresse = getAdresse(),
						adressebeskyttelse = null,
					),
				status =
					DeltakerStatusDto(
						type = DeltakerStatus.HAR_SLUTTET,
						gyldigFra = LocalDate.now().minusWeeks(1).atStartOfDay(),
						opprettetDato = LocalDateTime.now().minusWeeks(1),
					),
				dagerPerUke = null,
				prosentStilling = null,
				oppstartsdato = LocalDate.now().minusWeeks(5),
				sluttdato = LocalDate.now(),
				innsoktDato = LocalDate.now().minusMonths(2),
				bestillingTekst = "Bestilling",
				navKontor = "NAV Oslo",
				navVeileder = DeltakerNavVeilederDto(UUID.randomUUID(), "Per Veileder", null, null),
				deltarPaKurs = true,
				vurderingerFraArrangor = null,
			)

		ingestService.lagreDeltaker(deltakerId, deltakerDto)

		verify(exactly = 1) {
			deltakerRepository.insertOrUpdateDeltaker(
				match {
					it.skjultDato?.toLocalDate() == skjultDato.toLocalDate() && it.skjultAvAnsattId == skjultAvAnsattId
				},
			)
		}
	}

	@Test
	internal fun `lagreEndringsmelding - status AKTIV - lagres i db `() {
		val endringsmeldingId = UUID.randomUUID()
		val endringsmeldingDto =
			EndringsmeldingDto(
				id = endringsmeldingId,
				deltakerId = UUID.randomUUID(),
				utfortAvNavAnsattId = null,
				opprettetAvArrangorAnsattId = UUID.randomUUID(),
				utfortTidspunkt = null,
				status = Endringsmelding.Status.AKTIV,
				type = EndringsmeldingType.ENDRE_SLUTTDATO,
				innhold = Innhold.EndreSluttdatoInnhold(sluttdato = LocalDate.now().plusWeeks(3)),
				createdAt = LocalDateTime.now(),
			)

		ingestService.lagreEndringsmelding(endringsmeldingId, endringsmeldingDto)

		verify(exactly = 1) { endringsmeldingRepository.insertOrUpdateEndringsmelding(any()) }
	}

	@Test
	internal fun `lagreEndringsmelding - status UTDATERT - lagres i db `() {
		val endringsmeldingId = UUID.randomUUID()
		val endringsmeldingDto =
			EndringsmeldingDto(
				id = endringsmeldingId,
				deltakerId = UUID.randomUUID(),
				utfortAvNavAnsattId = null,
				opprettetAvArrangorAnsattId = UUID.randomUUID(),
				utfortTidspunkt = null,
				status = Endringsmelding.Status.UTDATERT,
				type = EndringsmeldingType.ENDRE_SLUTTDATO,
				innhold = Innhold.EndreSluttdatoInnhold(sluttdato = LocalDate.now().plusWeeks(3)),
				createdAt = LocalDateTime.now(),
			)

		ingestService.lagreEndringsmelding(endringsmeldingId, endringsmeldingDto)

		verify(exactly = 1) { endringsmeldingRepository.insertOrUpdateEndringsmelding(any()) }
	}
}
