package no.nav.tiltaksarrangor.service

import no.nav.tiltaksarrangor.client.amtperson.AmtPersonClient
import no.nav.tiltaksarrangor.ingest.model.NavEnhet
import no.nav.tiltaksarrangor.repositories.NavEnhetRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class NavEnhetService(
	private val repository: NavEnhetRepository,
	private val amtPersonClient: AmtPersonClient,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun get(id: UUID): NavEnhet = repository.get(id) ?: fetchEnhet(id)

	private fun fetchEnhet(id: UUID): NavEnhet {
		val enhet = amtPersonClient.hentEnhet(id)

		repository.upsert(enhet)
		log.info("Lagret nav-enhet $id")

		return enhet
	}
}
