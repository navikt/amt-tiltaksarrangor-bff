package no.nav.tiltaksarrangor.koordinator.service

import no.nav.tiltaksarrangor.client.amttiltak.AmtTiltakClient
import no.nav.tiltaksarrangor.client.amttiltak.dto.DeltakerDto
import no.nav.tiltaksarrangor.client.amttiltak.dto.TilgjengeligVeilederDto
import no.nav.tiltaksarrangor.client.amttiltak.dto.toEndringsmelding
import no.nav.tiltaksarrangor.client.amttiltak.dto.toStatus
import no.nav.tiltaksarrangor.client.amttiltak.dto.toVeileder
import no.nav.tiltaksarrangor.erPilot
import no.nav.tiltaksarrangor.ingest.model.AnsattRolle
import no.nav.tiltaksarrangor.ingest.model.Veiledertype
import no.nav.tiltaksarrangor.isDev
import no.nav.tiltaksarrangor.koordinator.model.AdminDeltakerliste
import no.nav.tiltaksarrangor.koordinator.model.Deltaker
import no.nav.tiltaksarrangor.koordinator.model.Deltakerliste
import no.nav.tiltaksarrangor.koordinator.model.KoordinatorFor
import no.nav.tiltaksarrangor.koordinator.model.LeggTilVeiledereRequest
import no.nav.tiltaksarrangor.koordinator.model.MineDeltakerlister
import no.nav.tiltaksarrangor.koordinator.model.TilgjengeligVeileder
import no.nav.tiltaksarrangor.koordinator.model.VeilederFor
import no.nav.tiltaksarrangor.model.StatusType
import no.nav.tiltaksarrangor.model.exceptions.UnauthorizedException
import no.nav.tiltaksarrangor.repositories.DeltakerlisteRepository
import no.nav.tiltaksarrangor.repositories.model.DeltakerlisteDbo
import no.nav.tiltaksarrangor.service.AnsattService
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
		val unikeRoller = ansatt.roller.map { it.rolle.name }.distinct()
		if (unikeRoller.isEmpty()) {
			throw UnauthorizedException("Ansatt ${ansatt.id} er ikke veileder eller koordinator")
		}
		val veilederFor = unikeRoller.find { it == AnsattRolle.VEILEDER.name }
			?.let {
				VeilederFor(
					veilederFor = ansatt.veilederDeltakere.filter { it.veilederType == Veiledertype.VEILEDER }.size,
					medveilederFor = ansatt.veilederDeltakere.filter { it.veilederType == Veiledertype.MEDVEILEDER }.size
				)
			}
		val koordinatorFor = unikeRoller.find { it == AnsattRolle.KOORDINATOR.name }
			?.let {
				val deltakerlister = deltakerlisteRepository.getDeltakerlister(ansatt.deltakerlister.map { it.deltakerlisteId }).toDeltakerliste()
				KoordinatorFor(deltakerlister = deltakerlister.filter { !it.erKurs || isDev() || erPilot(it.id) })
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

	fun getAlleDeltakerlister(): List<AdminDeltakerliste> {
		val deltakerlisterLagtTil = amtTiltakClient.getDeltakerlisterLagtTil()
		val tilgjengeligeDeltakerlister = amtTiltakClient.getTilgjengeligeDeltakerlister()

		return tilgjengeligeDeltakerlister.map {
			AdminDeltakerliste(
				id = it.id,
				navn = it.navn,
				tiltaksnavn = it.tiltak.tiltaksnavn,
				arrangorNavn = if (it.arrangor.organisasjonNavn.isNullOrEmpty()) it.arrangor.virksomhetNavn else it.arrangor.organisasjonNavn,
				arrangorOrgnummer = it.arrangor.virksomhetOrgnr,
				arrangorParentNavn = it.arrangor.virksomhetNavn,
				startDato = it.startDato,
				sluttDato = it.sluttDato,
				lagtTil = deltakerlisterLagtTil.find { gjennomforingDto -> gjennomforingDto.id == it.id } != null
			)
		}
	}

	fun leggTilDeltakerliste(deltakerlisteId: UUID) {
		amtTiltakClient.opprettTilgangTilGjennomforing(deltakerlisteId)
	}

	fun fjernDeltakerliste(deltakerlisteId: UUID) {
		amtTiltakClient.fjernTilgangTilGjennomforing(deltakerlisteId)
	}
}

fun List<DeltakerlisteDbo>.toDeltakerliste(): List<KoordinatorFor.Deltakerliste> {
	return this.map {
		KoordinatorFor.Deltakerliste(
			id = it.id,
			navn = it.navn,
			type = it.tiltakType,
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
