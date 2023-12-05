package no.nav.tiltaksarrangor.service

import no.nav.tiltaksarrangor.client.amttiltak.AmtTiltakClient
import no.nav.tiltaksarrangor.controller.request.RegistrerVurderingRequest
import no.nav.tiltaksarrangor.model.Adresse
import no.nav.tiltaksarrangor.model.Deltaker
import no.nav.tiltaksarrangor.model.DeltakerStatus
import no.nav.tiltaksarrangor.model.NavInformasjon
import no.nav.tiltaksarrangor.model.NavVeileder
import no.nav.tiltaksarrangor.model.StatusType
import no.nav.tiltaksarrangor.model.Veileder
import no.nav.tiltaksarrangor.model.Vurdering
import no.nav.tiltaksarrangor.model.Vurderingstype
import no.nav.tiltaksarrangor.model.exceptions.SkjultDeltakerException
import no.nav.tiltaksarrangor.model.exceptions.UnauthorizedException
import no.nav.tiltaksarrangor.model.exceptions.ValidationException
import no.nav.tiltaksarrangor.repositories.DeltakerRepository
import no.nav.tiltaksarrangor.repositories.EndringsmeldingRepository
import no.nav.tiltaksarrangor.repositories.model.AnsattDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerMedDeltakerlisteDbo
import no.nav.tiltaksarrangor.repositories.model.EndringsmeldingDbo
import no.nav.tiltaksarrangor.repositories.model.STATUSER_SOM_KAN_SKJULES
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class TiltaksarrangorService(
	private val amtTiltakClient: AmtTiltakClient,
	private val ansattService: AnsattService,
	private val metricsService: MetricsService,
	private val deltakerRepository: DeltakerRepository,
	private val endringsmeldingRepository: EndringsmeldingRepository,
	private val auditLoggerService: AuditLoggerService,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun getMineRoller(personIdent: String): List<String> {
		return ansattService.oppdaterOgHentMineRoller(personIdent).also { metricsService.incInnloggetAnsatt(roller = it) }
	}

	fun getDeltaker(
		personIdent: String,
		deltakerId: UUID,
	): Deltaker {
		val ansatt = getAnsattMedRoller(personIdent)
		val deltakerMedDeltakerliste =
			deltakerRepository.getDeltakerMedDeltakerliste(deltakerId)
				?: throw NoSuchElementException("Fant ikke deltaker med id $deltakerId")

		if (!deltakerMedDeltakerliste.deltaker.skalVises()) {
			throw NoSuchElementException("Fant ikke deltaker med id $deltakerId")
		}

		auditLoggerService.sendAuditLog(
			ansattPersonIdent = ansatt.personIdent,
			deltakerPersonIdent = deltakerMedDeltakerliste.deltaker.personident,
			arrangorId = deltakerMedDeltakerliste.deltakerliste.arrangorId,
		)

		if (!ansattService.harTilgangTilDeltaker(
				deltakerId = deltakerId,
				deltakerlisteId = deltakerMedDeltakerliste.deltakerliste.id,
				deltakerlisteArrangorId = deltakerMedDeltakerliste.deltakerliste.arrangorId,
				ansattDbo = ansatt,
			)
		) {
			throw UnauthorizedException("Ansatt ${ansatt.id} har ikke tilgang til deltaker med id $deltakerId")
		}

		if (deltakerMedDeltakerliste.deltaker.erSkjult()) {
			log.warn("Har forsøkt å hente deltaker som er fjernet")
			throw SkjultDeltakerException("Fant ikke deltaker med id $deltakerId")
		}

		val endringsmeldinger = endringsmeldingRepository.getEndringsmeldingerForDeltaker(deltakerId)
		val veiledere = ansattService.getVeiledereForDeltaker(deltakerId)

		return tilDeltaker(deltakerMedDeltakerliste, veiledere, endringsmeldinger)
	}

	fun registrerVurdering(
		personIdent: String,
		deltakerId: UUID,
		request: RegistrerVurderingRequest,
	) {
		val ansatt = getAnsattMedRoller(personIdent)
		val deltakerMedDeltakerliste =
			deltakerRepository.getDeltakerMedDeltakerliste(deltakerId)
				?: throw NoSuchElementException("Fant ikke deltaker med id $deltakerId")

		if (!ansattService.harTilgangTilDeltaker(
				deltakerId = deltakerId,
				deltakerlisteId = deltakerMedDeltakerliste.deltakerliste.id,
				deltakerlisteArrangorId = deltakerMedDeltakerliste.deltakerliste.arrangorId,
				ansattDbo = ansatt,
			)
		) {
			throw UnauthorizedException("Ansatt ${ansatt.id} har ikke tilgang til deltaker med id $deltakerId")
		}

		if (deltakerMedDeltakerliste.deltaker.erSkjult()) {
			log.warn("Har forsøkt å registrere vurdering for deltaker som er fjernet")
			throw SkjultDeltakerException("Fant ikke deltaker med id $deltakerId")
		}
		if (deltakerMedDeltakerliste.deltaker.status != StatusType.VURDERES) {
			throw IllegalStateException(
				"Kan ikke registrere vurdering for deltaker med id $deltakerId med annen status enn VURDERES. " +
					"Ugyldig status: ${deltakerMedDeltakerliste.deltaker.status.name}",
			)
		}
		if (request.vurderingstype == Vurderingstype.OPPFYLLER_IKKE_KRAVENE && request.begrunnelse.isNullOrEmpty()) {
			throw ValidationException("Kan ikke registrere vurdering for deltaker med id $deltakerId. Begrunnelse mangler.")
		}

		val oppdaterteVurderinger = amtTiltakClient.registrerVurdering(deltakerId, request)
		deltakerRepository.oppdaterVurderingerForDeltaker(deltakerId, oppdaterteVurderinger)
		metricsService.incVurderingOpprettet(request.vurderingstype)
		log.info("Registrert vurdering for deltaker med id $deltakerId")
	}

	fun fjernDeltaker(
		personIdent: String,
		deltakerId: UUID,
	) {
		val ansatt = getAnsattMedRoller(personIdent)
		val deltakerMedDeltakerliste =
			deltakerRepository.getDeltakerMedDeltakerliste(deltakerId)
				?: throw NoSuchElementException("Fant ikke deltaker med id $deltakerId")

		if (!ansattService.harTilgangTilDeltaker(
				deltakerId = deltakerId,
				deltakerlisteId = deltakerMedDeltakerliste.deltakerliste.id,
				deltakerlisteArrangorId = deltakerMedDeltakerliste.deltakerliste.arrangorId,
				ansattDbo = ansatt,
			)
		) {
			throw UnauthorizedException("Ansatt ${ansatt.id} har ikke tilgang til deltaker med id $deltakerId")
		}

		if (!deltakerMedDeltakerliste.deltaker.erSkjult()) {
			if (kanSkjules(deltakerMedDeltakerliste.deltaker)) {
				amtTiltakClient.skjulDeltakerForTiltaksarrangor(deltakerId)
				deltakerRepository.skjulDeltaker(deltakerId = deltakerId, ansattId = ansatt.id)
				metricsService.incFjernetDeltaker()
				log.info("Skjult deltaker med id $deltakerId")
			} else {
				throw IllegalStateException(
					"Kan ikke skjule deltaker med id $deltakerId. Ugyldig status: ${deltakerMedDeltakerliste.deltaker.status.name}",
				)
			}
		}
	}

	private fun getAnsattMedRoller(personIdent: String): AnsattDbo {
		val ansatt = ansattService.getAnsatt(personIdent) ?: throw UnauthorizedException("Ansatt finnes ikke")
		if (!ansattService.harRoller(ansatt.roller)) {
			throw UnauthorizedException("Ansatt ${ansatt.id} er ikke veileder eller koordinator hos noen arrangører")
		}
		return ansatt
	}

	private fun kanSkjules(deltakerDbo: DeltakerDbo): Boolean {
		return deltakerDbo.status in STATUSER_SOM_KAN_SKJULES
	}

	private fun tilDeltaker(
		deltakerMedDeltakerliste: DeltakerMedDeltakerlisteDbo,
		veiledere: List<Veileder>,
		endringsmeldinger: List<EndringsmeldingDbo>,
	): Deltaker {
		return Deltaker(
			id = deltakerMedDeltakerliste.deltaker.id,
			deltakerliste =
				Deltaker.Deltakerliste(
					id = deltakerMedDeltakerliste.deltakerliste.id,
					startDato = deltakerMedDeltakerliste.deltakerliste.startDato,
					sluttDato = deltakerMedDeltakerliste.deltakerliste.sluttDato,
					erKurs = deltakerMedDeltakerliste.deltakerliste.erKurs,
				),
			fornavn = deltakerMedDeltakerliste.deltaker.fornavn,
			mellomnavn = deltakerMedDeltakerliste.deltaker.mellomnavn,
			etternavn = deltakerMedDeltakerliste.deltaker.etternavn,
			fodselsnummer = deltakerMedDeltakerliste.deltaker.personident,
			telefonnummer = deltakerMedDeltakerliste.deltaker.telefonnummer,
			epost = deltakerMedDeltakerliste.deltaker.epost,
			status =
				DeltakerStatus(
					type = deltakerMedDeltakerliste.deltaker.status,
					endretDato = deltakerMedDeltakerliste.deltaker.statusOpprettetDato,
				),
			startDato = deltakerMedDeltakerliste.deltaker.startdato,
			sluttDato = deltakerMedDeltakerliste.deltaker.sluttdato,
			deltakelseProsent = deltakerMedDeltakerliste.deltaker.prosentStilling?.toInt(),
			dagerPerUke = deltakerMedDeltakerliste.deltaker.dagerPerUke,
			soktInnPa = deltakerMedDeltakerliste.deltakerliste.navn,
			soktInnDato = deltakerMedDeltakerliste.deltaker.innsoktDato.atStartOfDay(),
			tiltakskode = deltakerMedDeltakerliste.deltakerliste.tiltakType,
			bestillingTekst = deltakerMedDeltakerliste.deltaker.bestillingstekst,
			fjernesDato = deltakerMedDeltakerliste.deltaker.skalFjernesDato(),
			navInformasjon =
				NavInformasjon(
					navkontor = deltakerMedDeltakerliste.deltaker.navKontor,
					navVeileder =
						deltakerMedDeltakerliste.deltaker.navVeilederId?.let {
							NavVeileder(
								navn = deltakerMedDeltakerliste.deltaker.navVeilederNavn ?: "",
								epost = deltakerMedDeltakerliste.deltaker.navVeilederEpost,
								telefon = deltakerMedDeltakerliste.deltaker.navVeilederTelefon,
							)
						},
				),
			veiledere = veiledere,
			aktiveEndringsmeldinger = endringsmeldinger.filter { it.erAktiv() }.sortedBy { it.sendt }.map { it.toEndringsmelding() },
			historiskeEndringsmeldinger = endringsmeldinger.filter { !it.erAktiv() }.sortedByDescending { it.sendt }.map { it.toEndringsmelding() },
			adresse = deltakerMedDeltakerliste.getAdresse(),
			gjeldendeVurderingFraArrangor = deltakerMedDeltakerliste.deltaker.getGjeldendeVurdering(),
			historiskeVurderingerFraArrangor = deltakerMedDeltakerliste.deltaker.getHistoriskeVurderinger(),
		)
	}
}

fun DeltakerMedDeltakerlisteDbo.getAdresse(): Adresse? {
	if (deltaker.adresse == null) {
		return null
	}
	if (deltakerliste.skalViseAdresseForDeltaker()) {
		return if (deltaker.adresse.kontaktadresse != null) {
			deltaker.adresse.kontaktadresse.toAdresse()
		} else if (deltaker.adresse.oppholdsadresse != null) {
			deltaker.adresse.oppholdsadresse.toAdresse()
		} else if (deltaker.adresse.bostedsadresse != null) {
			deltaker.adresse.bostedsadresse.toAdresse()
		} else {
			null
		}
	}
	return null
}

fun DeltakerDbo.getGjeldendeVurdering(): Vurdering? {
	return vurderingerFraArrangor?.firstOrNull { it.gyldigTil == null }?.toVurdering()
}

fun DeltakerDbo.getHistoriskeVurderinger(): List<Vurdering>? {
	return vurderingerFraArrangor?.filter { it.gyldigTil != null }?.map { it.toVurdering() }
}
