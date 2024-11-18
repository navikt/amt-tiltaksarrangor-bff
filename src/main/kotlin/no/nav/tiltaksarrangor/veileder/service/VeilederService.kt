package no.nav.tiltaksarrangor.veileder.service

import no.nav.amt.lib.models.arrangor.melding.Forslag
import no.nav.tiltaksarrangor.ingest.model.AnsattRolle
import no.nav.tiltaksarrangor.melding.forslag.ForslagRepository
import no.nav.tiltaksarrangor.model.AktivEndring
import no.nav.tiltaksarrangor.model.DeltakerStatus
import no.nav.tiltaksarrangor.model.Endringsmelding
import no.nav.tiltaksarrangor.model.Veiledertype
import no.nav.tiltaksarrangor.model.exceptions.UnauthorizedException
import no.nav.tiltaksarrangor.model.getTypeFromEndringsmelding
import no.nav.tiltaksarrangor.model.getTypeFromForslag
import no.nav.tiltaksarrangor.repositories.DeltakerRepository
import no.nav.tiltaksarrangor.repositories.EndringsmeldingRepository
import no.nav.tiltaksarrangor.repositories.model.DeltakerMedDeltakerlisteDbo
import no.nav.tiltaksarrangor.repositories.model.EndringsmeldingDbo
import no.nav.tiltaksarrangor.repositories.model.VeilederDeltakerDbo
import no.nav.tiltaksarrangor.service.AnsattService
import no.nav.tiltaksarrangor.unleash.UnleashService
import no.nav.tiltaksarrangor.veileder.model.Deltaker
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class VeilederService(
	private val ansattService: AnsattService,
	private val deltakerRepository: DeltakerRepository,
	private val forslagRepository: ForslagRepository,
	private val endringsmeldingRepository: EndringsmeldingRepository,
	private val unleashService: UnleashService,
) {
	fun getMineDeltakere(personIdent: String): List<Deltaker> {
		val ansatt = ansattService.getAnsatt(personIdent) ?: throw UnauthorizedException("Ansatt finnes ikke")
		if (!ansattService.erVeileder(ansatt.roller)) {
			throw UnauthorizedException("Ansatt ${ansatt.id} er ikke veileder hos noen arrangører")
		}
		if (ansatt.veilederDeltakere.isEmpty()) {
			return emptyList()
		}

		val deltakere =
			deltakerRepository.getDeltakereMedDeltakerliste(ansatt.veilederDeltakere.map { it.deltakerId })
				.filter { ansattService.harRolleHosArrangor(it.deltakerliste.arrangorId, AnsattRolle.VEILEDER, ansatt.roller) }
				.filter { !it.deltaker.erSkjult() }
				.filter { it.deltaker.skalVises() }
				.filter { it.deltakerliste.erTilgjengeligForArrangor() }

		if (deltakere.isEmpty()) {
			return emptyList()
		}
		val endringsmeldinger = endringsmeldingRepository.getEndringsmeldingerForDeltakere(deltakere.map { it.deltaker.id })
		val aktiveForslag = forslagRepository.getAktiveForslagForDeltakere(deltakere.map { it.deltaker.id })

		return tilVeiledersDeltakere(deltakere, ansatt.veilederDeltakere, endringsmeldinger, aktiveForslag)
	}

	private fun tilVeiledersDeltakere(
		deltakere: List<DeltakerMedDeltakerlisteDbo>,
		ansattsVeilederDeltakere: List<VeilederDeltakerDbo>,
		endringsmeldinger: List<EndringsmeldingDbo>,
		aktiveForslag: List<Forslag>,
	): List<Deltaker> {
		return deltakere.map {
			val adressebeskyttet = it.deltaker.adressebeskyttet
			Deltaker(
				id = it.deltaker.id,
				deltakerliste =
					Deltaker.Deltakerliste(
						id = it.deltakerliste.id,
						type = it.deltakerliste.tiltakNavn,
						navn = it.deltakerliste.navn,
					),
				fornavn = if (adressebeskyttet) "" else it.deltaker.fornavn,
				mellomnavn = if (adressebeskyttet) null else it.deltaker.mellomnavn,
				etternavn = if (adressebeskyttet) "" else it.deltaker.etternavn,
				fodselsnummer = if (adressebeskyttet) "" else it.deltaker.personident,
				status =
					DeltakerStatus(
						type = it.deltaker.status,
						endretDato = it.deltaker.statusGyldigFraDato,
						aarsak = it.deltaker.statusAarsak,
					),
				startDato = it.deltaker.startdato,
				sluttDato = it.deltaker.sluttdato,
				veiledertype = getVeiledertype(it.deltaker.id, ansattsVeilederDeltakere),
				aktiveEndringsmeldinger = getEndringsmeldinger(it.deltaker.id, endringsmeldinger),
				aktivEndring = getAktivEndring(it.deltaker.id, endringsmeldinger, aktiveForslag, it.deltakerliste.tiltakType),
				sistEndret = it.deltaker.sistEndret,
				adressebeskyttet = adressebeskyttet,
			)
		}
	}

	private fun getVeiledertype(deltakerId: UUID, ansattsVeilederDeltakere: List<VeilederDeltakerDbo>): Veiledertype {
		return ansattsVeilederDeltakere.find { it.deltakerId == deltakerId }?.veilederType
			?: throw IllegalStateException("Deltaker med id $deltakerId mangler fra listen, skal ikke kunne skje!")
	}

	fun getAktivEndring(
		deltakerId: UUID,
		endringsmeldinger: List<EndringsmeldingDbo>,
		aktiveForslag: List<Forslag>,
		tiltakstype: String,
	): AktivEndring? {
		val aktiveForslagForDeltaker = getAktiveForslag(deltakerId, aktiveForslag)
		if (aktiveForslagForDeltaker.isNotEmpty()) {
			return aktiveForslagForDeltaker.map {
				AktivEndring(
					deltakerId,
					endingsType = getTypeFromForslag(it.endring),
					type = AktivEndring.Type.Forslag,
					sendt = it.opprettet.toLocalDate(),
				)
			}.maxByOrNull { it.sendt }
		}
		if (!unleashService.erKometMasterForTiltakstype(tiltakstype)) {
			val endringsmeldingerForDeltaker = getEndringsmeldinger(deltakerId, endringsmeldinger)
			if (endringsmeldingerForDeltaker.isNotEmpty()) {
				return endringsmeldingerForDeltaker.map {
					AktivEndring(
						deltakerId,
						endingsType = getTypeFromEndringsmelding(it.type),
						type = AktivEndring.Type.Endringsmelding,
						sendt = it.sendt,
					)
				}.maxBy { it.sendt }
			}
		}
		return null
	}

	private fun getEndringsmeldinger(deltakerId: UUID, endringsmeldinger: List<EndringsmeldingDbo>): List<Endringsmelding> {
		val endringsmeldingerForDeltaker = endringsmeldinger.filter { it.deltakerId == deltakerId }
		return endringsmeldingerForDeltaker.map { it.toEndringsmelding() }
	}

	private fun getAktiveForslag(deltakerId: UUID, aktiveForslag: List<Forslag>): List<Forslag> {
		return aktiveForslag.filter { it.deltakerId == deltakerId }
	}
}
