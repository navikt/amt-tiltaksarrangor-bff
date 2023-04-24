package no.nav.tiltaksarrangor.ingest

import no.nav.tiltaksarrangor.IntegrationTest
import no.nav.tiltaksarrangor.ingest.model.ArrangorDto
import no.nav.tiltaksarrangor.ingest.model.toArrangorDbo
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
	private val arrangorRepository = ArrangorRepository(NamedParameterJdbcTemplate(dataSource))

	@Autowired
	lateinit var testKafkaProducer: KafkaProducer<String, String>

	@Autowired
	lateinit var testKafkaConsumer: Consumer<String, String>

	@BeforeEach
	internal fun subscribe() {
		testKafkaConsumer.subscribeHvisIkkeSubscribed(ARRANGOR_TOPIC)
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
}
