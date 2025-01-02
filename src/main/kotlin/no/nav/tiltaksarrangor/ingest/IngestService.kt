package no.nav.tiltaksarrangor.ingest

import no.nav.amt.lib.models.arrangor.melding.EndringFraArrangor
import no.nav.amt.lib.models.arrangor.melding.Forslag
import no.nav.amt.lib.models.arrangor.melding.Melding
import no.nav.amt.lib.models.deltaker.DeltakerHistorikk
import no.nav.tiltaksarrangor.client.amtarrangor.AmtArrangorClient
import no.nav.tiltaksarrangor.client.amtarrangor.dto.toArrangorDbo
import no.nav.tiltaksarrangor.ingest.model.AVSLUTTENDE_STATUSER
import no.nav.tiltaksarrangor.ingest.model.AnsattDto
import no.nav.tiltaksarrangor.ingest.model.ArrangorDto
import no.nav.tiltaksarrangor.ingest.model.DeltakerDto
import no.nav.tiltaksarrangor.ingest.model.DeltakerStatus
import no.nav.tiltaksarrangor.ingest.model.DeltakerlisteDto
import no.nav.tiltaksarrangor.ingest.model.EndringsmeldingDto
import no.nav.tiltaksarrangor.ingest.model.NavAnsatt
import no.nav.tiltaksarrangor.ingest.model.SKJULES_ALLTID_STATUSER
import no.nav.tiltaksarrangor.ingest.model.toAnsattDbo
import no.nav.tiltaksarrangor.ingest.model.toArrangorDbo
import no.nav.tiltaksarrangor.ingest.model.toDeltakerDbo
import no.nav.tiltaksarrangor.ingest.model.toEndringsmeldingDbo
import no.nav.tiltaksarrangor.melding.forslag.ForslagService
import no.nav.tiltaksarrangor.repositories.AnsattRepository
import no.nav.tiltaksarrangor.repositories.ArrangorRepository
import no.nav.tiltaksarrangor.repositories.DeltakerRepository
import no.nav.tiltaksarrangor.repositories.DeltakerlisteRepository
import no.nav.tiltaksarrangor.repositories.EndringsmeldingRepository
import no.nav.tiltaksarrangor.repositories.model.DAGER_AVSLUTTET_DELTAKER_VISES
import no.nav.tiltaksarrangor.repositories.model.DeltakerDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerlisteDbo
import no.nav.tiltaksarrangor.service.NavAnsattService
import no.nav.tiltaksarrangor.service.NavEnhetService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Component
class IngestService(
	private val arrangorRepository: ArrangorRepository,
	private val ansattRepository: AnsattRepository,
	private val deltakerlisteRepository: DeltakerlisteRepository,
	private val deltakerRepository: DeltakerRepository,
	private val endringsmeldingRepository: EndringsmeldingRepository,
	private val amtArrangorClient: AmtArrangorClient,
	private val forslagService: ForslagService,
	private val navEnhetService: NavEnhetService,
	private val navAnsattService: NavAnsattService,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun lagreArrangor(arrangorId: UUID, arrangor: ArrangorDto?) {
		if (arrangor == null) {
			arrangorRepository.deleteArrangor(arrangorId)
			log.info("Slettet arrangør med id $arrangorId")
		} else {
			arrangorRepository.insertOrUpdateArrangor(arrangor.toArrangorDbo())
			log.info("Lagret arrangør med id $arrangorId")
		}
	}

	fun lagreAnsatt(ansattId: UUID, ansatt: AnsattDto?) {
		if (ansatt == null || ansatt.arrangorer.isEmpty()) {
			ansattRepository.deleteAnsatt(ansattId)
			log.info("Slettet ansatt med id $ansattId")
		} else {
			ansattRepository.insertOrUpdateAnsatt(ansatt.toAnsattDbo())
			log.info("Lagret ansatt med id $ansattId")
		}
	}

	fun lagreDeltakerliste(deltakerlisteId: UUID, deltakerlisteDto: DeltakerlisteDto?) {
		if (deltakerlisteDto == null) {
			deltakerlisteRepository.deleteDeltakerlisteOgDeltakere(deltakerlisteId)
			log.info("Slettet tombstonet deltakerliste med id $deltakerlisteId")
		} else if (deltakerlisteDto.skalLagres()) {
			deltakerlisteRepository.insertOrUpdateDeltakerliste(toDeltakerlisteDbo(deltakerlisteDto))
			log.info("Lagret deltakerliste med id $deltakerlisteId")
		} else {
			val antallSlettedeDeltakerlister = deltakerlisteRepository.deleteDeltakerlisteOgDeltakere(deltakerlisteId)
			if (antallSlettedeDeltakerlister > 0) {
				log.info("Slettet deltakerliste med id $deltakerlisteId")
			} else {
				log.info("Ignorert deltakerliste med id $deltakerlisteId")
			}
		}
	}

	fun lagreDeltaker(deltakerId: UUID, deltakerDto: DeltakerDto?) {
		if (deltakerDto == null) {
			deltakerRepository.deleteDeltaker(deltakerId)
			log.info("Slettet tombstonet deltaker med id $deltakerId")
			return
		}
		val lagretDeltaker = deltakerRepository.getDeltaker(deltakerId)
		if (deltakerDto.skalLagres(lagretDeltaker)) {
			leggTilNavAnsattOgEnhetHistorikk(deltakerDto)
			deltakerRepository.insertOrUpdateDeltaker(deltakerDto.toDeltakerDbo(lagretDeltaker))
			log.info("Lagret deltaker med id $deltakerId")
		} else {
			val antallSlettedeDeltakere = deltakerRepository.deleteDeltaker(deltakerId)
			if (antallSlettedeDeltakere > 0) {
				log.info("Slettet deltaker med id $deltakerId")
			} else {
				log.info("Ignorert deltaker med id $deltakerId")
			}
		}
	}

	private fun leggTilNavAnsattOgEnhetHistorikk(deltakerDto: DeltakerDto) {
		if (deltakerDto.historikk.isNullOrEmpty()) {
			return
		}
		lagreEnheterForHistorikk(deltakerDto.historikk)
		lagreAnsatteForHistorikk(deltakerDto.historikk)
	}

	fun lagreEnheterForHistorikk(historikk: List<DeltakerHistorikk>) {
		historikk.flatMap { it.navEnheter() }.distinct().forEach { id -> navEnhetService.hentOpprettEllerOppdaterNavEnhet(id) }
	}

	fun lagreAnsatteForHistorikk(historikk: List<DeltakerHistorikk>) {
		historikk.flatMap { it.navAnsatte() }.distinct().forEach { id -> navAnsattService.hentEllerOpprettNavAnsatt(id) }
	}

	fun lagreNavAnsatt(id: UUID, navAnsatt: NavAnsatt) {
		navAnsattService.upsert(navAnsatt)
	}

	fun lagreEndringsmelding(endringsmeldingId: UUID, endringsmeldingDto: EndringsmeldingDto?) {
		if (endringsmeldingDto == null) {
			endringsmeldingRepository.deleteEndringsmelding(endringsmeldingId)
			log.info("Slettet tombstonet endringsmelding med id $endringsmeldingId")
		} else {
			endringsmeldingRepository.insertOrUpdateEndringsmelding(endringsmeldingDto.toEndringsmeldingDbo())
			log.info("Lagret endringsmelding med id $endringsmeldingId")
		}
	}

	private fun DeltakerDto.skalLagres(lagretDeltaker: DeltakerDbo?): Boolean {
		if (status.type in SKJULES_ALLTID_STATUSER) {
			return false
		} else if (status.type == DeltakerStatus.IKKE_AKTUELL && deltarPaKurs && lagretDeltaker == null) {
			return false
		} else if (status.type in AVSLUTTENDE_STATUSER) {
			return harNyligSluttet()
		}
		return true
	}

	private fun DeltakerDto.harNyligSluttet(): Boolean =
		!LocalDateTime.now().isAfter(status.gyldigFra.plusDays(DAGER_AVSLUTTET_DELTAKER_VISES)) &&
			(sluttdato == null || sluttdato.isAfter(LocalDate.now().minusDays(DAGER_AVSLUTTET_DELTAKER_VISES)))

	private fun toDeltakerlisteDbo(deltakerlisteDto: DeltakerlisteDto): DeltakerlisteDbo = DeltakerlisteDbo(
		id = deltakerlisteDto.id,
		navn = deltakerlisteDto.navn,
		status = deltakerlisteDto.toDeltakerlisteStatus(),
		arrangorId = getArrangorId(deltakerlisteDto.virksomhetsnummer),
		tiltakNavn = deltakerlisteDto.tiltakstype.navn,
		tiltakType = deltakerlisteDto.tiltakstype.arenaKode,
		startDato = deltakerlisteDto.startDato,
		sluttDato = deltakerlisteDto.sluttDato,
		erKurs = deltakerlisteDto.erKurs(),
		tilgjengeligForArrangorFraOgMedDato = deltakerlisteDto.tilgjengeligForArrangorFraOgMedDato,
	)

	private fun getArrangorId(organisasjonsnummer: String): UUID {
		val arrangorId = arrangorRepository.getArrangor(organisasjonsnummer)?.id
		if (arrangorId != null) {
			return arrangorId
		}
		log.info("Fant ikke arrangør med orgnummer $organisasjonsnummer i databasen, henter fra amt-arrangør")
		val arrangor =
			amtArrangorClient.getArrangor(organisasjonsnummer)
				?: throw RuntimeException("Kunne ikke hente arrangør med orgnummer $organisasjonsnummer")
		arrangorRepository.insertOrUpdateArrangor(arrangor.toArrangorDbo())
		return arrangor.id
	}

	fun handleMelding(id: UUID, melding: Melding?) {
		if (melding == null) {
			log.warn("Mottok tombstone for melding med id: $id")
			forslagService.delete(id)
			return
		}
		when (melding) {
			is Forslag -> handleForslag(melding)
			is EndringFraArrangor -> {}
		}
	}

	private fun handleForslag(forslag: Forslag) {
		when (forslag.status) {
			is Forslag.Status.Avvist,
			is Forslag.Status.Godkjent,
			-> forslagService.delete(forslag.id)

			is Forslag.Status.Erstattet,
			is Forslag.Status.Tilbakekalt,
			Forslag.Status.VenterPaSvar,
			-> {
				log.debug("Håndterer ikke forslag {} med status {}", forslag.id, forslag.status.javaClass.simpleName)
			}
		}
	}
}

private val stottedeTiltak =
	setOf(
		"INDOPPFAG",
		"ARBFORB",
		"AVKLARAG",
		"VASV",
		"ARBRRHDAG",
		"DIGIOPPARB",
		"JOBBK",
		"GRUPPEAMO",
		"GRUFAGYRKE",
	)

fun DeltakerlisteDto.skalLagres(): Boolean {
	if (!stottedeTiltak.contains(tiltakstype.arenaKode)) {
		return false
	}
	if (status == DeltakerlisteDto.Status.GJENNOMFORES) {
		return true
	} else if (status == DeltakerlisteDto.Status.AVSLUTTET &&
		sluttDato != null &&
		LocalDate
			.now()
			.isBefore(sluttDato.plusDays(15))
	) {
		return true
	}
	return false
}
