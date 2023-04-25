package no.nav.tiltaksarrangor.ingest

import no.nav.tiltaksarrangor.IntegrationTest
import no.nav.tiltaksarrangor.ingest.model.AnsattDto
import no.nav.tiltaksarrangor.ingest.model.AnsattRolle
import no.nav.tiltaksarrangor.ingest.model.ArrangorDto
import no.nav.tiltaksarrangor.ingest.model.NavnDto
import no.nav.tiltaksarrangor.ingest.model.PersonaliaDto
import no.nav.tiltaksarrangor.ingest.model.TilknyttetArrangorDto
import no.nav.tiltaksarrangor.ingest.model.VeilederDto
import no.nav.tiltaksarrangor.ingest.model.Veiledertype
import no.nav.tiltaksarrangor.ingest.model.toAnsattDbo
import no.nav.tiltaksarrangor.ingest.model.toArrangorDbo
import no.nav.tiltaksarrangor.ingest.repositories.AnsattRepository
import no.nav.tiltaksarrangor.ingest.repositories.ArrangorRepository
import no.nav.tiltaksarrangor.kafka.subscribeHvisIkkeSubscribed
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
import java.util.UUID
import java.util.concurrent.TimeUnit

class KafkaListenerTest : IntegrationTest() {
	private val dataSource = SingletonPostgresContainer.getDataSource()
	private val template = NamedParameterJdbcTemplate(dataSource)
	private val arrangorRepository = ArrangorRepository(template)
	private val ansattRepository = AnsattRepository(template)

	@Autowired
	lateinit var testKafkaProducer: KafkaProducer<String, String>

	@Autowired
	lateinit var testKafkaConsumer: Consumer<String, String>

	@BeforeEach
	internal fun subscribe() {
		testKafkaConsumer.subscribeHvisIkkeSubscribed(ARRANGOR_TOPIC, ARRANGOR_ANSATT_TOPIC)
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
			overordnetArrangorId = UUID.randomUUID(),
			deltakerlister = emptyList()
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
			overordnetArrangorId = null,
			deltakerlister = emptyList()
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
		val ansattId = UUID.randomUUID()
		val ansattDto = AnsattDto(
			id = ansattId,
			personalia = PersonaliaDto(
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
					veileder = listOf(VeilederDto(UUID.randomUUID(), Veiledertype.VEILEDER)),
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
		val ansattId = UUID.randomUUID()
		val ansattDto = AnsattDto(
			id = ansattId,
			personalia = PersonaliaDto(
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
					veileder = listOf(VeilederDto(UUID.randomUUID(), Veiledertype.VEILEDER)),
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
}
