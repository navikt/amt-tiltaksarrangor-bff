package no.nav.tiltaksarrangor.koordinator.service

import no.nav.tiltaksarrangor.ingest.model.AnsattRolle
import no.nav.tiltaksarrangor.koordinator.model.Deltaker
import no.nav.tiltaksarrangor.koordinator.model.Deltakerliste
import no.nav.tiltaksarrangor.koordinator.model.Koordinator
import no.nav.tiltaksarrangor.koordinator.model.KoordinatorFor
import no.nav.tiltaksarrangor.koordinator.model.LeggTilVeiledereRequest
import no.nav.tiltaksarrangor.koordinator.model.MineDeltakerlister
import no.nav.tiltaksarrangor.koordinator.model.TilgjengeligVeileder
import no.nav.tiltaksarrangor.koordinator.model.VeilederFor
import no.nav.tiltaksarrangor.model.DeltakerStatus
import no.nav.tiltaksarrangor.model.Endringsmelding
import no.nav.tiltaksarrangor.model.Veileder
import no.nav.tiltaksarrangor.model.Veiledertype
import no.nav.tiltaksarrangor.model.exceptions.UnauthorizedException
import no.nav.tiltaksarrangor.model.exceptions.ValidationException
import no.nav.tiltaksarrangor.repositories.ArrangorRepository
import no.nav.tiltaksarrangor.repositories.DeltakerRepository
import no.nav.tiltaksarrangor.repositories.DeltakerlisteRepository
import no.nav.tiltaksarrangor.repositories.EndringsmeldingRepository
import no.nav.tiltaksarrangor.repositories.model.AnsattDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerlisteDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerlisteMedArrangorDbo
import no.nav.tiltaksarrangor.repositories.model.EndringsmeldingDbo
import no.nav.tiltaksarrangor.repositories.model.KoordinatorDeltakerlisteDbo
import no.nav.tiltaksarrangor.repositories.model.VeilederDeltakerDbo
import no.nav.tiltaksarrangor.repositories.model.VeilederForDeltakerDbo
import no.nav.tiltaksarrangor.service.AnsattService
import no.nav.tiltaksarrangor.service.MetricsService
import no.nav.tiltaksarrangor.service.getGjeldendeVurdering
import no.nav.tiltaksarrangor.unleash.UnleashService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class KoordinatorService(
	private val ansattService: AnsattService,
	private val deltakerlisteRepository: DeltakerlisteRepository,
	private val arrangorRepository: ArrangorRepository,
	private val deltakerRepository: DeltakerRepository,
	private val endringsmeldingRepository: EndringsmeldingRepository,
	private val metricsService: MetricsService,
	private val unleashService: UnleashService
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun getMineDeltakerlister(personIdent: String): MineDeltakerlister {
		val ansatt = ansattService.getAnsatt(personIdent) ?: throw UnauthorizedException("Ansatt finnes ikke")
		if (!ansattService.harRoller(ansatt.roller)) {
			throw UnauthorizedException("Ansatt ${ansatt.id} er ikke veileder eller koordinator")
		}
		val veilederFor = if (ansattService.erVeileder(ansatt.roller)) {
			getVeilederFor(ansatt.veilederDeltakere)
		} else {
			null
		}
		val koordinatorFor = if (ansattService.erKoordinator(ansatt.roller)) {
			getKoordinatorFor(ansatt.deltakerlister)
		} else {
			null
		}
		return MineDeltakerlister(
			veilederFor = veilederFor,
			koordinatorFor = koordinatorFor
		)
	}

	fun getTilgjengeligeVeiledere(deltakerlisteId: UUID, personIdent: String): List<TilgjengeligVeileder> {
		val ansatt = getAnsattMedKoordinatorRoller(personIdent)
		val deltakerliste = deltakerlisteRepository.getDeltakerliste(deltakerlisteId)
			?: throw NoSuchElementException("Fant ikke deltakerliste med id $deltakerlisteId")

		val harKoordinatorRolleHosArrangor = ansattService.harRolleHosArrangor(
			arrangorId = deltakerliste.arrangorId,
			rolle = AnsattRolle.KOORDINATOR,
			roller = ansatt.roller
		)

		if (harKoordinatorRolleHosArrangor && ansattService.deltakerlisteErLagtTil(ansatt, deltakerlisteId)) {
			return ansattService.getVeiledereForArrangor(deltakerliste.arrangorId).map {
				TilgjengeligVeileder(
					ansattId = it.id,
					fornavn = it.fornavn,
					mellomnavn = it.mellomnavn,
					etternavn = it.etternavn
				)
			}
		} else {
			throw UnauthorizedException("Ansatt ${ansatt.id} har ikke tilgang til deltakerliste med id $deltakerlisteId")
		}
	}

	fun tildelVeiledereForDeltaker(deltakerId: UUID, request: LeggTilVeiledereRequest, personIdent: String) {
		val ansatt = getAnsattMedKoordinatorRoller(personIdent)
		val deltakerMedDeltakerlisteDbo = deltakerRepository.getDeltakerMedDeltakerliste(deltakerId)
			?: throw NoSuchElementException("Fant ikke deltaker med id $deltakerId")

		val harKoordinatorRolleHosArrangor = ansattService.harRolleHosArrangor(
			arrangorId = deltakerMedDeltakerlisteDbo.deltakerliste.arrangorId,
			rolle = AnsattRolle.KOORDINATOR,
			roller = ansatt.roller
		)
		if (harKoordinatorRolleHosArrangor && ansattService.deltakerlisteErLagtTil(ansatt, deltakerMedDeltakerlisteDbo.deltakerliste.id)) {
			validerRequest(request)
			if (!ansattService.erAlleAnsatteVeiledereHosArrangor(
					ansattIder = request.veiledere.map { it.ansattId },
					arrangorId = deltakerMedDeltakerlisteDbo.deltakerliste.arrangorId
				)
			) {
				throw UnauthorizedException("Alle ansatte må ha veileder-rolle hos arrangør")
			}
			ansattService.tildelVeiledereForDeltaker(
				deltakerId = deltakerId,
				arrangorId = deltakerMedDeltakerlisteDbo.deltakerliste.arrangorId,
				veiledereForDeltaker = request.veiledere.map {
					VeilederForDeltakerDbo(
						ansattId = it.ansattId,
						veilederType = it.toVeiledertype()
					)
				}
			)
			metricsService.incTildeltVeileder()
			log.info("Lagt til veiledere for deltaker $deltakerId")
		} else {
			throw UnauthorizedException("Ansatt ${ansatt.id} har ikke tilgang til å tildele veileder for deltaker med id $deltakerId")
		}
	}

	private fun validerRequest(request: LeggTilVeiledereRequest) {
		if (request.veiledere.count { it.erMedveileder } > 3) {
			throw ValidationException("Deltakere kan ikke ha flere enn 3 medveiledere")
		}
		if (request.veiledere.count { !it.erMedveileder } > 1) {
			throw ValidationException("Deltakere kan ikke ha flere enn en veileder")
		}
		val unikeAnsatte = request.veiledere.distinctBy { it.ansattId }
		if (request.veiledere.size > unikeAnsatte.size) {
			throw ValidationException("Ansatt kan ikke ha flere veilederroller for en deltaker")
		}
	}

	fun getDeltakerliste(deltakerlisteId: UUID, personIdent: String): Deltakerliste {
		val ansatt = getAnsattMedKoordinatorRoller(personIdent)
		val deltakerlisteMedArrangor = deltakerlisteRepository.getDeltakerlisteMedArrangor(deltakerlisteId)
			?: throw NoSuchElementException("Fant ikke deltakerliste med id $deltakerlisteId")

		val harKoordinatorRolleHosArrangor = ansattService.harRolleHosArrangor(
			arrangorId = deltakerlisteMedArrangor.deltakerlisteDbo.arrangorId,
			rolle = AnsattRolle.KOORDINATOR,
			roller = ansatt.roller
		)
		if (harKoordinatorRolleHosArrangor && (!deltakerlisteMedArrangor.deltakerlisteDbo.erKurs || unleashService.skalViseKurs(deltakerlisteId))) {
			if (ansattService.deltakerlisteErLagtTil(ansatt, deltakerlisteId)) {
				return getDeltakerliste(deltakerlisteMedArrangor)
			} else {
				throw UnauthorizedException("Ansatt ${ansatt.id} kan ikke hente deltakerliste med id $deltakerlisteId før den er lagt til")
			}
		} else {
			throw UnauthorizedException("Ansatt ${ansatt.id} har ikke tilgang til deltakerliste med id $deltakerlisteId")
		}
	}

	private fun getDeltakerliste(deltakerlisteMedArrangor: DeltakerlisteMedArrangorDbo): Deltakerliste {
		val overordnetArrangor = deltakerlisteMedArrangor.arrangorDbo.overordnetArrangorId?.let { arrangorRepository.getArrangor(it) }
		val koordinatorer = ansattService.getKoordinatorerForDeltakerliste(
			deltakerlisteId = deltakerlisteMedArrangor.deltakerlisteDbo.id,
			arrangorId = deltakerlisteMedArrangor.arrangorDbo.id
		)
			.sortedBy { it.etternavn } // sorteringen er kun for KoordinatorControllerTest sin skyld

		val deltakere = deltakerRepository.getDeltakereForDeltakerliste(deltakerlisteMedArrangor.deltakerlisteDbo.id)
			.filter { !it.erSkjult() }
			.filter { it.skalVises() }

		val veiledereForDeltakerliste = ansattService.getVeiledereForDeltakere(deltakere.map { it.id })
		val endringsmeldinger = endringsmeldingRepository.getEndringsmeldingerForDeltakere(deltakere.map { it.id })

		return Deltakerliste(
			id = deltakerlisteMedArrangor.deltakerlisteDbo.id,
			navn = deltakerlisteMedArrangor.deltakerlisteDbo.navn,
			tiltaksnavn = deltakerlisteMedArrangor.deltakerlisteDbo.cleanTiltaksnavn(),
			arrangorNavn = overordnetArrangor?.navn ?: deltakerlisteMedArrangor.arrangorDbo.navn,
			startDato = deltakerlisteMedArrangor.deltakerlisteDbo.startDato,
			sluttDato = deltakerlisteMedArrangor.deltakerlisteDbo.sluttDato,
			status = Deltakerliste.Status.valueOf(deltakerlisteMedArrangor.deltakerlisteDbo.status.name),
			koordinatorer = koordinatorer.map {
				Koordinator(
					fornavn = it.fornavn,
					mellomnavn = it.mellomnavn,
					etternavn = it.etternavn
				)
			},
			deltakere = tilKoordinatorsDeltakere(deltakere, veiledereForDeltakerliste, endringsmeldinger),
			erKurs = deltakerlisteMedArrangor.deltakerlisteDbo.erKurs,
			tiltakType = deltakerlisteMedArrangor.deltakerlisteDbo.tiltakType
		)
	}

	private fun getVeilederFor(veilederDeltakere: List<VeilederDeltakerDbo>): VeilederFor {
		return VeilederFor(
			veilederFor = veilederDeltakere.filter { it.veilederType == Veiledertype.VEILEDER }.size,
			medveilederFor = veilederDeltakere.filter { it.veilederType == Veiledertype.MEDVEILEDER }.size
		)
	}

	private fun getKoordinatorFor(koordinatorsDeltakerlister: List<KoordinatorDeltakerlisteDbo>): KoordinatorFor {
		val deltakerlister = deltakerlisteRepository.getDeltakerlister(koordinatorsDeltakerlister.map { it.deltakerlisteId }).toDeltakerliste()
		return KoordinatorFor(deltakerlister = deltakerlister.filter { !it.erKurs || unleashService.skalViseKurs(it.id) })
	}

	private fun getAnsattMedKoordinatorRoller(personIdent: String): AnsattDbo {
		val ansatt = ansattService.getAnsatt(personIdent) ?: throw UnauthorizedException("Ansatt finnes ikke")
		if (!ansattService.erKoordinator(ansatt.roller)) {
			throw UnauthorizedException("Ansatt ${ansatt.id} er ikke koordinator hos noen arrangører")
		}
		return ansatt
	}

	private fun tilKoordinatorsDeltakere(
		deltakere: List<DeltakerDbo>,
		veiledere: List<Veileder>,
		endringsmeldinger: List<EndringsmeldingDbo>
	): List<Deltaker> {
		return deltakere.map {
			Deltaker(
				id = it.id,
				fornavn = it.fornavn,
				mellomnavn = it.mellomnavn,
				etternavn = it.etternavn,
				fodselsnummer = it.personident,
				soktInnDato = it.innsoktDato.atStartOfDay(),
				startDato = it.startdato,
				sluttDato = it.sluttdato,
				status = DeltakerStatus(
					type = it.status,
					endretDato = it.statusGyldigFraDato
				),
				veiledere = getVeiledereForDeltaker(it.id, veiledere),
				navKontor = it.navKontor,
				aktiveEndringsmeldinger = getEndringsmeldinger(it.id, endringsmeldinger),
				gjeldendeVurderingFraArrangor = it.getGjeldendeVurdering()
			)
		}
	}

	private fun getVeiledereForDeltaker(deltakerId: UUID, veiledereDeltakere: List<Veileder>): List<Veileder> {
		return veiledereDeltakere.filter { it.deltakerId == deltakerId }
	}

	private fun getEndringsmeldinger(deltakerId: UUID, endringsmeldinger: List<EndringsmeldingDbo>): List<Endringsmelding> {
		val endringsmeldingerForDeltaker = endringsmeldinger.filter { it.deltakerId == deltakerId }
		return endringsmeldingerForDeltaker.map { it.toEndringsmelding() }
	}
}

fun List<DeltakerlisteDbo>.toDeltakerliste(): List<KoordinatorFor.Deltakerliste> {
	return this.map {
		KoordinatorFor.Deltakerliste(
			id = it.id,
			navn = it.navn,
			type = it.cleanTiltaksnavn(),
			startdato = it.startDato,
			sluttdato = it.sluttDato,
			erKurs = it.erKurs
		)
	}
}
