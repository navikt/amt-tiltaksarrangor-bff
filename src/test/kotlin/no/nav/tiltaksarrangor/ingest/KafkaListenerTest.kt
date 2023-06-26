package no.nav.tiltaksarrangor.ingest

import no.nav.tiltaksarrangor.IntegrationTest
import no.nav.tiltaksarrangor.ingest.model.AnsattDto
import no.nav.tiltaksarrangor.ingest.model.AnsattPersonaliaDto
import no.nav.tiltaksarrangor.ingest.model.AnsattRolle
import no.nav.tiltaksarrangor.ingest.model.ArrangorDto
import no.nav.tiltaksarrangor.ingest.model.DeltakerDto
import no.nav.tiltaksarrangor.ingest.model.DeltakerKontaktinformasjonDto
import no.nav.tiltaksarrangor.ingest.model.DeltakerNavVeilederDto
import no.nav.tiltaksarrangor.ingest.model.DeltakerPersonaliaDto
import no.nav.tiltaksarrangor.ingest.model.DeltakerStatus
import no.nav.tiltaksarrangor.ingest.model.DeltakerStatusDto
import no.nav.tiltaksarrangor.ingest.model.DeltakerlisteArrangorDto
import no.nav.tiltaksarrangor.ingest.model.DeltakerlisteDto
import no.nav.tiltaksarrangor.ingest.model.DeltakerlisteStatus
import no.nav.tiltaksarrangor.ingest.model.EndringsmeldingDto
import no.nav.tiltaksarrangor.ingest.model.EndringsmeldingType
import no.nav.tiltaksarrangor.ingest.model.Innhold
import no.nav.tiltaksarrangor.ingest.model.NavnDto
import no.nav.tiltaksarrangor.ingest.model.TilknyttetArrangorDto
import no.nav.tiltaksarrangor.ingest.model.TiltakDto
import no.nav.tiltaksarrangor.ingest.model.VeilederDto
import no.nav.tiltaksarrangor.ingest.model.toAnsattDbo
import no.nav.tiltaksarrangor.ingest.model.toArrangorDbo
import no.nav.tiltaksarrangor.ingest.model.toDeltakerDbo
import no.nav.tiltaksarrangor.ingest.model.toDeltakerlisteDbo
import no.nav.tiltaksarrangor.ingest.model.toEndringsmeldingDbo
import no.nav.tiltaksarrangor.kafka.subscribeHvisIkkeSubscribed
import no.nav.tiltaksarrangor.model.StatusType
import no.nav.tiltaksarrangor.model.Veiledertype
import no.nav.tiltaksarrangor.repositories.AnsattRepository
import no.nav.tiltaksarrangor.repositories.ArrangorRepository
import no.nav.tiltaksarrangor.repositories.DeltakerRepository
import no.nav.tiltaksarrangor.repositories.DeltakerlisteRepository
import no.nav.tiltaksarrangor.repositories.EndringsmeldingRepository
import no.nav.tiltaksarrangor.repositories.model.DeltakerDbo
import no.nav.tiltaksarrangor.testutils.DbTestDataUtils
import no.nav.tiltaksarrangor.testutils.SingletonPostgresContainer
import no.nav.tiltaksarrangor.utils.JsonUtils
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.awaitility.Awaitility
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit

class KafkaListenerTest : IntegrationTest() {
	private val dataSource = SingletonPostgresContainer.getDataSource()
	private val template = NamedParameterJdbcTemplate(dataSource)
	private val arrangorRepository = ArrangorRepository(template)
	private val ansattRepository = AnsattRepository(template)
	private val deltakerRepository = DeltakerRepository(template)
	private val deltakerlisteRepository = DeltakerlisteRepository(template, deltakerRepository)
	private val endringsmeldingRepository = EndringsmeldingRepository(template)

	@Autowired
	lateinit var testKafkaProducer: KafkaProducer<String, String>

	@Autowired
	lateinit var testKafkaConsumer: Consumer<String, String>

	@BeforeEach
	internal fun subscribe() {
		testKafkaConsumer.subscribeHvisIkkeSubscribed(ARRANGOR_TOPIC, ARRANGOR_ANSATT_TOPIC, DELTAKERLISTE_TOPIC, DELTAKER_TOPIC, ENDRINGSMELDING_TOPIC)
	}

	@AfterEach
	internal fun tearDown() {
		DbTestDataUtils.cleanDatabase(dataSource)
	}

