package no.nav.tiltaksarrangor.service

import no.nav.tiltaksarrangor.client.amtarrangor.AmtArrangorClient
import no.nav.tiltaksarrangor.ingest.model.toAnsattDbo
import no.nav.tiltaksarrangor.repositories.AnsattRepository
import no.nav.tiltaksarrangor.repositories.model.AnsattDbo
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class AnsattService(
	private val amtArrangorClient: AmtArrangorClient,
	private val ansattRepository: AnsattRepository
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun oppdaterOgHentMineRoller(personIdent: String): List<String> {
		val ansatt = amtArrangorClient.getAnsatt(personIdent) ?: return emptyList<String>().also {
			log.info("Bruker uten tilganger har logget inn")
		}
		log.info("Hentet ansatt med id ${ansatt.id} fra amt-arrangør")

		ansattRepository.insertOrUpdateAnsatt(ansatt.toAnsattDbo())
		log.info("Lagret eller oppdatert ansatt med id ${ansatt.id}")
		ansattRepository.updateSistInnlogget(ansatt.id)

		return ansatt.arrangorer.flatMap { it.roller }.map { it.name }.distinct()
	}

	fun getAnsatt(personIdent: String): AnsattDbo? {
		return ansattRepository.getAnsatt(personIdent)
	}
}
