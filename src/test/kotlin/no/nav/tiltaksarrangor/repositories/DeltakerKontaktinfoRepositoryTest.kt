package no.nav.tiltaksarrangor.repositories

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tiltaksarrangor.consumer.model.Kontaktinformasjon
import no.nav.tiltaksarrangor.testutils.DbTestDataUtils.cleanDatabase
import no.nav.tiltaksarrangor.testutils.DeltakerContext
import no.nav.tiltaksarrangor.testutils.SingletonPostgresContainer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

class DeltakerKontaktinfoRepositoryTest {
	private val dataSource = SingletonPostgresContainer.getDataSource()
	private val template = NamedParameterJdbcTemplate(dataSource)
	private val repository = DeltakerKontaktinfoRepository(template)

	val kontaktinfo = Kontaktinformasjon(
		epost = "ny@epost",
		telefonnummer = "nyttTelefonnummer",
	)

	@AfterEach
	fun clean() {
		cleanDatabase(dataSource)
	}

	@Test
	fun `oppdaterKontaktinfo - en deltaker - oppdaterer`() {
		with(DeltakerContext()) {
			repository.oppdaterKontaktinformasjon(
				mapOf(deltaker.personident to kontaktinfo),
			)

			assertKontaktinfoOppdatert(this, kontaktinfo)
		}
	}

	@Test
	fun `oppdaterKontaktinfo - flere deltakere - oppdaterer`() {
		val deltakerCtx1 = DeltakerContext()
		val deltakerCtx2 = DeltakerContext()
		repository.oppdaterKontaktinformasjon(
			mapOf(
				deltakerCtx1.deltaker.personident to kontaktinfo,
				deltakerCtx2.deltaker.personident to kontaktinfo,
			),
		)

		assertKontaktinfoOppdatert(deltakerCtx1, kontaktinfo)
		assertKontaktinfoOppdatert(deltakerCtx2, kontaktinfo)
	}

	@Test
	fun `getPersonerForOppdatering - distribuerer deltakere på time`() {
		with(DeltakerContext()) {
			(0..23).forEach { time ->
				medPersonident(time.toString())
				repository.getPersonerForOppdatering(time).shouldNotBeNull().shouldBe(
					listOf(deltaker.personident),
				)
			}
		}
	}

	private fun assertKontaktinfoOppdatert(deltakerContext: DeltakerContext, kontaktinfo: Kontaktinformasjon) {
		val oppdatertDeltaker =
			deltakerContext.deltakerRepository.getDeltaker(deltakerContext.deltaker.id).shouldNotBeNull()

		oppdatertDeltaker.telefonnummer shouldBe kontaktinfo.telefonnummer
		oppdatertDeltaker.epost shouldBe kontaktinfo.epost
	}
}
