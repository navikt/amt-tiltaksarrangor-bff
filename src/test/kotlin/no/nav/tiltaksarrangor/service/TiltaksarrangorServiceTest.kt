package no.nav.tiltaksarrangor.service

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import no.nav.tiltaksarrangor.client.amtarrangor.AmtArrangorClient
import no.nav.tiltaksarrangor.client.amttiltak.AmtTiltakClient
import no.nav.tiltaksarrangor.controller.request.RegistrerVurderingRequest
import no.nav.tiltaksarrangor.ingest.model.AdresseDto
import no.nav.tiltaksarrangor.ingest.model.AnsattRolle
import no.nav.tiltaksarrangor.ingest.model.Bostedsadresse
import no.nav.tiltaksarrangor.ingest.model.EndringsmeldingType
import no.nav.tiltaksarrangor.ingest.model.Innhold
import no.nav.tiltaksarrangor.ingest.model.Kontaktadresse
import no.nav.tiltaksarrangor.ingest.model.Matrikkeladresse
import no.nav.tiltaksarrangor.ingest.model.Oppholdsadresse
import no.nav.tiltaksarrangor.ingest.model.Postboksadresse
import no.nav.tiltaksarrangor.ingest.model.Vegadresse
import no.nav.tiltaksarrangor.ingest.model.VurderingDto
import no.nav.tiltaksarrangor.model.Adressetype
import no.nav.tiltaksarrangor.model.Endringsmelding
import no.nav.tiltaksarrangor.model.StatusType
import no.nav.tiltaksarrangor.model.Veiledertype
import no.nav.tiltaksarrangor.model.Vurderingstype
import no.nav.tiltaksarrangor.model.exceptions.SkjultDeltakerException
import no.nav.tiltaksarrangor.model.exceptions.UnauthorizedException
import no.nav.tiltaksarrangor.model.exceptions.ValidationException
import no.nav.tiltaksarrangor.repositories.AnsattRepository
import no.nav.tiltaksarrangor.repositories.DeltakerRepository
import no.nav.tiltaksarrangor.repositories.DeltakerlisteRepository
import no.nav.tiltaksarrangor.repositories.EndringsmeldingRepository
import no.nav.tiltaksarrangor.repositories.model.AnsattDbo
import no.nav.tiltaksarrangor.repositories.model.AnsattRolleDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerMedDeltakerlisteDbo
import no.nav.tiltaksarrangor.repositories.model.EndringsmeldingDbo
import no.nav.tiltaksarrangor.repositories.model.KoordinatorDeltakerlisteDbo
import no.nav.tiltaksarrangor.repositories.model.VeilederDeltakerDbo
import no.nav.tiltaksarrangor.testutils.DbTestDataUtils
import no.nav.tiltaksarrangor.testutils.SingletonPostgresContainer
import no.nav.tiltaksarrangor.testutils.getDeltaker
import no.nav.tiltaksarrangor.testutils.getDeltakerliste
import no.nav.tiltaksarrangor.unleash.UnleashService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class TiltaksarrangorServiceTest {
	private val amtTiltakClient = mockk<AmtTiltakClient>()
	private val amtArrangorClient = mockk<AmtArrangorClient>()
	private val metricsService = mockk<MetricsService>(relaxed = true)
	private val auditLoggerService = mockk<AuditLoggerService>(relaxed = true)
	private val unleashService = mockk<UnleashService>()
	private val dataSource = SingletonPostgresContainer.getDataSource()
	private val template = NamedParameterJdbcTemplate(dataSource)
	private val ansattRepository = AnsattRepository(template)
	private val ansattService = AnsattService(amtArrangorClient, ansattRepository)
	private val deltakerRepository = DeltakerRepository(template)
	private val deltakerlisteRepository = DeltakerlisteRepository(template, deltakerRepository)
	private val endringsmeldingRepository = EndringsmeldingRepository(template)
	private val tiltaksarrangorService = TiltaksarrangorService(
		amtTiltakClient,
		ansattService,
		metricsService,
		deltakerRepository,
		endringsmeldingRepository,
		auditLoggerService,
		unleashService
	)

	@AfterEach
	internal fun tearDown() {
		DbTestDataUtils.cleanDatabase(dataSource)
		clearMocks(auditLoggerService, amtTiltakClient)
	}

	@Test
	fun `getDeltaker - ansatt har ikke rolle hos arrangor - returnerer unauthorized`() {
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
				deltakerlister = emptyList(),
				veilederDeltakere = emptyList()
			)
		)

		assertThrows<UnauthorizedException> {
			tiltaksarrangorService.getDeltaker(personIdent, deltakerId)
		}
	}

	@Test
	fun `getDeltaker - deltaker er skjult - returnerer skjult deltaker exception`() {
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.randomUUID()
		val deltaker = getDeltaker(deltakerId, deltakerliste.id).copy(skjultDato = LocalDateTime.now(), skjultAvAnsattId = UUID.randomUUID())
		deltakerRepository.insertOrUpdateDeltaker(deltaker)
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

		assertThrows<SkjultDeltakerException> {
			tiltaksarrangorService.getDeltaker(personIdent, deltakerId)
		}
	}

	@Test
	fun `getDeltaker - deltaker finnes og ansatt har tilgang - returnerer deltaker`() {
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.randomUUID()
		deltakerRepository.insertOrUpdateDeltaker(
			getDeltaker(deltakerId, deltakerliste.id).copy(
				vurderingerFraArrangor = listOf(
					VurderingDto(
						id = UUID.randomUUID(),
						deltakerId = deltakerId,
						vurderingstype = Vurderingstype.OPPFYLLER_IKKE_KRAVENE,
						begrunnelse = "Mangler førerkort",
						opprettetAvArrangorAnsattId = UUID.randomUUID(),
						gyldigFra = LocalDateTime.now().minusWeeks(2),
						gyldigTil = LocalDateTime.now()
					),
					VurderingDto(
						id = UUID.randomUUID(),
						deltakerId = deltakerId,
						vurderingstype = Vurderingstype.OPPFYLLER_KRAVENE,
						begrunnelse = null,
						opprettetAvArrangorAnsattId = UUID.randomUUID(),
						gyldigFra = LocalDateTime.now(),
						gyldigTil = null
					)
				)
			)
		)
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

		val deltaker = tiltaksarrangorService.getDeltaker(personIdent, deltakerId)

		deltaker.id shouldBe deltakerId
		deltaker.deltakerliste.id shouldBe deltakerliste.id
		deltaker.dagerPerUke shouldBe null
		deltaker.soktInnPa shouldBe deltakerliste.navn
		deltaker.tiltakskode shouldBe deltakerliste.tiltakType
		deltaker.aktiveEndringsmeldinger.size shouldBe 0
		deltaker.veiledere.size shouldBe 0
		deltaker.adresse?.adressetype shouldBe Adressetype.KONTAKTADRESSE
		deltaker.adresse?.postnummer shouldBe "1234"
		deltaker.adresse?.poststed shouldBe "MOSS"
		deltaker.adresse?.tilleggsnavn shouldBe null
		deltaker.adresse?.adressenavn shouldBe "Gate 1"
		deltaker.gjeldendeVurderingFraArrangor?.vurderingstype shouldBe Vurderingstype.OPPFYLLER_KRAVENE
		deltaker.historiskeVurderingerFraArrangor?.size shouldBe 1
		deltaker.historiskeVurderingerFraArrangor?.firstOrNull()?.vurderingstype shouldBe Vurderingstype.OPPFYLLER_IKKE_KRAVENE
	}

	@Test
	fun `getDeltaker - deltaker som har veiledere og endringsmeldinger finnes og ansatt har tilgang - returnerer deltaker`() {
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.randomUUID()
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId, deltakerliste.id))
		endringsmeldingRepository.insertOrUpdateEndringsmelding(
			EndringsmeldingDbo(
				id = UUID.randomUUID(),
				deltakerId = deltakerId,
				type = EndringsmeldingType.ENDRE_SLUTTDATO,
				innhold = Innhold.EndreSluttdatoInnhold(sluttdato = LocalDate.now()),
				status = Endringsmelding.Status.AKTIV,
				sendt = LocalDateTime.now()
			)
		)
		val veilederId = UUID.randomUUID()
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = veilederId,
				personIdent = UUID.randomUUID().toString(),
				fornavn = "Vei",
				mellomnavn = null,
				etternavn = "Leder",
				roller = listOf(
					AnsattRolleDbo(arrangorId, AnsattRolle.VEILEDER)
				),
				deltakerlister = emptyList(),
				veilederDeltakere = listOf(VeilederDeltakerDbo(deltakerId, Veiledertype.VEILEDER))
			)
		)
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

		val deltaker = tiltaksarrangorService.getDeltaker(personIdent, deltakerId)

		deltaker.id shouldBe deltakerId
		deltaker.deltakerliste.id shouldBe deltakerliste.id
		deltaker.dagerPerUke shouldBe null
		deltaker.soktInnPa shouldBe deltakerliste.navn
		deltaker.tiltakskode shouldBe deltakerliste.tiltakType
		deltaker.aktiveEndringsmeldinger.size shouldBe 1
		val endringsmelding = deltaker.aktiveEndringsmeldinger.first()
		endringsmelding.type shouldBe Endringsmelding.Type.ENDRE_SLUTTDATO
		val innhold = endringsmelding.innhold as Endringsmelding.Innhold.EndreSluttdatoInnhold
		innhold.sluttdato shouldBe LocalDate.now()
		deltaker.veiledere.size shouldBe 1
		val veileder = deltaker.veiledere.first()
		veileder.ansattId shouldBe veilederId
		veileder.veiledertype shouldBe Veiledertype.VEILEDER
		deltaker.gjeldendeVurderingFraArrangor?.vurderingstype shouldBe Vurderingstype.OPPFYLLER_IKKE_KRAVENE
		deltaker.historiskeVurderingerFraArrangor?.size shouldBe 0
	}

	@Test
	fun `fjernDeltaker - ansatt har ikke rolle hos arrangor - returnerer unauthorized`() {
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
				deltakerlister = emptyList(),
				veilederDeltakere = emptyList()
			)
		)

		assertThrows<UnauthorizedException> {
			tiltaksarrangorService.fjernDeltaker(personIdent, deltakerId)
		}
	}

	@Test
	fun `fjernDeltaker - deltaker er ikke aktuell og ansatt har tilgang - skjuler deltaker`() {
		coEvery { amtTiltakClient.skjulDeltakerForTiltaksarrangor(any()) } just Runs
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.randomUUID()
		val deltaker = getDeltaker(deltakerId, deltakerliste.id).copy(status = StatusType.IKKE_AKTUELL)
		deltakerRepository.insertOrUpdateDeltaker(deltaker)
		val ansattId = UUID.randomUUID()
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = ansattId,
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

		tiltaksarrangorService.fjernDeltaker(personIdent, deltakerId)

		val deltakerFraDb = deltakerRepository.getDeltaker(deltakerId)
		deltakerFraDb?.skjultAvAnsattId shouldBe ansattId
		deltakerFraDb?.skjultDato shouldNotBe null
	}

	@Test
	fun `fjernDeltaker - deltaker venter pa oppstart og ansatt har tilgang - returnerer illegal state exception`() {
		coEvery { amtTiltakClient.skjulDeltakerForTiltaksarrangor(any()) } just Runs
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.randomUUID()
		val deltaker = getDeltaker(deltakerId, deltakerliste.id).copy(status = StatusType.VENTER_PA_OPPSTART)
		deltakerRepository.insertOrUpdateDeltaker(deltaker)
		val ansattId = UUID.randomUUID()
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = ansattId,
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

		assertThrows<IllegalStateException> {
			tiltaksarrangorService.fjernDeltaker(personIdent, deltakerId)
		}

		val deltakerFraDb = deltakerRepository.getDeltaker(deltakerId)
		deltakerFraDb?.skjultAvAnsattId shouldBe null
		deltakerFraDb?.skjultDato shouldBe null
	}

	@Test
	fun `fjernDeltaker - deltaker kan skjules men amt-tiltak feiler - returnerer feil og lagrer ingenting`() {
		coEvery { amtTiltakClient.skjulDeltakerForTiltaksarrangor(any()) } throws RuntimeException("Noe gikk galt")
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.randomUUID()
		val deltaker = getDeltaker(deltakerId, deltakerliste.id).copy(status = StatusType.HAR_SLUTTET)
		deltakerRepository.insertOrUpdateDeltaker(deltaker)
		val ansattId = UUID.randomUUID()
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = ansattId,
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

		assertThrows<RuntimeException> {
			tiltaksarrangorService.fjernDeltaker(personIdent, deltakerId)
		}

		val deltakerFraDb = deltakerRepository.getDeltaker(deltakerId)
		deltakerFraDb?.skjultAvAnsattId shouldBe null
		deltakerFraDb?.skjultDato shouldBe null
	}

	@Test
	fun `registrerVurdering - ansatt har ikke rolle hos arrangor - returnerer unauthorized`() {
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.randomUUID()
		deltakerRepository.insertOrUpdateDeltaker(getDeltaker(deltakerId, deltakerliste.id).copy(status = StatusType.VURDERES))
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
				deltakerlister = emptyList(),
				veilederDeltakere = emptyList()
			)
		)

		assertThrows<UnauthorizedException> {
			tiltaksarrangorService.registrerVurdering(personIdent, deltakerId, RegistrerVurderingRequest(Vurderingstype.OPPFYLLER_IKKE_KRAVENE, "Ikke gode nok norskkunnskaper"))
		}
	}

	@Test
	fun `registrerVurdering - deltaker har status vurderes og ansatt har tilgang - vurdering blir lagret`() {
		val deltakerId = UUID.randomUUID()
		val ansattId = UUID.randomUUID()
		val forsteVurdering = VurderingDto(
			id = UUID.randomUUID(),
			deltakerId = deltakerId,
			vurderingstype = Vurderingstype.OPPFYLLER_IKKE_KRAVENE,
			begrunnelse = "Mangler grunnkurs",
			opprettetAvArrangorAnsattId = UUID.randomUUID(),
			gyldigFra = LocalDateTime.now().minusWeeks(1),
			gyldigTil = null
		)
		val andreVurdering = VurderingDto(
			id = UUID.randomUUID(),
			deltakerId = deltakerId,
			vurderingstype = Vurderingstype.OPPFYLLER_KRAVENE,
			begrunnelse = null,
			opprettetAvArrangorAnsattId = ansattId,
			gyldigFra = LocalDateTime.now(),
			gyldigTil = null
		)
		coEvery { amtTiltakClient.registrerVurdering(any(), any()) } returns listOf(
			forsteVurdering.copy(gyldigTil = LocalDateTime.now()),
			andreVurdering
		)
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltaker = getDeltaker(deltakerId, deltakerliste.id).copy(
			status = StatusType.VURDERES,
			vurderingerFraArrangor = listOf(forsteVurdering)
		)
		deltakerRepository.insertOrUpdateDeltaker(deltaker)
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = ansattId,
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

		tiltaksarrangorService.registrerVurdering(personIdent, deltakerId, RegistrerVurderingRequest(Vurderingstype.OPPFYLLER_KRAVENE, null))

		val deltakerFraDb = deltakerRepository.getDeltaker(deltakerId)
		deltakerFraDb?.vurderingerFraArrangor?.size shouldBe 2
		deltakerFraDb?.vurderingerFraArrangor?.find { it.gyldigTil == null }?.vurderingstype shouldBe Vurderingstype.OPPFYLLER_KRAVENE
		deltakerFraDb?.vurderingerFraArrangor?.find { it.gyldigTil != null }?.vurderingstype shouldBe Vurderingstype.OPPFYLLER_IKKE_KRAVENE
	}

	@Test
	fun `registrerVurdering - deltaker venter pa oppstart og ansatt har tilgang - returnerer illegal state exception`() {
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.randomUUID()
		val deltaker = getDeltaker(deltakerId, deltakerliste.id).copy(status = StatusType.VENTER_PA_OPPSTART, vurderingerFraArrangor = null)
		deltakerRepository.insertOrUpdateDeltaker(deltaker)
		val ansattId = UUID.randomUUID()
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = ansattId,
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

		assertThrows<IllegalStateException> {
			tiltaksarrangorService.registrerVurdering(personIdent, deltakerId, RegistrerVurderingRequest(Vurderingstype.OPPFYLLER_KRAVENE, null))
		}

		val deltakerFraDb = deltakerRepository.getDeltaker(deltakerId)
		deltakerFraDb?.vurderingerFraArrangor shouldBe null
	}

	@Test
	fun `registrerVurdering - reguest har type OPPFYLLER_IKKE_KRAVENE og mangler begrunnelse - returnerer ValidationException`() {
		val personIdent = "12345678910"
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		val deltakerId = UUID.randomUUID()
		val deltaker = getDeltaker(deltakerId, deltakerliste.id).copy(status = StatusType.VURDERES, vurderingerFraArrangor = null)
		deltakerRepository.insertOrUpdateDeltaker(deltaker)
		val ansattId = UUID.randomUUID()
		ansattRepository.insertOrUpdateAnsatt(
			AnsattDbo(
				id = ansattId,
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

		assertThrows<ValidationException> {
			tiltaksarrangorService.registrerVurdering(personIdent, deltakerId, RegistrerVurderingRequest(Vurderingstype.OPPFYLLER_IKKE_KRAVENE, null))
		}

		val deltakerFraDb = deltakerRepository.getDeltaker(deltakerId)
		deltakerFraDb?.vurderingerFraArrangor shouldBe null
	}

	@Test
	fun `getAdresse - deltaker har adresse, tiltakstype jobbklubb - returnerer null`() {
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId).copy(tiltakType = "JOBBK")
		val deltakerId = UUID.randomUUID()
		val deltaker = getDeltaker(deltakerId, deltakerliste.id)
		val deltakerMedDeltakerlisteDbo = DeltakerMedDeltakerlisteDbo(deltaker, deltakerliste)

		val adresse = deltakerMedDeltakerlisteDbo.getAdresse()

		adresse shouldBe null
	}

	@Test
	fun `getAdresse - deltaker har kontaktadresse, bostedsadresse og oppholdsadresse, tiltakstype AFT - returnerer kontaktadresse`() {
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId).copy(tiltakType = "ARBFORB")
		val deltakerId = UUID.randomUUID()
		val deltaker = getDeltaker(deltakerId, deltakerliste.id).copy(
			adresse = AdresseDto(
				bostedsadresse = Bostedsadresse(
					coAdressenavn = "C/O Gutterommet",
					vegadresse = null,
					matrikkeladresse = Matrikkeladresse(
						tilleggsnavn = "Gården",
						postnummer = "0484",
						poststed = "OSLO"
					)
				),
				oppholdsadresse = Oppholdsadresse(
					coAdressenavn = null,
					vegadresse = Vegadresse(
						husnummer = "1",
						husbokstav = "B",
						adressenavn = "Veien",
						tilleggsnavn = null,
						postnummer = "1234",
						poststed = "MOSS"
					),
					matrikkeladresse = Matrikkeladresse(
						tilleggsnavn = "Fortet",
						postnummer = "0101",
						poststed = "ANDEBY"
					)
				),
				kontaktadresse = Kontaktadresse(
					coAdressenavn = null,
					vegadresse = null,
					postboksadresse = Postboksadresse(
						postboks = "45451",
						postnummer = "3312",
						poststed = "VESTØYA"
					)
				)
			)
		)
		val deltakerMedDeltakerlisteDbo = DeltakerMedDeltakerlisteDbo(deltaker, deltakerliste)

		val adresse = deltakerMedDeltakerlisteDbo.getAdresse()

		adresse?.adressetype shouldBe Adressetype.KONTAKTADRESSE
		adresse?.postnummer shouldBe "3312"
		adresse?.poststed shouldBe "VESTØYA"
		adresse?.tilleggsnavn shouldBe null
		adresse?.adressenavn shouldBe "Postboks 45451"
	}

	@Test
	fun `getAdresse - deltaker har bostedsadresse og oppholdsadresse, tiltakstype AFT - returnerer oppholdsadresse`() {
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId).copy(tiltakType = "ARBFORB")
		val deltakerId = UUID.randomUUID()
		val deltaker = getDeltaker(deltakerId, deltakerliste.id).copy(
			adresse = AdresseDto(
				bostedsadresse = Bostedsadresse(
					coAdressenavn = "C/O Gutterommet",
					vegadresse = null,
					matrikkeladresse = Matrikkeladresse(
						tilleggsnavn = "Gården",
						postnummer = "0484",
						poststed = "OSLO"
					)
				),
				oppholdsadresse = Oppholdsadresse(
					coAdressenavn = "C/O Pappa",
					vegadresse = Vegadresse(
						husnummer = "1",
						husbokstav = "B",
						adressenavn = "Veien",
						tilleggsnavn = null,
						postnummer = "1234",
						poststed = "MOSS"
					),
					matrikkeladresse = Matrikkeladresse(
						tilleggsnavn = "Fortet",
						postnummer = "0101",
						poststed = "ANDEBY"
					)
				),
				kontaktadresse = null
			)
		)
		val deltakerMedDeltakerlisteDbo = DeltakerMedDeltakerlisteDbo(deltaker, deltakerliste)

		val adresse = deltakerMedDeltakerlisteDbo.getAdresse()

		adresse?.adressetype shouldBe Adressetype.OPPHOLDSADRESSE
		adresse?.postnummer shouldBe "1234"
		adresse?.poststed shouldBe "MOSS"
		adresse?.tilleggsnavn shouldBe null
		adresse?.adressenavn shouldBe "C/O Pappa, Veien 1B"
	}

	@Test
	fun `getAdresse - deltaker har bare bostedsadresse, tiltakstype AFT - returnerer bostedsadresse`() {
		val arrangorId = UUID.randomUUID()
		val deltakerliste = getDeltakerliste(arrangorId).copy(tiltakType = "ARBFORB")
		val deltakerId = UUID.randomUUID()
		val deltaker = getDeltaker(deltakerId, deltakerliste.id).copy(
			adresse = AdresseDto(
				bostedsadresse = Bostedsadresse(
					coAdressenavn = "C/O Gutterommet",
					vegadresse = null,
					matrikkeladresse = Matrikkeladresse(
						tilleggsnavn = "Gården",
						postnummer = "0484",
						poststed = "OSLO"
					)
				),
				oppholdsadresse = null,
				kontaktadresse = null
			)
		)
		val deltakerMedDeltakerlisteDbo = DeltakerMedDeltakerlisteDbo(deltaker, deltakerliste)

		val adresse = deltakerMedDeltakerlisteDbo.getAdresse()

		adresse?.adressetype shouldBe Adressetype.BOSTEDSADRESSE
		adresse?.postnummer shouldBe "0484"
		adresse?.poststed shouldBe "OSLO"
		adresse?.tilleggsnavn shouldBe "Gården"
		adresse?.adressenavn shouldBe "C/O Gutterommet"
	}
}
