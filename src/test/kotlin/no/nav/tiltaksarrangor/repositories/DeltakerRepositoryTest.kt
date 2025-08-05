package no.nav.tiltaksarrangor.repositories

import io.kotest.matchers.shouldBe
import no.nav.tiltaksarrangor.RepositoryTestBase
import no.nav.tiltaksarrangor.testutils.DeltakerContext
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [DeltakerRepository::class])
class DeltakerRepositoryTest(
	private val repository: DeltakerRepository,
) : RepositoryTestBase() {
	@Test
	fun `oppdaterEnhetsnavnForDeltakere - nytt enhetsnavn - oppdaterer`() {
		with(DeltakerContext(applicationContext)) {
			val nyttEnhetsNavn = "Nytt Navn"
			repository.oppdaterEnhetsnavnForDeltakere(deltaker.navKontor!!, nyttEnhetsNavn)

			repository.getDeltaker(deltaker.id)!!.navKontor shouldBe nyttEnhetsNavn
		}
	}
}
