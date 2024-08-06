package no.nav.tiltaksarrangor.melding.endring

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.amt.lib.kafka.config.LocalKafkaConfig
import no.nav.amt.lib.models.arrangor.melding.EndringFraArrangor
import no.nav.amt.lib.testing.AsyncUtils
import no.nav.amt.lib.testing.SingletonKafkaProvider
import no.nav.tiltaksarrangor.kafka.stringStringConsumer
import no.nav.tiltaksarrangor.melding.MELDING_TOPIC
import no.nav.tiltaksarrangor.melding.MeldingProducer
import no.nav.tiltaksarrangor.melding.endring.request.LeggTilOppstartsdatoRequest
import no.nav.tiltaksarrangor.melding.forslag.ForslagRepository
import no.nav.tiltaksarrangor.melding.forslag.ForslagService
import no.nav.tiltaksarrangor.repositories.AnsattRepository
import no.nav.tiltaksarrangor.repositories.DeltakerRepository
import no.nav.tiltaksarrangor.repositories.EndringsmeldingRepository
import no.nav.tiltaksarrangor.service.AnsattService
import no.nav.tiltaksarrangor.service.DeltakerMapper
import no.nav.tiltaksarrangor.testutils.DeltakerContext
import no.nav.tiltaksarrangor.testutils.SingletonPostgresContainer
import no.nav.tiltaksarrangor.utils.JsonUtils.objectMapper
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDate
import java.util.UUID
import kotlin.reflect.KClass

class EndringServiceTest {
	private val dataSource = SingletonPostgresContainer.getDataSource()
	private val template = NamedParameterJdbcTemplate(dataSource)
	private val deltakerRepository = DeltakerRepository(template)
	private val producer = MeldingProducer(LocalKafkaConfig(SingletonKafkaProvider.getHost()))

	private val deltakerMapper = DeltakerMapper(
		ansattService = AnsattService(mockk(), AnsattRepository(template)),
		forslagService = ForslagService(ForslagRepository(template), producer),
		endringsmeldingRepository = EndringsmeldingRepository(template),
	)

	private val endringService = EndringService(
		producer = producer,
		deltakerRepository = deltakerRepository,
		deltakerMapper = deltakerMapper,
	)

	@Test
	fun `endreDeltaker - ny endring - returnerer oppdaterer deltaker og produserer melding`() {
		with(DeltakerContext()) {
			setVenterPaOppstart()
			val request = LeggTilOppstartsdatoRequest(
				startdato = LocalDate.now().plusWeeks(7),
				sluttdato = LocalDate.now().plusWeeks(42),
			)
			val oppdatertDeltaker = endringService.endreDeltaker(
				deltaker = deltaker,
				deltakerliste = deltakerliste,
				ansatt = koordinator,
				request = request,
			)

			oppdatertDeltaker.startDato shouldBe request.startdato
			oppdatertDeltaker.sluttDato shouldBe request.sluttdato

			assertProducedEndring(deltaker.id, EndringFraArrangor.LeggTilOppstartsdato::class)
		}
	}

	@Test
	fun `endreDeltaker - ny endring - oppdaterer og lagrer deltaker`() {
		with(DeltakerContext()) {
			setVenterPaOppstart()
			val request = LeggTilOppstartsdatoRequest(
				startdato = LocalDate.now().plusWeeks(7),
				sluttdato = LocalDate.now().plusWeeks(42),
			)
			endringService.endreDeltaker(
				deltaker = deltaker,
				deltakerliste = deltakerliste,
				ansatt = koordinator,
				request = request,
			)

			val oppdatertDeltaker = deltakerRepository.getDeltaker(deltaker.id)
			oppdatertDeltaker?.startdato shouldBe request.startdato
			oppdatertDeltaker?.sluttdato shouldBe request.sluttdato
		}
	}
}

fun <T : EndringFraArrangor.Endring> assertProducedEndring(deltakerId: UUID, endringstype: KClass<T>) {
	val cache = mutableMapOf<UUID, EndringFraArrangor>()

	val consumer = stringStringConsumer(MELDING_TOPIC) { k, v ->
		cache[UUID.fromString(k)] = objectMapper.readValue(v)
	}

	consumer.run()

	AsyncUtils.eventually {
		val endring = cache.firstNotNullOf {
			if (it.value.deltakerId == deltakerId) {
				it.value
			} else {
				null
			}
		}

		endring.endring::class shouldBe endringstype
	}

	consumer.stop()
}
