package no.nav.tiltaksarrangor.veileder.service

import no.nav.tiltaksarrangor.ingest.model.AnsattRolle
import no.nav.tiltaksarrangor.model.DeltakerStatus
import no.nav.tiltaksarrangor.model.Endringsmelding
import no.nav.tiltaksarrangor.model.Veiledertype
import no.nav.tiltaksarrangor.model.exceptions.UnauthorizedException
import no.nav.tiltaksarrangor.repositories.DeltakerRepository
import no.nav.tiltaksarrangor.repositories.EndringsmeldingRepository
import no.nav.tiltaksarrangor.repositories.model.DeltakerMedDeltakerlisteDbo
import no.nav.tiltaksarrangor.repositories.model.EndringsmeldingDbo
import no.nav.tiltaksarrangor.repositories.model.VeilederDeltakerDbo
import no.nav.tiltaksarrangor.service.AnsattService
import no.nav.tiltaksarrangor.veileder.model.Deltaker
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class VeilederService(
	private val ansattService: AnsattService,
	private val deltakerRepository: DeltakerRepository,
	private val endringsmeldingRepository: EndringsmeldingRepository,
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

		if (deltakere.isEmpty()) {
			return emptyList()
		}
		val endringsmeldinger = endringsmeldingRepository.getEndringsmeldingerForDeltakere(deltakere.map { it.deltaker.id })

		return tilVeiledersDeltakere(deltakere, ansatt.veilederDeltakere, endringsmeldinger)
	}

	private fun tilVeiledersDeltakere(
		deltakere: List<DeltakerMedDeltakerlisteDbo>,
		ansattsVeilederDeltakere: List<VeilederDeltakerDbo>,
		endringsmeldinger: List<EndringsmeldingDbo>,
	): List<Deltaker> {
		return deltakere.map {
			Deltaker(
				id = it.deltaker.id,
				deltakerliste =
					Deltaker.Deltakerliste(
						id = it.deltakerliste.id,
						type = it.deltakerliste.cleanTiltaksnavn(),
						navn = it.deltakerliste.navn,
					),
				fornavn = it.deltaker.fornavn,
				mellomnavn = it.deltaker.mellomnavn,
				etternavn = it.deltaker.etternavn,
				fodselsnummer = it.deltaker.personident,
				status =
					DeltakerStatus(
						type = it.deltaker.status,
						endretDato = it.deltaker.statusGyldigFraDato,
					),
				startDato = it.deltaker.startdato,
				sluttDato = it.deltaker.sluttdato,
				veiledertype = getVeiledertype(it.deltaker.id, ansattsVeilederDeltakere),
				aktiveEndringsmeldinger = getEndringsmeldinger(it.deltaker.id, endringsmeldinger),
			)
		}
	}

	private fun getVeiledertype(deltakerId: UUID, ansattsVeilederDeltakere: List<VeilederDeltakerDbo>): Veiledertype {
		return ansattsVeilederDeltakere.find { it.deltakerId == deltakerId }?.veilederType
			?: throw IllegalStateException("Deltaker med id $deltakerId mangler fra listen, skal ikke kunne skje!")
	}

	private fun getEndringsmeldinger(deltakerId: UUID, endringsmeldinger: List<EndringsmeldingDbo>): List<Endringsmelding> {
		val endringsmeldingerForDeltaker = endringsmeldinger.filter { it.deltakerId == deltakerId }
		return endringsmeldingerForDeltaker.map { it.toEndringsmelding() }
	}
}
