package no.nav.tiltaksarrangor.service

import no.nav.tiltaksarrangor.client.amtperson.AmtPersonClient
import no.nav.tiltaksarrangor.ingest.model.NavAnsatt
import no.nav.tiltaksarrangor.model.DeltakerHistorikk
import no.nav.tiltaksarrangor.repositories.NavAnsattRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class NavAnsattService(
	private val repository: NavAnsattRepository,
	private val amtPersonClient: AmtPersonClient,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	suspend fun hentEllerOpprettNavAnsatt(id: UUID): NavAnsatt {
		repository
			.get(id)
			?.let { return it }

		log.info("Fant ikke oppdatert nav-ansatt med nummer $id, henter fra amt-person-service")
		return fetchNavAnsatt(id)
	}

	private fun fetchNavAnsatt(id: UUID): NavAnsatt {
		val navAnsatt = amtPersonClient.hentNavAnsatt(id)

		repository.upsert(navAnsatt)
		log.info("Lagret nav-ansatt $id")

		return navAnsatt
	}

	fun upsert(navAnsatt: NavAnsatt) {
		repository.upsert(navAnsatt)
		log.info("Lagret nav-ansatt med id ${navAnsatt.id}")
	}

	fun hentAnsatteForHistorikk(historikk: List<DeltakerHistorikk>): Map<UUID, NavAnsatt> {
		val ider = historikk.flatMap { it.navAnsatte() }.distinct()
		return hentAnsatte(ider)
	}

	private fun hentAnsatte(veilederIder: List<UUID>) = repository.getMany(veilederIder).associateBy { it.id }
}
