package no.nav.tiltaksarrangor.service

import no.nav.tiltaksarrangor.client.amtarrangor.AmtArrangorClient
import no.nav.tiltaksarrangor.ingest.model.AnsattRolle
import no.nav.tiltaksarrangor.ingest.model.toAnsattDbo
import no.nav.tiltaksarrangor.model.Veileder
import no.nav.tiltaksarrangor.repositories.AnsattRepository
import no.nav.tiltaksarrangor.repositories.model.AnsattDbo
import no.nav.tiltaksarrangor.repositories.model.AnsattRolleDbo
import no.nav.tiltaksarrangor.repositories.model.KoordinatorDeltakerlisteDbo
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

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

	fun getVeiledereForDeltaker(deltakerId: UUID): List<Veileder> {
		return ansattRepository.getVeiledereForDeltaker(deltakerId).map { it.toVeileder(deltakerId) }
	}

	fun leggTilDeltakerliste(ansattId: UUID, deltakerlisteId: UUID, arrangorId: UUID) {
		amtArrangorClient.leggTilDeltakerlisteForKoordinator(ansattId = ansattId, deltakerlisteId = deltakerlisteId, arrangorId = arrangorId)
		ansattRepository.insertKoordinatorDeltakerliste(ansattId = ansattId, deltakerliste = KoordinatorDeltakerlisteDbo(deltakerlisteId))
	}

	fun fjernDeltakerliste(ansattId: UUID, deltakerlisteId: UUID, arrangorId: UUID) {
		amtArrangorClient.fjernDeltakerlisteForKoordinator(ansattId = ansattId, deltakerlisteId = deltakerlisteId, arrangorId = arrangorId)
		ansattRepository.deleteKoordinatorDeltakerliste(ansattId = ansattId, deltakerliste = KoordinatorDeltakerlisteDbo(deltakerlisteId))
	}

	fun harRoller(roller: List<AnsattRolleDbo>): Boolean {
		val unikeRoller = roller.map { it.rolle }.distinct()
		return unikeRoller.isNotEmpty()
	}

	fun erKoordinator(roller: List<AnsattRolleDbo>): Boolean {
		val unikeRoller = roller.map { it.rolle }.distinct()
		return unikeRoller.find { it == AnsattRolle.KOORDINATOR } != null
	}

	fun erVeileder(roller: List<AnsattRolleDbo>): Boolean {
		val unikeRoller = roller.map { it.rolle }.distinct()
		return unikeRoller.find { it == AnsattRolle.VEILEDER } != null
	}

	fun harRolleHosArrangor(arrangorId: UUID, rolle: AnsattRolle, roller: List<AnsattRolleDbo>): Boolean {
		return roller.find { it.arrangorId == arrangorId && it.rolle == rolle } != null
	}

	fun harTilgangTilDeltaker(deltakerId: UUID, deltakerlisteId: UUID, deltakerlisteArrangorId: UUID, ansattDbo: AnsattDbo): Boolean {
		val erKoordinatorHosArrangor = harRolleHosArrangor(deltakerlisteArrangorId, AnsattRolle.KOORDINATOR, ansattDbo.roller)
		val erVeilederHosArrangor = harRolleHosArrangor(deltakerlisteArrangorId, AnsattRolle.VEILEDER, ansattDbo.roller)

		if (erKoordinatorHosArrangor && ansattDbo.deltakerlister.find { it.deltakerlisteId == deltakerlisteId } != null) {
			return true
		} else if (erVeilederHosArrangor && ansattDbo.veilederDeltakere.find { it.deltakerId == deltakerId } != null) {
			return true
		}
		return false
	}
}
