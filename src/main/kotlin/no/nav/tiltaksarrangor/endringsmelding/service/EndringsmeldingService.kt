package no.nav.tiltaksarrangor.endringsmelding.service

import no.nav.tiltaksarrangor.client.amttiltak.AmtTiltakClient
import no.nav.tiltaksarrangor.client.amttiltak.request.AvsluttDeltakelseRequest
import no.nav.tiltaksarrangor.client.amttiltak.request.DeltakerIkkeAktuellRequest
import no.nav.tiltaksarrangor.client.amttiltak.request.EndreDeltakelsesprosentRequest
import no.nav.tiltaksarrangor.client.amttiltak.request.EndreOppstartsdatoRequest
import no.nav.tiltaksarrangor.client.amttiltak.request.EndreSluttaarsakRequest
import no.nav.tiltaksarrangor.client.amttiltak.request.EndreSluttdatoRequest
import no.nav.tiltaksarrangor.client.amttiltak.request.ForlengDeltakelseRequest
import no.nav.tiltaksarrangor.client.amttiltak.request.LeggTilOppstartsdatoRequest
import no.nav.tiltaksarrangor.endringsmelding.controller.request.EndringsmeldingRequest
import no.nav.tiltaksarrangor.endringsmelding.controller.request.toEndringsmeldingDbo
import no.nav.tiltaksarrangor.endringsmelding.controller.response.EndringsmeldingResponse
import no.nav.tiltaksarrangor.model.StatusType
import no.nav.tiltaksarrangor.model.exceptions.SkjultDeltakerException
import no.nav.tiltaksarrangor.model.exceptions.UnauthorizedException
import no.nav.tiltaksarrangor.model.exceptions.ValidationException
import no.nav.tiltaksarrangor.repositories.DeltakerRepository
import no.nav.tiltaksarrangor.repositories.EndringsmeldingRepository
import no.nav.tiltaksarrangor.repositories.model.AnsattDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerMedDeltakerlisteDbo
import no.nav.tiltaksarrangor.service.AnsattService
import no.nav.tiltaksarrangor.service.MetricsService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class EndringsmeldingService(
	private val amtTiltakClient: AmtTiltakClient,
	private val ansattService: AnsattService,
	private val endringsmeldingRepository: EndringsmeldingRepository,
	private val deltakerRepository: DeltakerRepository,
	private val metricsService: MetricsService,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun getAlleEndringsmeldinger(deltakerId: UUID, personIdent: String): EndringsmeldingResponse {
		val ansatt = getAnsattMedRoller(personIdent)
		val deltakerMedDeltakerliste =
			deltakerRepository.getDeltakerMedDeltakerliste(deltakerId)?.takeIf { it.deltakerliste.erTilgjengeligForArrangor() }
				?: throw NoSuchElementException("Fant ikke deltaker med id $deltakerId")
		val harTilgangTilDeltaker =
			ansattService.harTilgangTilDeltaker(
				deltakerId = deltakerId,
				deltakerlisteId = deltakerMedDeltakerliste.deltakerliste.id,
				deltakerlisteArrangorId = deltakerMedDeltakerliste.deltakerliste.arrangorId,
				ansattDbo = ansatt,
			)
		if (!harTilgangTilDeltaker || !ansattService.harTilgangTilEndringsmeldingerOgVurderingForDeltaker(deltakerMedDeltakerliste, ansatt)) {
			throw UnauthorizedException("Ansatt ${ansatt.id} har ikke tilgang til deltaker med id $deltakerId")
		}
		if (deltakerMedDeltakerliste.deltaker.erSkjult()) {
			throw SkjultDeltakerException("Deltaker med id $deltakerId er skjult for tiltaksarrangør")
		}
		val endringsmeldinger = endringsmeldingRepository.getEndringsmeldingerForDeltaker(deltakerId)
		return EndringsmeldingResponse(
			aktiveEndringsmeldinger = endringsmeldinger.sortedBy { it.sendt }.filter { it.erAktiv() }
				.map { it.toEndringsmelding() },
			historiskeEndringsmeldinger = endringsmeldinger.sortedByDescending { it.sendt }.filter { !it.erAktiv() }
				.map { it.toEndringsmelding() },
		)
	}

	fun opprettEndringsmelding(
		deltakerId: UUID,
		request: EndringsmeldingRequest,
		personIdent: String,
	) {
		val ansatt = getAnsattMedRoller(personIdent)
		val deltakerMedDeltakerliste =
			deltakerRepository.getDeltakerMedDeltakerliste(deltakerId)?.takeIf { it.deltakerliste.erTilgjengeligForArrangor() }
				?: throw NoSuchElementException("Fant ikke deltaker med id $deltakerId")
		val harTilgangTilDeltaker =
			ansattService.harTilgangTilDeltaker(
				deltakerId = deltakerId,
				deltakerlisteId = deltakerMedDeltakerliste.deltakerliste.id,
				deltakerlisteArrangorId = deltakerMedDeltakerliste.deltakerliste.arrangorId,
				ansattDbo = ansatt,
			)
		if (!harTilgangTilDeltaker || !ansattService.harTilgangTilEndringsmeldingerOgVurderingForDeltaker(deltakerMedDeltakerliste, ansatt)) {
			throw UnauthorizedException("Ansatt ${ansatt.id} har ikke tilgang til deltaker med id $deltakerId")
		}
		if (deltakerMedDeltakerliste.deltaker.erSkjult()) {
			throw SkjultDeltakerException("Deltaker med id $deltakerId er skjult for tiltaksarrangør")
		}

		val endringsmeldingId =
			when (request.innhold.type) {
				EndringsmeldingRequest.EndringsmeldingType.LEGG_TIL_OPPSTARTSDATO ->
					amtTiltakClient.leggTilOppstartsdato(
						deltakerId,
						LeggTilOppstartsdatoRequest((request.innhold as EndringsmeldingRequest.Innhold.LeggTilOppstartsdatoInnhold).oppstartsdato),
					)

				EndringsmeldingRequest.EndringsmeldingType.ENDRE_OPPSTARTSDATO ->
					amtTiltakClient.endreOppstartsdato(
						deltakerId,
						EndreOppstartsdatoRequest((request.innhold as EndringsmeldingRequest.Innhold.EndreOppstartsdatoInnhold).oppstartsdato),
					)

				EndringsmeldingRequest.EndringsmeldingType.AVSLUTT_DELTAKELSE -> {
					val innhold = request.innhold as EndringsmeldingRequest.Innhold.AvsluttDeltakelseInnhold
					amtTiltakClient.avsluttDeltakelse(
						deltakerId,
						AvsluttDeltakelseRequest(innhold.sluttdato, innhold.aarsak),
					)
				}

				EndringsmeldingRequest.EndringsmeldingType.FORLENG_DELTAKELSE ->
					amtTiltakClient.forlengDeltakelse(
						deltakerId,
						ForlengDeltakelseRequest((request.innhold as EndringsmeldingRequest.Innhold.ForlengDeltakelseInnhold).sluttdato),
					)

				EndringsmeldingRequest.EndringsmeldingType.ENDRE_DELTAKELSE_PROSENT -> {
					val innhold = request.innhold as EndringsmeldingRequest.Innhold.EndreDeltakelseProsentInnhold
					amtTiltakClient.endreDeltakelsesprosent(
						deltakerId,
						EndreDeltakelsesprosentRequest(
							innhold.deltakelseProsent,
							innhold.dagerPerUke,
							innhold.gyldigFraDato,
						),
					)
				}

				EndringsmeldingRequest.EndringsmeldingType.DELTAKER_IKKE_AKTUELL ->
					amtTiltakClient.deltakerIkkeAktuell(
						deltakerId,
						DeltakerIkkeAktuellRequest((request.innhold as EndringsmeldingRequest.Innhold.DeltakerIkkeAktuellInnhold).aarsak),
					)

				EndringsmeldingRequest.EndringsmeldingType.ENDRE_SLUTTDATO ->
					amtTiltakClient.endreSluttdato(
						deltakerId,
						EndreSluttdatoRequest((request.innhold as EndringsmeldingRequest.Innhold.EndreSluttdatoInnhold).sluttdato),
					)

				EndringsmeldingRequest.EndringsmeldingType.ENDRE_SLUTTAARSAK ->
					endreSluttaarsak(
						deltakerMedDeltakerliste,
						request.innhold as EndringsmeldingRequest.Innhold.EndreSluttaarsakInnhold,
					)
			}

		endringsmeldingRepository.lagreNyOgMerkAktiveEndringsmeldingMedSammeTypeSomUtfort(
			request.toEndringsmeldingDbo(endringsmeldingId = endringsmeldingId, deltakerId = deltakerId),
		)
		log.info("Endringsmelding av type ${request.innhold.type.name} opprettet med id $endringsmeldingId for deltaker $deltakerId")
	}

	fun slettEndringsmelding(endringsmeldingId: UUID, personIdent: String) {
		val ansatt = getAnsattMedRoller(personIdent)
		val endringsmeldingMedDeltakerOgDeltakerliste =
			endringsmeldingRepository.getEndringsmeldingMedDeltakerOgDeltakerliste(endringsmeldingId)?.takeIf {
				it.deltakerlisteDbo.erTilgjengeligForArrangor()
			}
				?: throw NoSuchElementException("Fant ikke endringsmelding med id $endringsmeldingId")

		val harTilgangTilDeltaker =
			ansattService.harTilgangTilDeltaker(
				deltakerId = endringsmeldingMedDeltakerOgDeltakerliste.deltakerDbo.id,
				deltakerlisteId = endringsmeldingMedDeltakerOgDeltakerliste.deltakerlisteDbo.id,
				deltakerlisteArrangorId = endringsmeldingMedDeltakerOgDeltakerliste.deltakerlisteDbo.arrangorId,
				ansattDbo = ansatt,
			)
		if (!harTilgangTilDeltaker || !ansattService.harTilgangTilEndringsmeldingerOgVurderingForDeltaker(
				DeltakerMedDeltakerlisteDbo(
					endringsmeldingMedDeltakerOgDeltakerliste.deltakerDbo,
					endringsmeldingMedDeltakerOgDeltakerliste.deltakerlisteDbo,
				),
				ansatt,
			)
		) {
			throw UnauthorizedException(
				"Ansatt ${ansatt.id} har ikke tilgang til deltaker med id ${endringsmeldingMedDeltakerOgDeltakerliste.deltakerDbo.id}",
			)
		}

		amtTiltakClient.tilbakekallEndringsmelding(endringsmeldingId)
		endringsmeldingRepository.tilbakekallEndringsmelding(endringsmeldingId)
		metricsService.incTilbakekaltEndringsmelding()
		log.info("Tilbakekalt endringsmelding med id $endringsmeldingId")
	}

	private fun getAnsattMedRoller(personIdent: String): AnsattDbo {
		val ansatt = ansattService.getAnsatt(personIdent) ?: throw UnauthorizedException("Ansatt finnes ikke")
		if (!ansattService.harRoller(ansatt.roller)) {
			throw UnauthorizedException("Ansatt ${ansatt.id} er ikke veileder eller koordinator hos noen arrangører")
		}
		return ansatt
	}

	private fun endreSluttaarsak(
		deltakerMedDeltakerliste: DeltakerMedDeltakerlisteDbo,
		innhold: EndringsmeldingRequest.Innhold.EndreSluttaarsakInnhold,
	): UUID {
		if (deltakerMedDeltakerliste.deltaker.status != StatusType.HAR_SLUTTET || deltakerMedDeltakerliste.deltakerliste.erKurs) {
			throw ValidationException(
				"Kan ikke endre sluttaarsak på deltaker som har status ${deltakerMedDeltakerliste.deltaker.status} eller deltar på et kurs",
			)
		}

		return amtTiltakClient.endreSluttaarsak(
			deltakerMedDeltakerliste.deltaker.id,
			EndreSluttaarsakRequest(innhold.aarsak),
		)
	}
}
