package no.nav.tiltaksarrangor.service

import no.nav.tiltaksarrangor.client.amttiltak.AmtTiltakClient
import no.nav.tiltaksarrangor.client.amttiltak.dto.toEndringsmelding
import no.nav.tiltaksarrangor.client.amttiltak.request.AvsluttDeltakelseRequest
import no.nav.tiltaksarrangor.client.amttiltak.request.DeltakerIkkeAktuellRequest
import no.nav.tiltaksarrangor.client.amttiltak.request.EndreDeltakelsesprosentRequest
import no.nav.tiltaksarrangor.client.amttiltak.request.EndreOppstartsdatoRequest
import no.nav.tiltaksarrangor.client.amttiltak.request.EndreSluttdatoRequest
import no.nav.tiltaksarrangor.client.amttiltak.request.ForlengDeltakelseRequest
import no.nav.tiltaksarrangor.client.amttiltak.request.LeggTilOppstartsdatoRequest
import no.nav.tiltaksarrangor.controller.request.EndringsmeldingRequest
import no.nav.tiltaksarrangor.model.Deltaker
import no.nav.tiltaksarrangor.model.DeltakerStatus
import no.nav.tiltaksarrangor.model.Endringsmelding
import no.nav.tiltaksarrangor.model.NavInformasjon
import no.nav.tiltaksarrangor.model.NavVeileder
import no.nav.tiltaksarrangor.model.Veileder
import no.nav.tiltaksarrangor.model.exceptions.UnauthorizedException
import no.nav.tiltaksarrangor.repositories.DeltakerRepository
import no.nav.tiltaksarrangor.repositories.EndringsmeldingRepository
import no.nav.tiltaksarrangor.repositories.model.AnsattDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerMedDeltakerlisteDbo
import no.nav.tiltaksarrangor.repositories.model.EndringsmeldingDbo
import no.nav.tiltaksarrangor.repositories.model.STATUSER_SOM_KAN_SKJULES
import no.nav.tiltaksarrangor.utils.erPilot
import no.nav.tiltaksarrangor.utils.isDev
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
	private val auditLoggerService: AuditLoggerService
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun getMineRoller(personIdent: String): List<String> {
		return ansattService.oppdaterOgHentMineRoller(personIdent).also { metricsService.incInnloggetAnsatt(roller = it) }
	}

	fun getDeltaker(personIdent: String, deltakerId: UUID): Deltaker {
		val ansatt = getAnsattMedRoller(personIdent)
		val deltakerMedDeltakerliste = deltakerRepository.getDeltakerMedDeltakerliste(deltakerId) ?: throw NoSuchElementException("Fant ikke deltaker med id $deltakerId")
		auditLoggerService.sendAuditLog(
			ansattPersonIdent = ansatt.personIdent,
			deltakerPersonIdent = deltakerMedDeltakerliste.deltaker.personident,
			arrangorId = deltakerMedDeltakerliste.deltakerliste.arrangorId
		)

		if (!ansattService.harTilgangTilDeltaker(deltakerId = deltakerId, deltakerlisteId = deltakerMedDeltakerliste.deltakerliste.id, deltakerlisteArrangorId = deltakerMedDeltakerliste.deltakerliste.arrangorId, ansattDbo = ansatt)) {
			throw UnauthorizedException("Ansatt ${ansatt.id} har ikke tilgang til deltaker med id $deltakerId")
		}

		if (deltakerMedDeltakerliste.deltaker.erSkjult()) {
			log.warn("Har forsøkt å hente deltaker som er fjernet")
			throw NoSuchElementException("Fant ikke deltaker med id $deltakerId")
		} else if (deltakerMedDeltakerliste.deltakerliste.erKurs && !(isDev() || erPilot(deltakerMedDeltakerliste.deltakerliste.id))) {
			log.warn("Har forsøkt å hente kurs-deltaker som ikke tilhører pilot")
			throw NoSuchElementException("Fant ikke deltaker med id $deltakerId")
		}

		val endringsmeldinger = endringsmeldingRepository.getEndringsmeldingerForDeltaker(deltakerId)
		val veiledere = ansattService.getVeiledereForDeltaker(deltakerId)

		return tilDeltaker(deltakerMedDeltakerliste, veiledere, endringsmeldinger)
	}

	fun fjernDeltaker(personIdent: String, deltakerId: UUID) {
		val ansatt = getAnsattMedRoller(personIdent)
		val deltakerMedDeltakerliste = deltakerRepository.getDeltakerMedDeltakerliste(deltakerId) ?: throw NoSuchElementException("Fant ikke deltaker med id $deltakerId")

		if (!ansattService.harTilgangTilDeltaker(deltakerId = deltakerId, deltakerlisteId = deltakerMedDeltakerliste.deltakerliste.id, deltakerlisteArrangorId = deltakerMedDeltakerliste.deltakerliste.arrangorId, ansattDbo = ansatt)) {
			throw UnauthorizedException("Ansatt ${ansatt.id} har ikke tilgang til deltaker med id $deltakerId")
		}

		if (!deltakerMedDeltakerliste.deltaker.erSkjult()) {
			if (kanSkjules(deltakerMedDeltakerliste.deltaker)) {
				amtTiltakClient.skjulDeltakerForTiltaksarrangor(deltakerId)
				deltakerRepository.skjulDeltaker(deltakerId = deltakerId, ansattId = ansatt.id)
			} else {
				throw IllegalStateException("Kan ikke skjule deltaker med id $deltakerId. Ugyldig status: ${deltakerMedDeltakerliste.deltaker.status.name}")
			}
		}
	}

	fun getAktiveEndringsmeldinger(deltakerId: UUID): List<Endringsmelding> {
		return amtTiltakClient.getAktiveEndringsmeldinger(deltakerId).map { it.toEndringsmelding() }
	}

	fun opprettEndringsmelding(deltakerId: UUID, request: EndringsmeldingRequest) {
		when (request.innhold.type) {
			EndringsmeldingRequest.EndringsmeldingType.LEGG_TIL_OPPSTARTSDATO ->
				amtTiltakClient.leggTilOppstartsdato(deltakerId, LeggTilOppstartsdatoRequest((request.innhold as EndringsmeldingRequest.Innhold.LeggTilOppstartsdatoInnhold).oppstartsdato))

			EndringsmeldingRequest.EndringsmeldingType.ENDRE_OPPSTARTSDATO ->
				amtTiltakClient.endreOppstartsdato(deltakerId, EndreOppstartsdatoRequest((request.innhold as EndringsmeldingRequest.Innhold.EndreOppstartsdatoInnhold).oppstartsdato))

			EndringsmeldingRequest.EndringsmeldingType.AVSLUTT_DELTAKELSE -> {
				val innhold = request.innhold as EndringsmeldingRequest.Innhold.AvsluttDeltakelseInnhold
				amtTiltakClient.avsluttDeltakelse(deltakerId, AvsluttDeltakelseRequest(innhold.sluttdato, innhold.aarsak))
			}
			EndringsmeldingRequest.EndringsmeldingType.FORLENG_DELTAKELSE -> amtTiltakClient.forlengDeltakelse(deltakerId, ForlengDeltakelseRequest((request.innhold as EndringsmeldingRequest.Innhold.ForlengDeltakelseInnhold).sluttdato))
			EndringsmeldingRequest.EndringsmeldingType.ENDRE_DELTAKELSE_PROSENT -> {
				val innhold = request.innhold as EndringsmeldingRequest.Innhold.EndreDeltakelseProsentInnhold
				amtTiltakClient.endreDeltakelsesprosent(deltakerId, EndreDeltakelsesprosentRequest(innhold.deltakelseProsent, innhold.dagerPerUke, innhold.gyldigFraDato))
			}
			EndringsmeldingRequest.EndringsmeldingType.DELTAKER_IKKE_AKTUELL -> amtTiltakClient.deltakerIkkeAktuell(deltakerId, DeltakerIkkeAktuellRequest((request.innhold as EndringsmeldingRequest.Innhold.DeltakerIkkeAktuellInnhold).aarsak))
			EndringsmeldingRequest.EndringsmeldingType.ENDRE_SLUTTDATO -> amtTiltakClient.endreSluttdato(deltakerId, EndreSluttdatoRequest((request.innhold as EndringsmeldingRequest.Innhold.EndreSluttdatoInnhold).sluttdato))
			EndringsmeldingRequest.EndringsmeldingType.DELTAKER_ER_AKTUELL -> amtTiltakClient.deltakerErAktuell(deltakerId)
		}
	}

	fun slettEndringsmelding(endringsmeldingId: UUID) {
		amtTiltakClient.tilbakekallEndringsmelding(endringsmeldingId)
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
		endringsmeldinger: List<EndringsmeldingDbo>
	): Deltaker {
		return Deltaker(
			id = deltakerMedDeltakerliste.deltaker.id,
			deltakerliste = Deltaker.Deltakerliste(
				id = deltakerMedDeltakerliste.deltakerliste.id,
				startDato = deltakerMedDeltakerliste.deltakerliste.startDato,
				sluttDato = deltakerMedDeltakerliste.deltakerliste.sluttDato,
				erKurs = deltakerMedDeltakerliste.deltakerliste.erKurs
			),
			fornavn = deltakerMedDeltakerliste.deltaker.fornavn,
			mellomnavn = deltakerMedDeltakerliste.deltaker.mellomnavn,
			etternavn = deltakerMedDeltakerliste.deltaker.etternavn,
			fodselsnummer = deltakerMedDeltakerliste.deltaker.personident,
			telefonnummer = deltakerMedDeltakerliste.deltaker.telefonnummer,
			epost = deltakerMedDeltakerliste.deltaker.epost,
			status = DeltakerStatus(
				type = deltakerMedDeltakerliste.deltaker.status,
				endretDato = deltakerMedDeltakerliste.deltaker.statusOpprettetDato
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
			navInformasjon = NavInformasjon(
				navkontor = deltakerMedDeltakerliste.deltaker.navKontor,
				navVeileder = deltakerMedDeltakerliste.deltaker.navVeilederId?.let {
					NavVeileder(
						navn = deltakerMedDeltakerliste.deltaker.navVeilederNavn ?: "",
						epost = deltakerMedDeltakerliste.deltaker.navVeilederEpost,
						telefon = deltakerMedDeltakerliste.deltaker.navVeilederTelefon
					)
				}
			),
			veiledere = veiledere,
			aktiveEndringsmeldinger = endringsmeldinger.map { it.toEndringsmelding() }
		)
	}
}