	@Test
	fun `listen - melding pa arrangor-topic - lagres i database`() {
		val arrangorId = UUID.randomUUID()
		val arrangorDto = ArrangorDto(
			id = arrangorId,
			navn = "Arrangør AS",
			organisasjonsnummer = "88888888",
			overordnetArrangorId = UUID.randomUUID()
		)
		testKafkaProducer.send(
			ProducerRecord(
				ARRANGOR_TOPIC,
				null,
				arrangorId.toString(),
				JsonUtils.objectMapper.writeValueAsString(arrangorDto)
			)
		).get()

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until {
			arrangorRepository.getArrangor(arrangorId) != null
		}
	}

	@Test
	fun `listen - tombstonemelding pa arrangor-topic - slettes i database`() {
		val arrangorId = UUID.randomUUID()
		val arrangorDto = ArrangorDto(
			id = arrangorId,
			navn = "Arrangør AS",
			organisasjonsnummer = "77777777",
			overordnetArrangorId = null
		)
		arrangorRepository.insertOrUpdateArrangor(arrangorDto.toArrangorDbo())
		testKafkaProducer.send(
			ProducerRecord(
				ARRANGOR_TOPIC,
				null,
				arrangorId.toString(),
				null
			)
		).get()

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until {
			arrangorRepository.getArrangor(arrangorId) == null
		}
	}

	@Test
	fun `listen - melding pa arrangor-ansatt-topic - lagres i database`() {
		val deltakerId = UUID.randomUUID()
		deltakerRepository.insertOrUpdateDeltaker(
			DeltakerDbo(
				id = deltakerId,
				deltakerlisteId = UUID.randomUUID(),
				personident = "1234",
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				telefonnummer = null,
				epost = null,
				erSkjermet = false,
				status = StatusType.DELTAR,
				statusOpprettetDato = LocalDateTime.now(),
				statusGyldigFraDato = LocalDate.of(2023, 2, 1).atStartOfDay(),
				dagerPerUke = null,
				prosentStilling = null,
				startdato = LocalDate.of(2023, 2, 15),
				sluttdato = null,
				innsoktDato = LocalDate.now(),
				bestillingstekst = "tekst",
				navKontor = null,
				navVeilederId = null,
				navVeilederEpost = null,
				navVeilederNavn = null,
				skjultAvAnsattId = null,
				skjultDato = null
			)
		)
		val ansattId = UUID.randomUUID()
		val ansattDto = AnsattDto(
			id = ansattId,
			personalia = AnsattPersonaliaDto(
				personident = "12345678910",
				navn = NavnDto(
					fornavn = "Fornavn",
					mellomnavn = null,
					etternavn = "Etternavn"
				)
			),
			arrangorer = listOf(
				TilknyttetArrangorDto(
					arrangorId = UUID.randomUUID(),
					roller = listOf(AnsattRolle.KOORDINATOR, AnsattRolle.VEILEDER),
					veileder = listOf(VeilederDto(deltakerId, Veiledertype.VEILEDER)),
					koordinator = listOf(UUID.randomUUID())
				)
			)
		)
		testKafkaProducer.send(
			ProducerRecord(
				ARRANGOR_ANSATT_TOPIC,
				null,
				ansattId.toString(),
				JsonUtils.objectMapper.writeValueAsString(ansattDto)
			)
		).get()

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until {
			ansattRepository.getAnsatt(ansattId) != null &&
				ansattRepository.getAnsattRolleListe(ansattId).size == 2 &&
				ansattRepository.getKoordinatorDeltakerlisteDboListe(ansattId).size == 1 &&
				ansattRepository.getVeilederDeltakerDboListe(ansattId).size == 1
		}
	}

	@Test
	fun `listen - tombstonemelding pa arrangor-ansatt-topic - slettes i database`() {
		val deltakerId = UUID.randomUUID()
		deltakerRepository.insertOrUpdateDeltaker(
			DeltakerDbo(
				id = deltakerId,
				deltakerlisteId = UUID.randomUUID(),
				personident = "1234",
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				telefonnummer = null,
				epost = null,
				erSkjermet = false,
				status = StatusType.DELTAR,
				statusOpprettetDato = LocalDateTime.now(),
				statusGyldigFraDato = LocalDate.of(2023, 2, 1).atStartOfDay(),
				dagerPerUke = null,
				prosentStilling = null,
				startdato = LocalDate.of(2023, 2, 15),
				sluttdato = null,
				innsoktDato = LocalDate.now(),
				bestillingstekst = "tekst",
				navKontor = null,
				navVeilederId = null,
				navVeilederEpost = null,
				navVeilederNavn = null,
				skjultAvAnsattId = null,
				skjultDato = null
			)
		)
		val ansattId = UUID.randomUUID()
		val ansattDto = AnsattDto(
			id = ansattId,
			personalia = AnsattPersonaliaDto(
				personident = "12345678910",
				navn = NavnDto(
					fornavn = "Fornavn",
					mellomnavn = null,
					etternavn = "Etternavn"
				)
			),
			arrangorer = listOf(
				TilknyttetArrangorDto(
					arrangorId = UUID.randomUUID(),
					roller = listOf(AnsattRolle.KOORDINATOR, AnsattRolle.VEILEDER),
					veileder = listOf(VeilederDto(deltakerId, Veiledertype.VEILEDER)),
					koordinator = listOf(UUID.randomUUID())
				)
			)
		)
		ansattRepository.insertOrUpdateAnsatt(ansattDto.toAnsattDbo())
		testKafkaProducer.send(
			ProducerRecord(
				ARRANGOR_ANSATT_TOPIC,
				null,
				ansattId.toString(),
				null
			)
		).get()

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until {
			ansattRepository.getAnsattRolleListe(ansattId).isEmpty() &&
				ansattRepository.getKoordinatorDeltakerlisteDboListe(ansattId).isEmpty() &&
				ansattRepository.getVeilederDeltakerDboListe(ansattId).isEmpty() &&
				ansattRepository.getAnsatt(ansattId) == null
		}
	}

