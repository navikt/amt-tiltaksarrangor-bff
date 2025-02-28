package no.nav.tiltaksarrangor.service

import no.nav.amt.lib.models.deltaker.DeltakerHistorikk
import no.nav.tiltaksarrangor.client.amtperson.AmtPersonClient
import no.nav.tiltaksarrangor.client.amtperson.NavAnsattResponse
import no.nav.tiltaksarrangor.ingest.model.NavAnsatt
import no.nav.tiltaksarrangor.model.UlestEndring
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

	fun hentNavAnsatt(id: UUID): NavAnsatt? {
		repository
			.get(id)
			?.let { return it }
		return null
	}

	fun hentEllerOpprettNavAnsatt(id: UUID): NavAnsatt {
		repository
			.get(id)
			?.let { return it }

		log.info("Fant ikke oppdatert nav-ansatt med nummer $id, henter fra amt-person-service")
		return fetchNavAnsatt(id)
	}

	private fun fetchNavAnsatt(id: UUID): NavAnsatt {
		val navAnsatt = amtPersonClient.hentNavAnsatt(id).toModel()

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

	fun hentAnsatteForUlesteEndringer(ulesteEndringer: List<UlestEndring>): Map<UUID, NavAnsatt> {
		val ider = ulesteEndringer.mapNotNull { it.hentNavAnsattId() }.distinct()
		return hentAnsatte(ider)
	}
}

fun NavAnsattResponse.toModel() = NavAnsatt(
	id = id,
	navident = navIdent,
	navn = navn,
	epost = epost,
	telefon = telefon,
)
