package no.nav.tiltaksarrangor.koordinator.service

import no.nav.tiltaksarrangor.client.amttiltak.AmtTiltakClient
import no.nav.tiltaksarrangor.client.amttiltak.dto.DeltakerDto
import no.nav.tiltaksarrangor.client.amttiltak.dto.DeltakeroversiktDto
import no.nav.tiltaksarrangor.client.amttiltak.dto.KoordinatorInfoDto
import no.nav.tiltaksarrangor.client.amttiltak.dto.TilgjengeligVeilederDto
import no.nav.tiltaksarrangor.client.amttiltak.dto.toEndringsmelding
import no.nav.tiltaksarrangor.client.amttiltak.dto.toStatus
import no.nav.tiltaksarrangor.client.amttiltak.dto.toVeileder
import no.nav.tiltaksarrangor.koordinator.model.AdminDeltakerliste
import no.nav.tiltaksarrangor.koordinator.model.Deltaker
import no.nav.tiltaksarrangor.koordinator.model.Deltakerliste
import no.nav.tiltaksarrangor.koordinator.model.KoordinatorFor
import no.nav.tiltaksarrangor.koordinator.model.LeggTilVeiledereRequest
import no.nav.tiltaksarrangor.koordinator.model.MineDeltakerlister
import no.nav.tiltaksarrangor.koordinator.model.TilgjengeligVeileder
import no.nav.tiltaksarrangor.koordinator.model.VeilederFor
import no.nav.tiltaksarrangor.model.StatusType
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class KoordinatorService(
	private val amtTiltakClient: AmtTiltakClient
) {
	fun getMineDeltakerlister(): MineDeltakerlister {
		return amtTiltakClient.getMineDeltakerlister().toMineDeltakerlister()
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

fun DeltakeroversiktDto.toMineDeltakerlister(): MineDeltakerlister {
	return MineDeltakerlister(
		veilederFor = veilederInfo?.let {
			VeilederFor(
				veilederFor = it.veilederFor,
				medveilederFor = it.medveilederFor
			)
		},
		koordinatorFor = koordinatorInfo?.let {
			KoordinatorFor(
				deltakerlister = it.deltakerlister.map { deltakerlisteDto -> deltakerlisteDto.toDeltakerliste() }
			)
		}
	)
}

fun KoordinatorInfoDto.DeltakerlisteDto.toDeltakerliste(): KoordinatorFor.Deltakerliste {
	return KoordinatorFor.Deltakerliste(
		id = id,
		type = type,
		navn = navn,
		startdato = startdato,
		sluttdato = sluttdato,
		erKurs = erKurs
	)
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