	@Test
	fun `listen - melding pa deltakerliste-topic - lagres i database`() {
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
				navn = "Det flotte tiltaket",
				type = "AMO"
			),
			startDato = LocalDate.of(2023, 5, 2),
			sluttDato = null,
			erKurs = false
		)
		testKafkaProducer.send(
			ProducerRecord(
				DELTAKERLISTE_TOPIC,
				null,
				deltakerlisteId.toString(),
				JsonUtils.objectMapper.writeValueAsString(deltakerlisteDto)
			)
		).get()

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until {
			deltakerlisteRepository.getDeltakerliste(deltakerlisteId) != null
		}
	}

	@Test
	fun `listen - tombstonemelding pa deltakerliste-topic - slettes i database`() {
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
				navn = "Det flotte tiltaket",
				type = "AMO"
			),
			startDato = LocalDate.of(2023, 5, 2),
			sluttDato = null,
			erKurs = false
		)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerlisteDto.toDeltakerlisteDbo())
		testKafkaProducer.send(
			ProducerRecord(
				DELTAKERLISTE_TOPIC,
				null,
				deltakerlisteId.toString(),
				null
			)
		).get()

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until {
			deltakerlisteRepository.getDeltakerliste(deltakerlisteId) == null
		}
	}

	@Test
	fun `listen - avsluttet deltakerliste-melding pa deltakerliste-topic og deltakerliste finnes i db - sletter deltakerliste og deltaker fra db`() {
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
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerlisteDto.toDeltakerlisteDbo())
		val deltakerId = UUID.randomUUID()
		val deltakerDto = DeltakerDto(
			id = deltakerId,
			deltakerlisteId = deltakerlisteId,
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
			skjult = null,
			deltarPaKurs = false
		)
		deltakerRepository.insertOrUpdateDeltaker(deltakerDto.toDeltakerDbo())
		val avsluttetDeltakerlisteDto = deltakerlisteDto.copy(status = DeltakerlisteStatus.AVSLUTTET, sluttDato = LocalDate.now().minusWeeks(4))
		testKafkaProducer.send(
			ProducerRecord(
				DELTAKERLISTE_TOPIC,
				null,
				deltakerlisteId.toString(),
				JsonUtils.objectMapper.writeValueAsString(avsluttetDeltakerlisteDto)
			)
		).get()

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until {
			deltakerlisteRepository.getDeltakerliste(deltakerlisteId) == null &&
				deltakerRepository.getDeltaker(deltakerId) == null
		}
	}

	@Test
	fun `listen - melding pa deltaker-topic - lagres i database`() {
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
			skjult = null,
			deltarPaKurs = false
		)
		testKafkaProducer.send(
			ProducerRecord(
				DELTAKER_TOPIC,
				null,
				deltakerId.toString(),
				JsonUtils.objectMapper.writeValueAsString(deltakerDto)
			)
		).get()

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until {
			deltakerRepository.getDeltaker(deltakerId) != null
		}
	}

	@Test
	fun `listen - tombstonemelding pa deltaker-topic - slettes i database`() {
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
			skjult = null,
			deltarPaKurs = false
		)
		deltakerRepository.insertOrUpdateDeltaker(deltakerDto.toDeltakerDbo())
		testKafkaProducer.send(
			ProducerRecord(
				DELTAKER_TOPIC,
				null,
				deltakerId.toString(),
				null
			)
		).get()

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until {
			deltakerRepository.getDeltaker(deltakerId) == null
		}
	}

	@Test
	fun `listen - avsluttet deltaker-melding pa deltaker-topic og deltaker finnes i db - sletter deltaker fra db`() {
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
			skjult = null,
			deltarPaKurs = false
		)
		deltakerRepository.insertOrUpdateDeltaker(deltakerDto.toDeltakerDbo())
		val avsluttetDeltakerDto = deltakerDto.copy(
			status = DeltakerStatusDto(
				type = DeltakerStatus.HAR_SLUTTET,
				gyldigFra = LocalDateTime.now().minusWeeks(4),
				opprettetDato = LocalDateTime.now().minusWeeks(4)
			)
		)
		testKafkaProducer.send(
			ProducerRecord(
				DELTAKER_TOPIC,
				null,
				deltakerId.toString(),
				JsonUtils.objectMapper.writeValueAsString(avsluttetDeltakerDto)
			)
		).get()

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until {
			deltakerRepository.getDeltaker(deltakerId) == null
		}
	}

	@Test
	fun `listen - melding pa endringsmelding-topic - lagres i database`() {
		val endringsmeldingId = UUID.randomUUID()
		val endringsmeldingDto = EndringsmeldingDto(
			id = endringsmeldingId,
			deltakerId = UUID.randomUUID(),
			utfortAvNavAnsattId = null,
			opprettetAvArrangorAnsattId = UUID.randomUUID(),
			utfortTidspunkt = null,
			status = "AKTIV",
			type = EndringsmeldingType.ENDRE_SLUTTDATO,
			innhold = Innhold.EndreSluttdatoInnhold(sluttdato = LocalDate.now().plusWeeks(3)),
			createdAt = LocalDateTime.now()
		)
		testKafkaProducer.send(
			ProducerRecord(
				ENDRINGSMELDING_TOPIC,
				null,
				endringsmeldingId.toString(),
				JsonUtils.objectMapper.writeValueAsString(endringsmeldingDto)
			)
		).get()

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until {
			endringsmeldingRepository.getEndringsmelding(endringsmeldingId) != null
		}
	}

	@Test
	fun `listen - tombstonemelding pa endringsmelding-topic - slettes i database`() {
		val endringsmeldingId = UUID.randomUUID()
		val endringsmeldingDto = EndringsmeldingDto(
			id = endringsmeldingId,
			deltakerId = UUID.randomUUID(),
			utfortAvNavAnsattId = null,
			opprettetAvArrangorAnsattId = UUID.randomUUID(),
			utfortTidspunkt = null,
			status = "AKTIV",
			type = EndringsmeldingType.ENDRE_SLUTTDATO,
			innhold = Innhold.EndreSluttdatoInnhold(sluttdato = LocalDate.now().plusWeeks(3)),
			createdAt = LocalDateTime.now()
		)
		endringsmeldingRepository.insertOrUpdateEndringsmelding(endringsmeldingDto.toEndringsmeldingDbo())
		testKafkaProducer.send(
			ProducerRecord(
				ENDRINGSMELDING_TOPIC,
				null,
				endringsmeldingId.toString(),
				null
			)
		).get()

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until {
			endringsmeldingRepository.getEndringsmelding(endringsmeldingId) == null
		}
	}

	@Test
	fun `listen - utfort endringsmelding-melding pa endringmelding-topic og endringsmelding finnes i db - sletter endringsmelding fra db`() {
		val endringsmeldingId = UUID.randomUUID()
		val endringsmeldingDto = EndringsmeldingDto(
			id = endringsmeldingId,
			deltakerId = UUID.randomUUID(),
			utfortAvNavAnsattId = null,
			opprettetAvArrangorAnsattId = UUID.randomUUID(),
			utfortTidspunkt = null,
			status = "AKTIV",
			type = EndringsmeldingType.DELTAKER_ER_AKTUELL,
			innhold = null,
			createdAt = LocalDateTime.now()
		)
		endringsmeldingRepository.insertOrUpdateEndringsmelding(endringsmeldingDto.toEndringsmeldingDbo())
		val utfortEndringsmeldingDto = EndringsmeldingDto(
			id = endringsmeldingId,
			deltakerId = UUID.randomUUID(),
			utfortAvNavAnsattId = null,
			opprettetAvArrangorAnsattId = UUID.randomUUID(),
			utfortTidspunkt = null,
			status = "UTFORT",
			type = EndringsmeldingType.DELTAKER_ER_AKTUELL,
			innhold = null,
			createdAt = LocalDateTime.now()
		)
		testKafkaProducer.send(
			ProducerRecord(
				ENDRINGSMELDING_TOPIC,
				null,
				endringsmeldingId.toString(),
				JsonUtils.objectMapper.writeValueAsString(utfortEndringsmeldingDto)
			)
		).get()

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until {
			endringsmeldingRepository.getEndringsmelding(endringsmeldingId) == null
		}
	}
}
