package no.nav.tiltaksarrangor.koordinator.service

import no.nav.tiltaksarrangor.client.amttiltak.AmtTiltakClient
import no.nav.tiltaksarrangor.client.amttiltak.dto.DeltakerDto
import no.nav.tiltaksarrangor.client.amttiltak.dto.TilgjengeligVeilederDto
import no.nav.tiltaksarrangor.client.amttiltak.dto.toEndringsmelding
import no.nav.tiltaksarrangor.client.amttiltak.dto.toStatus
import no.nav.tiltaksarrangor.client.amttiltak.dto.toVeileder
import no.nav.tiltaksarrangor.koordinator.model.Deltaker
import no.nav.tiltaksarrangor.koordinator.model.Deltakerliste
import no.nav.tiltaksarrangor.koordinator.model.KoordinatorFor
import no.nav.tiltaksarrangor.koordinator.model.LeggTilVeiledereRequest
import no.nav.tiltaksarrangor.koordinator.model.MineDeltakerlister
import no.nav.tiltaksarrangor.koordinator.model.TilgjengeligVeileder
import no.nav.tiltaksarrangor.koordinator.model.VeilederFor
import no.nav.tiltaksarrangor.model.StatusType
import no.nav.tiltaksarrangor.model.Veiledertype
import no.nav.tiltaksarrangor.model.exceptions.UnauthorizedException
import no.nav.tiltaksarrangor.repositories.DeltakerlisteRepository
import no.nav.tiltaksarrangor.repositories.model.DeltakerlisteDbo
import no.nav.tiltaksarrangor.repositories.model.KoordinatorDeltakerlisteDbo
import no.nav.tiltaksarrangor.repositories.model.VeilederDeltakerDbo
import no.nav.tiltaksarrangor.service.AnsattService
import no.nav.tiltaksarrangor.utils.erPilot
import no.nav.tiltaksarrangor.utils.isDev
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class KoordinatorService(
	private val amtTiltakClient: AmtTiltakClient,
	private val ansattService: AnsattService,
	private val deltakerlisteRepository: DeltakerlisteRepository
) {
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

	fun getTilgjengeligeVeiledere(deltakerlisteId: UUID): List<TilgjengeligVeileder> {
		return amtTiltakClient.getTilgjengeligeVeiledere(deltakerlisteId).map { it.toTilgjengeligVeileder() }
	}

	fun tildelVeiledereForDeltaker(deltakerId: UUID, request: LeggTilVeiledereRequest) {
		amtTiltakClient.tildelVeiledereForDeltaker(deltakerId, request)
	}

	fun getDeltakerliste(deltakerlisteId: UUID): Deltakerliste {
		val gjennomforing = amtTiltakClient.getGjennomforing(deltakerlisteId)
		val deltakere = amtTiltakClient.getDeltakerePaGjennomforing(deltakerlisteId)
		val koordinatorer = amtTiltakClient.getKoordinatorer(deltakerlisteId)

		return Deltakerliste(
			id = gjennomforing.id,
			navn = gjennomforing.navn,
			tiltaksnavn = gjennomforing.tiltak.tiltaksnavn,
			arrangorNavn = if (gjennomforing.arrangor.organisasjonNavn.isNullOrEmpty()) gjennomforing.arrangor.virksomhetNavn else gjennomforing.arrangor.organisasjonNavn,
			startDato = gjennomforing.startDato,
			sluttDato = gjennomforing.sluttDato,
			status = Deltakerliste.Status.valueOf(gjennomforing.status.name),
			koordinatorer = koordinatorer,
			deltakere = deltakere
				.map { it.toDeltaker(gjennomforing.erKurs) }
				.filter { !gjennomforing.erKurs || it.status.type != StatusType.IKKE_AKTUELL },
			erKurs = gjennomforing.erKurs
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
		return KoordinatorFor(deltakerlister = deltakerlister.filter { !it.erKurs || isDev() || erPilot(it.id) })
	}
}

fun List<DeltakerlisteDbo>.toDeltakerliste(): List<KoordinatorFor.Deltakerliste> {
	return this.map {
		KoordinatorFor.Deltakerliste(
			id = it.id,
			navn = it.navn,
			type = it.tiltakNavn,
			startdato = it.startDato,
			sluttdato = it.sluttDato,
			erKurs = it.erKurs
		)
	}
}

fun TilgjengeligVeilederDto.toTilgjengeligVeileder(): TilgjengeligVeileder {
	return TilgjengeligVeileder(
		ansattId = ansattId,
		fornavn = fornavn,
		mellomnavn = mellomnavn,
		etternavn = etternavn
	)
}

fun DeltakerDto.toDeltaker(erKurs: Boolean): Deltaker {
	return Deltaker(
		id = id,
		fornavn = fornavn,
		mellomnavn = mellomnavn,
		etternavn = etternavn,
		fodselsnummer = fodselsnummer,
		soktInnDato = registrertDato,
		startDato = startDato,
		sluttDato = sluttDato,
		status = status.toStatus(erKurs),
		veiledere = aktiveVeiledere.map { it.toVeileder() },
		navKontor = navKontor,
		aktiveEndringsmeldinger = aktiveEndringsmeldinger.map { it.toEndringsmelding() }
	)
}
