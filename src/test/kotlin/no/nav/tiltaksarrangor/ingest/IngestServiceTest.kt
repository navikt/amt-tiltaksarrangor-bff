package no.nav.tiltaksarrangor.ingest

import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.amt.lib.models.arrangor.melding.EndringFraArrangor
import no.nav.amt.lib.models.arrangor.melding.Forslag
import no.nav.amt.lib.models.deltaker.Deltakelsesinnhold
import no.nav.amt.lib.models.deltaker.DeltakerHistorikk
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
import no.nav.tiltaksarrangor.melding.forslag.ForslagService
import no.nav.tiltaksarrangor.melding.forslag.forlengDeltakelseForslag
import no.nav.tiltaksarrangor.model.DeltakerStatusAarsak
import no.nav.tiltaksarrangor.model.Endringsmelding
import no.nav.tiltaksarrangor.model.Kilde
import no.nav.tiltaksarrangor.model.StatusType
import no.nav.tiltaksarrangor.repositories.AnsattRepository
import no.nav.tiltaksarrangor.repositories.ArrangorRepository
import no.nav.tiltaksarrangor.repositories.DeltakerRepository
import no.nav.tiltaksarrangor.repositories.DeltakerlisteRepository
import no.nav.tiltaksarrangor.repositories.EndringsmeldingRepository
import no.nav.tiltaksarrangor.repositories.UlestEndringRepository
import no.nav.tiltaksarrangor.service.NavAnsattService
import no.nav.tiltaksarrangor.service.NavEnhetService
import no.nav.tiltaksarrangor.testutils.getAdresse
import no.nav.tiltaksarrangor.testutils.getDeltaker
import no.nav.tiltaksarrangor.testutils.getNavAnsatt
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
	private val navAnsattService = mockk<NavAnsattService>(relaxUnitFun = true)
	private val navEnhetService = mockk<NavEnhetService>(relaxUnitFun = true)
	private val forslagService = mockk<ForslagService>(relaxUnitFun = true)
	private val ulestEndringRepository = mockk<UlestEndringRepository>()
	private val ingestService =
		IngestService(
			arrangorRepository,
			ansattRepository,
			deltakerlisteRepository,
			deltakerRepository,
			endringsmeldingRepository,
			amtArrangorClient,
			forslagService,
			navEnhetService,
			navAnsattService,
			ulestEndringRepository,
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
		every { ulestEndringRepository.insert(any(), any()) } returns mockk()
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
	internal fun `lagreDeltaker - status DELTAR - lagres i db `(): Unit = runBlocking {
		with(DeltakerDtoCtx()) {
			medStatus(DeltakerStatus.DELTAR)
			every { deltakerRepository.getDeltaker(any()) } returns null
			ingestService.lagreDeltaker(deltakerDto.id, deltakerDto)

			verify(exactly = 1) { deltakerRepository.insertOrUpdateDeltaker(any()) }
		}
	}

	@Test
	internal fun `lagreDeltaker - status SOKT_INN - lagres ikke i db `(): Unit = runBlocking {
		with(DeltakerDtoCtx()) {
			medStatus(DeltakerStatus.SOKT_INN)
			every { deltakerRepository.getDeltaker(any()) } returns null
			ingestService.lagreDeltaker(deltakerDto.id, deltakerDto)

			verify(exactly = 0) { deltakerRepository.insertOrUpdateDeltaker(any()) }
			verify(exactly = 1) { deltakerRepository.deleteDeltaker(deltakerDto.id) }
		}
	}

	@Test
	internal fun `lagreDeltaker - status HAR_SLUTTET for mer enn 40 dager siden - lagres ikke i db `(): Unit = runBlocking {
		with(DeltakerDtoCtx()) {
			medStatus(DeltakerStatus.HAR_SLUTTET, 41)
			every { deltakerRepository.getDeltaker(any()) } returns null
			ingestService.lagreDeltaker(deltakerDto.id, deltakerDto)
			verify(exactly = 0) { deltakerRepository.insertOrUpdateDeltaker(any()) }
			verify(exactly = 1) { deltakerRepository.deleteDeltaker(deltakerDto.id) }
		}
	}

	@Test
	internal fun `lagreDeltaker - status HAR_SLUTTET for mindre enn 40 dager siden - lagres i db `(): Unit = runBlocking {
		with(DeltakerDtoCtx()) {
			medStatus(DeltakerStatus.HAR_SLUTTET, 39)
			every { deltakerRepository.getDeltaker(any()) } returns null
			ingestService.lagreDeltaker(deltakerDto.id, deltakerDto)

			verify(exactly = 1) { deltakerRepository.insertOrUpdateDeltaker(any()) }
		}
	}

	@Test
	internal fun `lagreDeltaker - status IKKE_AKTUELL og deltar pa kurs og finnes ikke i db fra for - lagres ikke i db `(): Unit =
		runBlocking {
			with(DeltakerDtoCtx()) {
				every { deltakerRepository.getDeltaker(any()) } returns null
				medStatus(DeltakerStatus.IKKE_AKTUELL)
				medDeltarPaKurs()
				ingestService.lagreDeltaker(deltakerDto.id, deltakerDto)

				verify(exactly = 0) { deltakerRepository.insertOrUpdateDeltaker(any()) }
				verify(exactly = 1) { deltakerRepository.deleteDeltaker(deltakerDto.id) }
			}
		}

	@Test
	internal fun `lagreDeltaker - status IKKE_AKTUELL og deltar pa kurs og finnes i db fra for - lagres i db `(): Unit = runBlocking {
		with(DeltakerDtoCtx()) {
			medStatus(DeltakerStatus.IKKE_AKTUELL)
			medDeltarPaKurs()
			every { deltakerRepository.getDeltaker(any()) } returns getDeltaker(deltakerDto.id)
			ingestService.lagreDeltaker(deltakerDto.id, deltakerDto)

			verify(exactly = 1) { deltakerRepository.insertOrUpdateDeltaker(any()) }
			verify(exactly = 0) { deltakerRepository.deleteDeltaker(deltakerDto.id) }
		}
	}

	@Test
	internal fun `lagreDeltaker - status IKKE_AKTUELL for mer enn 40 dager siden, deltar pa kurs, finnes i db - lagres ikke i db `(): Unit =
		runBlocking {
			with(DeltakerDtoCtx()) {
				medStatus(DeltakerStatus.IKKE_AKTUELL, 42)
				medDeltarPaKurs()
				every { deltakerRepository.getDeltaker(any()) } returns getDeltaker(deltakerDto.id)

				ingestService.lagreDeltaker(deltakerDto.id, deltakerDto)

				verify(exactly = 0) { deltakerRepository.insertOrUpdateDeltaker(any()) }
				verify(exactly = 1) { deltakerRepository.deleteDeltaker(deltakerDto.id) }
			}
		}

	@Test
	internal fun `lagreDeltaker - status HAR_SLUTTET mindre enn 40 dager siden, sluttdato mer enn 40 dager - lagres ikke i db `(): Unit =
		runBlocking {
			with(DeltakerDtoCtx()) {
				medStatus(DeltakerStatus.HAR_SLUTTET, gyldigFraDagerSiden = 39)
				medSluttdato(dagerSiden = 41)
				every { deltakerRepository.getDeltaker(any()) } returns null
				ingestService.lagreDeltaker(deltakerDto.id, deltakerDto)

				verify(exactly = 0) { deltakerRepository.insertOrUpdateDeltaker(any()) }
				verify(exactly = 1) { deltakerRepository.deleteDeltaker(deltakerDto.id) }
			}
		}

	@Test
	internal fun `lagreDeltaker - har adressebeskyttelse - lagres i db `(): Unit = runBlocking {
		with(DeltakerDtoCtx()) {
			medAdressebeskyttelse()
			every { deltakerRepository.getDeltaker(any()) } returns null
			ingestService.lagreDeltaker(deltakerDto.id, deltakerDto)

			verify(exactly = 1) { deltakerRepository.insertOrUpdateDeltaker(any()) }
		}
	}

	@Test
	internal fun `lagreDeltaker - skjult, ny status DELTAR - fjerner skjuling i db `(): Unit = runBlocking {
		with(DeltakerDtoCtx()) {
			medStatus(DeltakerStatus.DELTAR)

			val opprinneligDeltaker =
				getDeltaker(deltakerDto.id).copy(
					status = StatusType.HAR_SLUTTET,
					skjultDato = LocalDateTime.now(),
					skjultAvAnsattId = UUID.randomUUID(),
				)
			every { deltakerRepository.getDeltaker(any()) } returns opprinneligDeltaker

			ingestService.lagreDeltaker(deltakerDto.id, deltakerDto)

			verify(exactly = 1) { deltakerRepository.insertOrUpdateDeltaker(match { it.skjultDato == null && it.skjultAvAnsattId == null }) }
		}
	}

	@Test
	internal fun `lagreDeltaker - skjult, samme status - beholder skjuling i db `(): Unit = runBlocking {
		with(DeltakerDtoCtx()) {
			medStatus(DeltakerStatus.HAR_SLUTTET)

			val skjultDato = LocalDateTime.now().minusDays(2)
			val skjultAvAnsattId = UUID.randomUUID()
			val opprinneligDeltaker =
				getDeltaker(deltakerDto.id).copy(
					status = StatusType.HAR_SLUTTET,
					skjultDato = skjultDato,
					skjultAvAnsattId = skjultAvAnsattId,
				)
			every { deltakerRepository.getDeltaker(any()) } returns opprinneligDeltaker

			ingestService.lagreDeltaker(deltakerDto.id, deltakerDto)

			verify(exactly = 1) {
				deltakerRepository.insertOrUpdateDeltaker(
					match {
						it.skjultDato?.toLocalDate() == skjultDato.toLocalDate() && it.skjultAvAnsattId == skjultAvAnsattId
					},
				)
			}
		}
	}

	@Test
	internal fun `lagreDeltaker - historikk inneholder svar på forslag som ikke finnes i db - lagrer ulest endring i db `(): Unit =
		runBlocking {
			with(DeltakerDtoCtx()) {
				val lagretDeltaker = getDeltaker(deltakerDto.id)
				val forslag = forlengDeltakelseForslag(
					status = Forslag.Status.Avvist(
						Forslag.NavAnsatt(
							UUID.randomUUID(),
							UUID.randomUUID(),
						),
						LocalDateTime.now(),
						"Fordi...",
					),
				)
				val nyDeltaker = deltakerDto.copy(historikk = listOf(DeltakerHistorikk.Forslag(forslag)))
				every { deltakerRepository.getDeltaker(any()) } returns lagretDeltaker
				every { navEnhetService.hentOpprettEllerOppdaterNavEnhet(any()) } returns mockk()
				every { navAnsattService.hentEllerOpprettNavAnsatt(any()) } returns mockk()
				ingestService.lagreDeltaker(nyDeltaker.id, nyDeltaker)

				verify(exactly = 1) { ulestEndringRepository.insert(any(), any()) }
			}
		}

	@Test
	internal fun `lagreDeltaker - historikk inneholder endring fra arrangør - lagrer ikke i db `(): Unit = runBlocking {
		with(DeltakerDtoCtx()) {
			val lagretDeltaker = getDeltaker(deltakerDto.id)
			val endringFraArrangor = DeltakerHistorikk.EndringFraArrangor(
				EndringFraArrangor(
					id = UUID.randomUUID(),
					deltakerId = lagretDeltaker.id,
					opprettetAvArrangorAnsattId = UUID.randomUUID(),
					opprettet = LocalDate.of(2023, 1, 1).atStartOfDay(),
					endring = EndringFraArrangor.LeggTilOppstartsdato(
						startdato = LocalDate.of(2023, 2, 1),
						sluttdato = null,
					),
				),
			)
			val nyDeltaker = deltakerDto.copy(historikk = listOf(endringFraArrangor))
			every { deltakerRepository.getDeltaker(any()) } returns lagretDeltaker
			every { navEnhetService.hentOpprettEllerOppdaterNavEnhet(any()) } returns mockk()
			every { navAnsattService.hentEllerOpprettNavAnsatt(any()) } returns mockk()
			ingestService.lagreDeltaker(nyDeltaker.id, nyDeltaker)

			verify(exactly = 0) { ulestEndringRepository.insert(any(), any()) }
		}
	}

	@Test
	internal fun `lagreDeltaker - historikk inneholder svar på forslag som  finnes i db - lagrer ikke endring i db `(): Unit = runBlocking {
		val forslag = DeltakerHistorikk.Forslag(
			forlengDeltakelseForslag(
				status = Forslag.Status.Godkjent(
					Forslag.NavAnsatt(
						UUID.randomUUID(),
						UUID.randomUUID(),
					),
					LocalDateTime.now(),
				),
			),
		)

		with(DeltakerDtoCtx()) {
			val lagretDeltaker = getDeltaker(deltakerDto.id).copy(
				historikk = listOf(
					forslag,
				),
			)

			val nyDeltaker = deltakerDto.copy(historikk = listOf(forslag))
			every { deltakerRepository.getDeltaker(any()) } returns lagretDeltaker
			every { navEnhetService.hentOpprettEllerOppdaterNavEnhet(any()) } returns mockk()
			every { navAnsattService.hentEllerOpprettNavAnsatt(any()) } returns mockk()
			ingestService.lagreDeltaker(nyDeltaker.id, nyDeltaker)

			verify(exactly = 0) { ulestEndringRepository.insert(any(), any()) }
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

	@Test
	internal fun `lagreNavAnsatt - ny ansatt - lagres`() {
		val navAnsatt = getNavAnsatt()

		ingestService.lagreNavAnsatt(navAnsatt.id, navAnsatt)

		verify(exactly = 1) { navAnsattService.upsert(navAnsatt) }
	}

	@Test
	internal fun `handleForslag - forslaget er aktivt - gjør ingenting`() {
		val forslag = forlengDeltakelseForslag()

		ingestService.handleMelding(forslag.id, forslag)

		verify(exactly = 0) { forslagService.delete(forslag.id) }
	}

	@Test
	internal fun `handleForslag - forslaget er tilbakekalt - gjør ingenting`() {
		val forslag = forlengDeltakelseForslag(
			status = Forslag.Status.Tilbakekalt(
				UUID.randomUUID(),
				LocalDateTime.now(),
			),
		)

		ingestService.handleMelding(forslag.id, forslag)

		verify(exactly = 0) { forslagService.delete(forslag.id) }
	}

	@Test
	internal fun `handleForslag - forslaget er godkjent - sletter`() {
		val forslag = forlengDeltakelseForslag(
			status = Forslag.Status.Godkjent(
				Forslag.NavAnsatt(
					UUID.randomUUID(),
					UUID.randomUUID(),
				),
				LocalDateTime.now(),
			),
		)

		ingestService.handleMelding(forslag.id, forslag)

		verify(exactly = 1) { forslagService.delete(forslag.id) }
	}

	@Test
	internal fun `handleForslag - forslaget er avvist - sletter`() {
		val forslag = forlengDeltakelseForslag(
			status = Forslag.Status.Avvist(
				Forslag.NavAnsatt(
					UUID.randomUUID(),
					UUID.randomUUID(),
				),
				LocalDateTime.now(),
				"Fordi...",
			),
		)

		ingestService.handleMelding(forslag.id, forslag)

		verify(exactly = 1) { forslagService.delete(forslag.id) }
	}
}

class DeltakerDtoCtx {
	var deltakerDto = DeltakerDto(
		id = UUID.randomUUID(),
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
				gyldigFra = LocalDate.now().minusWeeks(1).atStartOfDay(),
				opprettetDato = LocalDateTime.now().minusWeeks(1),
				aarsak = null,
				aarsaksbeskrivelse = null,
			),
		dagerPerUke = null,
		prosentStilling = null,
		oppstartsdato = LocalDate.now().minusWeeks(5),
		sluttdato = LocalDate.now(),
		innsoktDato = LocalDate.now().minusMonths(2),
		bestillingTekst = "Bestilling",
		navKontor = "NAV Oslo",
		navVeileder = DeltakerNavVeilederDto(UUID.randomUUID(), "Per Veileder", null, null),
		deltarPaKurs = false,
		vurderingerFraArrangor = null,
		innhold = Deltakelsesinnhold(
			ledetekst = "Innholdsledetekst...",
			innhold = listOf(
				no.nav.amt.lib.models.deltaker.Innhold(
					tekst = "tekst",
					innholdskode = "kode",
					valgt = true,
					beskrivelse = "beskrivelse",
				),
			),
		),
		kilde = Kilde.ARENA,
		historikk = null, // vedtak?
		sistEndret = LocalDateTime.now(),
		forsteVedtakFattet = LocalDate.now().minusMonths(2),
	)

	fun medSluttdato(dagerSiden: Long) {
		deltakerDto = deltakerDto.copy(
			sluttdato = LocalDate.now().minusDays(dagerSiden),
		)
	}

	fun medAdressebeskyttelse() {
		deltakerDto = deltakerDto.copy(
			personalia = deltakerDto.personalia.copy(adressebeskyttelse = "STRENGT_FORTROLIG"),
		)
	}

	fun medDeltarPaKurs() {
		deltakerDto = deltakerDto.copy(deltarPaKurs = true)
	}

	fun medStatus(
		type: DeltakerStatus,
		gyldigFraDagerSiden: Long = 1L,
		aarsak: DeltakerStatusAarsak.Type? = null,
		aarsakbeskrivelse: String? = null,
	) {
		deltakerDto = deltakerDto.copy(
			status = DeltakerStatusDto(
				type = type,
				gyldigFra = LocalDate.now().minusDays(gyldigFraDagerSiden).atStartOfDay(),
				opprettetDato = LocalDateTime.now().minusDays(gyldigFraDagerSiden),
				aarsak = aarsak,
				aarsaksbeskrivelse = aarsakbeskrivelse,
			),
		)
	}

	fun medVurderinger() {
		deltakerDto = deltakerDto.copy(vurderingerFraArrangor = getVurderinger(deltakerDto.id))
	}
}
