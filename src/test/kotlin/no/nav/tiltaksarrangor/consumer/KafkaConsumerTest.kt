package no.nav.tiltaksarrangor.consumer

import no.nav.amt.lib.models.deltaker.DeltakerEndring
import no.nav.amt.lib.models.deltaker.DeltakerHistorikk
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import no.nav.amt.lib.models.deltakerliste.tiltakstype.ArenaKode
import no.nav.amt.lib.utils.objectMapper
import no.nav.tiltaksarrangor.IntegrationTest
import no.nav.tiltaksarrangor.consumer.model.AnsattDto
import no.nav.tiltaksarrangor.consumer.model.AnsattPersonaliaDto
import no.nav.tiltaksarrangor.consumer.model.AnsattRolle
import no.nav.tiltaksarrangor.consumer.model.ArrangorDto
import no.nav.tiltaksarrangor.consumer.model.DeltakerlistePayload
import no.nav.tiltaksarrangor.consumer.model.EndringsmeldingDto
import no.nav.tiltaksarrangor.consumer.model.EndringsmeldingType
import no.nav.tiltaksarrangor.consumer.model.Innhold
import no.nav.tiltaksarrangor.consumer.model.NavnDto
import no.nav.tiltaksarrangor.consumer.model.Oppstartstype
import no.nav.tiltaksarrangor.consumer.model.TilknyttetArrangorDto
import no.nav.tiltaksarrangor.consumer.model.VeilederDto
import no.nav.tiltaksarrangor.consumer.model.toAnsattDbo
import no.nav.tiltaksarrangor.consumer.model.toArrangorDbo
import no.nav.tiltaksarrangor.consumer.model.toDeltakerDbo
import no.nav.tiltaksarrangor.consumer.model.toEndringsmeldingDbo
import no.nav.tiltaksarrangor.kafka.subscribeHvisIkkeSubscribed
import no.nav.tiltaksarrangor.model.DeltakerlisteStatus
import no.nav.tiltaksarrangor.model.Endringsmelding
import no.nav.tiltaksarrangor.model.Veiledertype
import no.nav.tiltaksarrangor.repositories.AnsattRepository
import no.nav.tiltaksarrangor.repositories.ArrangorRepository
import no.nav.tiltaksarrangor.repositories.DeltakerRepository
import no.nav.tiltaksarrangor.repositories.DeltakerlisteRepository
import no.nav.tiltaksarrangor.repositories.EndringsmeldingRepository
import no.nav.tiltaksarrangor.repositories.model.DeltakerlisteDbo
import no.nav.tiltaksarrangor.testutils.getDeltaker
import no.nav.tiltaksarrangor.testutils.getDeltakerliste
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.awaitility.Awaitility
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit

