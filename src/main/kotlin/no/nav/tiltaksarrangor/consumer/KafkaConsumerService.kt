package no.nav.tiltaksarrangor.consumer

import no.nav.amt.lib.models.arrangor.melding.EndringFraArrangor
import no.nav.amt.lib.models.arrangor.melding.Forslag
import no.nav.amt.lib.models.arrangor.melding.Melding
import no.nav.amt.lib.models.arrangor.melding.Vurdering
import no.nav.amt.lib.models.deltaker.DeltakerHistorikk
import no.nav.amt.lib.models.deltakerliste.tiltakstype.ArenaKode
import no.nav.amt.lib.models.tiltakskoordinator.EndringFraTiltakskoordinator
import no.nav.tiltaksarrangor.client.amtarrangor.AmtArrangorClient
import no.nav.tiltaksarrangor.client.amtarrangor.dto.toArrangorDbo
import no.nav.tiltaksarrangor.client.amtperson.AmtPersonClient
import no.nav.tiltaksarrangor.client.amtperson.NavEnhetDto
import no.nav.tiltaksarrangor.consumer.model.AVSLUTTENDE_STATUSER
import no.nav.tiltaksarrangor.consumer.model.AnsattDto
import no.nav.tiltaksarrangor.consumer.model.ArrangorDto
import no.nav.tiltaksarrangor.consumer.model.DeltakerDto
import no.nav.tiltaksarrangor.consumer.model.DeltakerStatus
import no.nav.tiltaksarrangor.consumer.model.DeltakerlisteDto
import no.nav.tiltaksarrangor.consumer.model.EndringsmeldingDto
import no.nav.tiltaksarrangor.consumer.model.NavAnsatt
import no.nav.tiltaksarrangor.consumer.model.NavEnhet
import no.nav.tiltaksarrangor.consumer.model.SKJULES_ALLTID_STATUSER
import no.nav.tiltaksarrangor.consumer.model.toAnsattDbo
import no.nav.tiltaksarrangor.consumer.model.toArrangorDbo
import no.nav.tiltaksarrangor.consumer.model.toDeltakerDbo
import no.nav.tiltaksarrangor.consumer.model.toEndringsmeldingDbo
import no.nav.tiltaksarrangor.melding.forslag.ForslagService
import no.nav.tiltaksarrangor.model.DeltakerStatusAarsak
import no.nav.tiltaksarrangor.model.Oppdatering
import no.nav.tiltaksarrangor.repositories.AnsattRepository
import no.nav.tiltaksarrangor.repositories.ArrangorRepository
import no.nav.tiltaksarrangor.repositories.DeltakerRepository
import no.nav.tiltaksarrangor.repositories.DeltakerlisteRepository
import no.nav.tiltaksarrangor.repositories.EndringsmeldingRepository
import no.nav.tiltaksarrangor.repositories.UlestEndringRepository
import no.nav.tiltaksarrangor.repositories.model.DAGER_AVSLUTTET_DELTAKER_VISES
import no.nav.tiltaksarrangor.repositories.model.DeltakerDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerlisteDbo
import no.nav.tiltaksarrangor.service.NavAnsattService
import no.nav.tiltaksarrangor.service.NavEnhetService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Component
class KafkaConsumerService(
	private val arrangorRepository: ArrangorRepository,
	private val ansattRepository: AnsattRepository,
	private val deltakerlisteRepository: DeltakerlisteRepository,
	private val deltakerRepository: DeltakerRepository,
	private val endringsmeldingRepository: EndringsmeldingRepository,
	private val amtArrangorClient: AmtArrangorClient,
	private val forslagService: ForslagService,
	private val navEnhetService: NavEnhetService,
	private val navAnsattService: NavAnsattService,
	private val ulestEndringRepository: UlestEndringRepository,
	private val amtPersonClient: AmtPersonClient,
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
		if (ansatt == null) {
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
		val gjennomforing = deltakerlisteRepository.getDeltakerliste(deltakerDto.deltakerlisteId)
		val erEnkeltplass = gjennomforing?.tiltakType?.toTiltaksKode()?.erEnkeltplass() == true
		if (deltakerDto.skalLagres(lagretDeltaker, erEnkeltplass)) {
			leggTilNavAnsattOgEnhetHistorikk(deltakerDto)

			if (lagretDeltaker == null) {
				val oppdatertKontaktinformasjon = amtPersonClient
					.hentOppdatertKontaktinfo(deltakerDto.personalia.personident)
					.getOrDefault(deltakerDto.personalia.kontaktinformasjon)

				deltakerRepository.insertOrUpdateDeltaker(
					deltakerDto
						.copy(personalia = deltakerDto.personalia.copy(kontaktinformasjon = oppdatertKontaktinformasjon))
						.toDeltakerDbo(null),
				)
				lagreNyDeltakerUlestEndring(deltakerDto, deltakerId)
			} else {
				lagreUlesteMeldinger(deltakerId, deltakerDto, lagretDeltaker)

				deltakerRepository.insertOrUpdateDeltaker(deltakerDto.toDeltakerDbo(lagretDeltaker))
			}
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

	private fun lagreNyDeltakerUlestEndring(deltakerDto: DeltakerDto, deltakerId: UUID) {
		val vedtak = deltakerDto.historikk?.filterIsInstance<DeltakerHistorikk.Vedtak>()
		val endring = deltakerDto.historikk?.filterIsInstance<DeltakerHistorikk.EndringFraTiltakskoordinator>()

		if (!endring.isNullOrEmpty()) {
			endring.forEach {
				lagreNyDeltakerUlestEndringForTiltakskoordinatorEndring(it.endringFraTiltakskoordinator, deltakerId)
			}
		} else if (deltakerDto.historikk == null || vedtak.isNullOrEmpty()) {
			ulestEndringRepository.insert(
				deltakerId,
				Oppdatering.NyDeltaker(
					opprettetAvNavn = null,
					opprettetAvEnhet = null,
					opprettet = deltakerDto.innsoktDato,
				),
			)
		} else {
			vedtak.minBy { it.vedtak.opprettet }.vedtak.let {
				ulestEndringRepository.insert(
					deltakerId,
					Oppdatering.NyDeltaker(
						opprettetAvNavn = navAnsattService.hentNavAnsatt(it.opprettetAv)?.navn,
						opprettetAvEnhet = navEnhetService.hentOpprettEllerOppdaterNavEnhet(it.opprettetAvEnhet).navn,
						opprettet = it.opprettet.toLocalDate(),
					),
				)
			}
		}
	}

	private fun lagreNyDeltakerUlestEndringForTiltakskoordinatorEndring(endring: EndringFraTiltakskoordinator, deltakerId: UUID) {
		val oppdatering = mapToTiltakskoordinatorOppdatering(endring, true)

		if (oppdatering != null) {
			ulestEndringRepository.insert(
				deltakerId,
				oppdatering,
			)
		}
	}

	private fun lagreUlesteMeldinger(
		deltakerId: UUID,
		deltakerDto: DeltakerDto,
		lagretDeltaker: DeltakerDbo,
	) {
		if (deltakerDto.navVeileder?.id != lagretDeltaker.navVeilederId) {
			ulestEndringRepository.insert(
				deltakerId,
				Oppdatering.NavEndring(
					nyNavVeileder = true,
					navVeilederNavn = deltakerDto.navVeileder?.navn,
					navVeilederEpost = deltakerDto.navVeileder?.epost,
					navVeilederTelefonnummer = deltakerDto.navVeileder?.telefonnummer,
					navEnhet = deltakerDto.navKontor,
				),
			)
		} else if (deltakerDto.navKontor != lagretDeltaker.navKontor) {
			lagreOppdateringNavEndring(
				deltaker = lagretDeltaker,
				nyttNavn = deltakerDto.navVeileder?.navn,
				nyEpost = deltakerDto.navVeileder?.epost,
				nyttTelefonnummer = deltakerDto.navVeileder?.telefonnummer,
				nyNavEnhet = deltakerDto.navKontor,
			)
		}

		val ulesteEndringerFraHistorikk = hentUlesteEndringerFraHistorikk(lagretDeltaker, deltakerDto)
		ulesteEndringerFraHistorikk.forEach {
			ulestEndringRepository.insert(
				deltakerId,
				it,
			)
		}
	}

	private fun hentUlesteEndringerFraHistorikk(lagretDeltaker: DeltakerDbo, nyDeltaker: DeltakerDto): List<Oppdatering> {
		if (nyDeltaker.historikk.isNullOrEmpty()) {
			return emptyList()
		}

		return nyDeltaker.historikk
			.minus(lagretDeltaker.historikk.toSet())
			.mapNotNull { toDeltakerOppdatering(it) }
	}

	private fun toDeltakerOppdatering(historikk: DeltakerHistorikk): Oppdatering? = when (historikk) {
		is DeltakerHistorikk.Endring -> Oppdatering.DeltakelsesEndring(historikk.endring)
		is DeltakerHistorikk.Forslag -> {
			when (historikk.forslag.status) {
				is Forslag.Status.Avvist -> Oppdatering.AvvistForslag(historikk.forslag)
				is Forslag.Status.Godkjent,
				is Forslag.Status.Erstattet,
				is Forslag.Status.Tilbakekalt,
				Forslag.Status.VenterPaSvar,
				-> null
			}
		}

		is DeltakerHistorikk.EndringFraTiltakskoordinator -> {
			mapToTiltakskoordinatorOppdatering(historikk.endringFraTiltakskoordinator, false)
		}

		is DeltakerHistorikk.InnsokPaaFellesOppstart,
		is DeltakerHistorikk.EndringFraArrangor,
		is DeltakerHistorikk.ImportertFraArena,
		is DeltakerHistorikk.Vedtak,
		is DeltakerHistorikk.VurderingFraArrangor,
		-> null
	}

	private fun mapToTiltakskoordinatorOppdatering(endring: EndringFraTiltakskoordinator, erNyDeltaker: Boolean): Oppdatering? =
		when (val e = endring.endring) {
			is EndringFraTiltakskoordinator.DelMedArrangor -> Oppdatering.DeltMedArrangor(
				deltAvNavn = navAnsattService.hentEllerOpprettNavAnsatt(endring.endretAv).navn,
				deltAvEnhet = navEnhetService.hentOpprettEllerOppdaterNavEnhet(endring.endretAvEnhet).navn,
				delt = endring.endret.toLocalDate(),
			)

			is EndringFraTiltakskoordinator.TildelPlass -> Oppdatering.TildeltPlass(
				tildeltPlassAvNavn = navAnsattService.hentEllerOpprettNavAnsatt(endring.endretAv).navn,
				tildeltPlassAvEnhet = navEnhetService.hentOpprettEllerOppdaterNavEnhet(endring.endretAvEnhet).navn,
				tildeltPlass = endring.endret.toLocalDate(),
				erNyDeltaker,
			)

			is EndringFraTiltakskoordinator.SettPaaVenteliste -> null
			is EndringFraTiltakskoordinator.Avslag -> Oppdatering.Avslag(
				endretAv = navAnsattService.hentEllerOpprettNavAnsatt(endring.endretAv).navn,
				endretAvEnhet = navEnhetService.hentOpprettEllerOppdaterNavEnhet(endring.endretAvEnhet).navn,
				aarsak = e.aarsak.toDeltakerStatusAarsak(),
				begrunnelse = e.begrunnelse,
			)
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
		lagreUlestEndringNavOppdatering(id)

		navAnsattService.upsert(navAnsatt)
	}

	private fun lagreUlestEndringNavOppdatering(navAnsattId: UUID) {
		val lagretNavAnsatt = navAnsattService.hentNavAnsatt(navAnsattId)
		if (lagretNavAnsatt != null) {
			deltakerRepository.getDeltakereMedNavAnsatt(navAnsattId).forEach {
				lagreNavOppdateringer(it, lagretNavAnsatt)
			}
		}
	}

	private fun lagreNavOppdateringer(deltaker: DeltakerDbo, navAnsatt: NavAnsatt) {
		val endretNavn = navAnsatt.navn != deltaker.navVeilederNavn
		val endretEpost = navAnsatt.epost != deltaker.navVeilederEpost
		val endretTelefonnummer = navAnsatt.telefon != deltaker.navVeilederTelefon
		val harEndringer = endretNavn || endretEpost || endretTelefonnummer
		if (!harEndringer) {
			return
		}

		ulestEndringRepository.insert(
			deltaker.id,
			Oppdatering.NavEndring(
				nyNavVeileder = false,
				navVeilederNavn = if (endretNavn) navAnsatt.navn else null,
				navVeilederEpost = if (endretEpost) navAnsatt.epost else null,
				navVeilederTelefonnummer = if (endretTelefonnummer) navAnsatt.telefon else null,
				navEnhet = null,
			),
		)
	}

	private fun lagreOppdateringNavEndring(
		deltaker: DeltakerDbo,
		nyttNavn: String?,
		nyEpost: String?,
		nyttTelefonnummer: String?,
		nyNavEnhet: String?,
	) {
		val endretNavn = nyttNavn != deltaker.navVeilederNavn
		val endretEpost = nyEpost != deltaker.navVeilederEpost
		val endretTelefonnummer = nyttTelefonnummer != deltaker.navVeilederTelefon
		val endretNavEnhet = nyNavEnhet != deltaker.navKontor
		val harEndringer = endretNavn || endretEpost || endretTelefonnummer || endretNavEnhet
		if (!harEndringer) {
			return
		}

		ulestEndringRepository.insert(
			deltaker.id,
			Oppdatering.NavEndring(
				nyNavVeileder = false,
				navVeilederNavn = if (endretNavn) nyttNavn else null,
				navVeilederEpost = if (endretEpost) nyEpost else null,
				navVeilederTelefonnummer = if (endretTelefonnummer) nyttTelefonnummer else null,
				navEnhet = if (endretNavEnhet) nyNavEnhet else null,
			),
		)
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

	private fun DeltakerDto.skalLagres(lagretDeltaker: DeltakerDbo?, erEnkeltplass: Boolean): Boolean {
		if (erEnkeltplass) {
			return false
		} else if (status.type in SKJULES_ALLTID_STATUSER) {
			return false
		} else if (status.type == DeltakerStatus.IKKE_AKTUELL && deltarPaKurs && lagretDeltaker == null) {
			return false
		} else if (status.type in AVSLUTTENDE_STATUSER) {
			return harNyligSluttet()
		} else if (status.type == DeltakerStatus.SOKT_INN) {
			return erManueltDeltMedArrangor
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
		tiltakNavn = getTiltakstypeNavn(deltakerlisteDto.tiltakstype),
		tiltakType = ArenaKode.valueOf(deltakerlisteDto.tiltakstype.arenaKode),
		startDato = deltakerlisteDto.startDato,
		sluttDato = deltakerlisteDto.sluttDato,
		erKurs = deltakerlisteDto.erKurs(),
		oppstartstype = deltakerlisteDto.oppstart,
		tilgjengeligForArrangorFraOgMedDato = deltakerlisteDto.tilgjengeligForArrangorFraOgMedDato,
	)

	private fun getTiltakstypeNavn(tiltakstype: DeltakerlisteDto.Tiltakstype): String {
		if (tiltakstype.navn == "Jobbklubb") {
			return "Jobbsøkerkurs"
		} else {
			return tiltakstype.navn
		}
	}

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
			is EndringFraArrangor,
			is Vurdering,
			-> {
			}
		}
	}

	private fun handleForslag(forslag: Forslag) {
		when (forslag.status) {
			is Forslag.Status.Avvist,
			is Forslag.Status.Godkjent,
			-> {
				forslagService.delete(forslag.id)
			}

			is Forslag.Status.Erstattet,
			is Forslag.Status.Tilbakekalt,
			Forslag.Status.VenterPaSvar,
			-> {
				log.debug("Håndterer ikke forslag {} med status {}", forslag.id, forslag.status.javaClass.simpleName)
			}
		}
	}

	@Transactional
	fun lagreNavEnhet(id: UUID, enhet: NavEnhetDto) {
		val opprinneligEnhet = navEnhetService.hentEnhet(id)
		if (opprinneligEnhet != null && opprinneligEnhet.navn != enhet.navn) {
			deltakerRepository.oppdaterEnhetsnavnForDeltakere(
				opprinneligEnhetsnavn = opprinneligEnhet.navn,
				nyttEnhetsnavn = enhet.navn,
			)
			log.info("Oppdaterer enhetsnavn for deltakere fra ${opprinneligEnhet.navn} til ${enhet.navn}")
		}

		navEnhetService.upsert(enhet.toModel())
		log.info("Lagret nav-enhet med id $id")
	}
}

private fun NavEnhetDto.toModel() = NavEnhet(
	id = id,
	enhetsnummer = enhetId,
	navn = navn,
)

fun DeltakerlisteDto.skalLagres(): Boolean {
	if (!tiltakstype.erStottet()) return false

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

fun EndringFraTiltakskoordinator.Avslag.Aarsak.toDeltakerStatusAarsak() = DeltakerStatusAarsak(
	type = DeltakerStatusAarsak.Type.valueOf(this.type.name),
	beskrivelse = this.beskrivelse,
)