class KafkaConsumerTest(
	private val arrangorRepository: ArrangorRepository,
	private val ansattRepository: AnsattRepository,
	private val deltakerRepository: DeltakerRepository,
	private val deltakerlisteRepository: DeltakerlisteRepository,
	private val endringsmeldingRepository: EndringsmeldingRepository,
	private val testKafkaProducer: KafkaProducer<String, String>,
	private val testKafkaConsumer: Consumer<String, String>,
) : IntegrationTest() {
	@BeforeEach
	internal fun subscribe() {
		testKafkaConsumer.subscribeHvisIkkeSubscribed(
			ARRANGOR_TOPIC,
			ARRANGOR_ANSATT_TOPIC,
			DELTAKERLISTE_V1_TOPIC,
			DELTAKER_TOPIC,
			ENDRINGSMELDING_TOPIC,
		)
	}

	@Test
	fun `listen - melding pa arrangor-topic - lagres i database`() {
		val arrangorId = UUID.randomUUID()
		val arrangorDto =
			ArrangorDto(
				id = arrangorId,
				navn = "Arrangør AS",
				organisasjonsnummer = "88888888",
				overordnetArrangorId = UUID.randomUUID(),
			)
		testKafkaProducer
			.send(
				ProducerRecord(
					ARRANGOR_TOPIC,
					null,
					arrangorId.toString(),
					objectMapper.writeValueAsString(arrangorDto),
				),
			).get()

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until {
			arrangorRepository.getArrangor(arrangorId) != null
		}
	}

	@Test
	fun `listen - tombstonemelding pa arrangor-topic - slettes i database`() {
		val arrangorId = UUID.randomUUID()
		val arrangorDto =
			ArrangorDto(
				id = arrangorId,
				navn = "Arrangør AS",
				organisasjonsnummer = "77777777",
				overordnetArrangorId = null,
			)
		arrangorRepository.insertOrUpdateArrangor(arrangorDto.toArrangorDbo())
		testKafkaProducer
			.send(
				ProducerRecord(
					ARRANGOR_TOPIC,
					null,
					arrangorId.toString(),
					null,
				),
			).get()

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until {
			arrangorRepository.getArrangor(arrangorId) == null
		}
	}

	@Test
	fun `listen - melding pa arrangor-ansatt-topic - lagres i database`() {
		val deltaker = getDeltaker(UUID.randomUUID())
		deltakerRepository.insertOrUpdateDeltaker(deltaker)
		val ansattId = UUID.randomUUID()
		val ansattDto =
			AnsattDto(
				id = ansattId,
				personalia =
					AnsattPersonaliaDto(
						personident = "12345678910",
						navn =
							NavnDto(
								fornavn = "Fornavn",
								mellomnavn = null,
								etternavn = "Etternavn",
							),
					),
				arrangorer =
					listOf(
						TilknyttetArrangorDto(
							arrangorId = UUID.randomUUID(),
							roller = listOf(AnsattRolle.KOORDINATOR, AnsattRolle.VEILEDER),
							veileder = listOf(VeilederDto(deltaker.id, Veiledertype.VEILEDER)),
							koordinator = listOf(UUID.randomUUID()),
						),
					),
			)
		testKafkaProducer
			.send(
				ProducerRecord(
					ARRANGOR_ANSATT_TOPIC,
					null,
					ansattId.toString(),
					objectMapper.writeValueAsString(ansattDto),
				),
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
		val deltaker = getDeltaker(UUID.randomUUID())
		deltakerRepository.insertOrUpdateDeltaker(deltaker)
		val ansattId = UUID.randomUUID()
		val ansattDto =
			AnsattDto(
				id = ansattId,
				personalia =
					AnsattPersonaliaDto(
						personident = "12345678910",
						navn =
							NavnDto(
								fornavn = "Fornavn",
								mellomnavn = null,
								etternavn = "Etternavn",
							),
					),
				arrangorer =
					listOf(
						TilknyttetArrangorDto(
							arrangorId = UUID.randomUUID(),
							roller = listOf(AnsattRolle.KOORDINATOR, AnsattRolle.VEILEDER),
							veileder = listOf(VeilederDto(deltaker.id, Veiledertype.VEILEDER)),
							koordinator = listOf(UUID.randomUUID()),
						),
					),
			)
		ansattRepository.insertOrUpdateAnsatt(ansattDto.toAnsattDbo())
		testKafkaProducer
			.send(
				ProducerRecord(
					ARRANGOR_ANSATT_TOPIC,
					null,
					ansattId.toString(),
					null,
				),
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
		val arrangorDto =
			ArrangorDto(
				id = UUID.randomUUID(),
				navn = "Arrangør AS",
				organisasjonsnummer = "77777777",
				overordnetArrangorId = null,
			)
		arrangorRepository.insertOrUpdateArrangor(arrangorDto.toArrangorDbo())
		val deltakerlisteId = UUID.randomUUID()
		val deltakerlisteDto =
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
				startDato = LocalDate.of(2023, 5, 2),
				sluttDato = null,
				status = DeltakerlistePayload.Status.GJENNOMFORES,
				virksomhetsnummer = arrangorDto.organisasjonsnummer,
				oppstart = Oppstartstype.LOPENDE,
				tilgjengeligForArrangorFraOgMedDato = null,
			)
		testKafkaProducer
			.send(
				ProducerRecord(
					DELTAKERLISTE_V1_TOPIC,
					null,
					deltakerlisteId.toString(),
					objectMapper.writeValueAsString(deltakerlisteDto),
				),
			).get()

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until {
			deltakerlisteRepository.getDeltakerliste(deltakerlisteId) != null
		}
	}

	@Test
	fun `listen - tombstonemelding pa deltakerliste-topic - slettes i database`() {
		val deltakerlisteId = UUID.randomUUID()
		val deltakerlisteDbo =
			DeltakerlisteDbo(
				id = deltakerlisteId,
				navn = "Gjennomføring av tiltak",
				status = DeltakerlisteStatus.GJENNOMFORES,
				arrangorId = UUID.randomUUID(),
				tiltakNavn = "Det flotte tiltaket",
				tiltakType = ArenaKode.DIGIOPPARB,
				startDato = LocalDate.of(2023, 5, 2),
				sluttDato = null,
				erKurs = false,
				oppstartstype = Oppstartstype.LOPENDE,
				tilgjengeligForArrangorFraOgMedDato = null,
			)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerlisteDbo)
		testKafkaProducer
			.send(
				ProducerRecord(
					DELTAKERLISTE_V1_TOPIC,
					null,
					deltakerlisteId.toString(),
					null,
				),
			).get()

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until {
			deltakerlisteRepository.getDeltakerliste(deltakerlisteId) == null
		}
	}

	@Test
	fun `listen - avsluttet deltakerliste-melding pa deltakerliste-topic, deltakerliste finnes - sletter deltakerliste og deltaker fra db`() {
		val deltakerlisteId = UUID.randomUUID()
		val deltakerlisteDbo =
			DeltakerlisteDbo(
				id = deltakerlisteId,
				navn = "Gjennomføring av tiltak",
				status = DeltakerlisteStatus.GJENNOMFORES,
				arrangorId = UUID.randomUUID(),
				tiltakNavn = "Avsluttet tiltak",
				tiltakType = ArenaKode.DIGIOPPARB,
				startDato = LocalDate.now().minusYears(2),
				sluttDato = null,
				erKurs = false,
				oppstartstype = Oppstartstype.LOPENDE,
				tilgjengeligForArrangorFraOgMedDato = LocalDate.now().minusYears(2),
			)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerlisteDbo)

		val deltaker = getDeltaker(UUID.randomUUID(), deltakerlisteId)
		deltakerRepository.insertOrUpdateDeltaker(deltaker)

		val avsluttetDeltakerlisteDto =
			DeltakerlistePayload(
				id = deltakerlisteDbo.id,
				tiltakstype =
					DeltakerlistePayload.Tiltakstype(
						id = UUID.randomUUID(),
						navn = deltakerlisteDbo.tiltakNavn,
						arenaKode = deltakerlisteDbo.tiltakType.name,
						tiltakskode = deltakerlisteDbo.tiltakType.toTiltaksKode().toString(),
					),
				navn = deltakerlisteDbo.navn,
				startDato = deltakerlisteDbo.startDato!!,
				sluttDato = LocalDate.now().minusWeeks(4),
				status = DeltakerlistePayload.Status.AVSLUTTET,
				virksomhetsnummer = "888888888",
				oppstart = Oppstartstype.LOPENDE,
				tilgjengeligForArrangorFraOgMedDato = deltakerlisteDbo.tilgjengeligForArrangorFraOgMedDato,
			)
		testKafkaProducer
			.send(
				ProducerRecord(
					DELTAKERLISTE_V1_TOPIC,
					null,
					deltakerlisteId.toString(),
					objectMapper.writeValueAsString(avsluttetDeltakerlisteDto),
				),
			).get()

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until {
			deltakerlisteRepository.getDeltakerliste(deltakerlisteId) == null &&
				deltakerRepository.getDeltaker(deltaker.id) == null
		}
	}

	@Test
	fun `listen - melding pa deltaker-topic - lagres i database`() {
		val enhetId = UUID.randomUUID()
		val ansattId = UUID.randomUUID()
		with(DeltakerDtoCtx()) {
			deltakerlisteRepository.insertOrUpdateDeltakerliste(getDeltakerliste(id = deltakerDto.deltakerlisteId, UUID.randomUUID()))
			mockAmtPersonServer.addEnhetResponse(enhetId)
			mockAmtPersonServer.addAnsattResponse(ansattId)
			val avbrytDeltakelseEndring = DeltakerEndring.Endring.AvbrytDeltakelse(
				DeltakerEndring.Aarsak(DeltakerEndring.Aarsak.Type.TRENGER_ANNEN_STOTTE, null),
				sluttdato = LocalDate.now().minusWeeks(4),
				begrunnelse = null,
			)
			val dto = deltakerDto.copy(
				historikk = listOf(
					DeltakerHistorikk.Endring(
						endring = DeltakerEndring(
							id = UUID.randomUUID(),
							endring = avbrytDeltakelseEndring,
							deltakerId = deltakerDto.id,
							endretAv = ansattId,
							endretAvEnhet = enhetId,
							forslag = null,
							endret = LocalDateTime.now(),
						),
					),
				),
			)
			medVurderinger()

			mockAmtPersonServer.addKontaktinformasjonResponse(dto.personalia.personident)

			testKafkaProducer
				.send(
					ProducerRecord(
						DELTAKER_TOPIC,
						null,
						dto.id.toString(),
						objectMapper.writeValueAsString(dto),
					),
				).get()

			Awaitility.await().atMost(5, TimeUnit.SECONDS).until {
				deltakerRepository.getDeltaker(dto.id) != null
			}
		}
	}

	@Test
	fun `listen - tombstonemelding pa deltaker-topic - slettes i database`() {
		val deltaker = getDeltaker(UUID.randomUUID())

		deltakerRepository.insertOrUpdateDeltaker(deltaker)
		testKafkaProducer
			.send(
				ProducerRecord(
					DELTAKER_TOPIC,
					null,
					deltaker.id.toString(),
					null,
				),
			).get()

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until {
			deltakerRepository.getDeltaker(deltaker.id) == null
		}
	}

	@Test
	fun `listen - avsluttet deltaker-melding pa deltaker-topic og deltaker finnes i db - sletter deltaker fra db`() {
		with(DeltakerDtoCtx()) {
			deltakerlisteRepository.insertOrUpdateDeltakerliste(getDeltakerliste(id = deltakerDto.deltakerlisteId, UUID.randomUUID()))

			deltakerRepository.insertOrUpdateDeltaker(deltakerDto.toDeltakerDbo(null))

			medStatus(DeltakerStatus.Type.HAR_SLUTTET, 50)

			testKafkaProducer
				.send(
					ProducerRecord(
						DELTAKER_TOPIC,
						null,
						deltakerDto.id.toString(),
						objectMapper.writeValueAsString(deltakerDto),
					),
				).get()

			Awaitility.await().atMost(5, TimeUnit.SECONDS).until {
				deltakerRepository.getDeltaker(deltakerDto.id) == null
			}
		}
	}

	@Test
	fun `listen - melding pa endringsmelding-topic - lagres i database`() {
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
		testKafkaProducer
			.send(
				ProducerRecord(
					ENDRINGSMELDING_TOPIC,
					null,
					endringsmeldingId.toString(),
					objectMapper.writeValueAsString(endringsmeldingDto),
				),
			).get()

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until {
			endringsmeldingRepository.getEndringsmelding(endringsmeldingId) != null
		}
	}

	@Test
	fun `listen - tombstonemelding pa endringsmelding-topic - slettes i database`() {
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
		endringsmeldingRepository.insertOrUpdateEndringsmelding(endringsmeldingDto.toEndringsmeldingDbo())
		testKafkaProducer
			.send(
				ProducerRecord(
					ENDRINGSMELDING_TOPIC,
					null,
					endringsmeldingId.toString(),
					null,
				),
			).get()

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until {
			endringsmeldingRepository.getEndringsmelding(endringsmeldingId) == null
		}
	}

	@Test
	fun `listen - utfort endringsmelding-melding pa endringmelding-topic og endringsmelding finnes i db - oppdaterer endringsmelding i db`() {
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
		endringsmeldingRepository.insertOrUpdateEndringsmelding(endringsmeldingDto.toEndringsmeldingDbo())
		val utfortEndringsmeldingDto =
			EndringsmeldingDto(
				id = endringsmeldingId,
				deltakerId = UUID.randomUUID(),
				utfortAvNavAnsattId = null,
				opprettetAvArrangorAnsattId = UUID.randomUUID(),
				utfortTidspunkt = null,
				status = Endringsmelding.Status.UTFORT,
				type = EndringsmeldingType.ENDRE_SLUTTDATO,
				innhold = Innhold.EndreSluttdatoInnhold(sluttdato = LocalDate.now().plusWeeks(3)),
				createdAt = LocalDateTime.now(),
			)
		testKafkaProducer
			.send(
				ProducerRecord(
					ENDRINGSMELDING_TOPIC,
					null,
					endringsmeldingId.toString(),
					objectMapper.writeValueAsString(utfortEndringsmeldingDto),
				),
			).get()

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until {
			endringsmeldingRepository.getEndringsmelding(endringsmeldingId)?.status == Endringsmelding.Status.UTFORT
		}
	}
}
