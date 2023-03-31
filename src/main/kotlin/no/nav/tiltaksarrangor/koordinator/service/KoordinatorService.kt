package no.nav.tiltaksarrangor.koordinator.service

import no.nav.tiltaksarrangor.client.AmtTiltakClient
import no.nav.tiltaksarrangor.client.dto.DeltakerDto
import no.nav.tiltaksarrangor.client.dto.DeltakerlisteDto
import no.nav.tiltaksarrangor.client.dto.DeltakeroversiktDto
import no.nav.tiltaksarrangor.client.dto.TilgjengeligVeilederDto
import no.nav.tiltaksarrangor.client.dto.toEndringsmelding
import no.nav.tiltaksarrangor.client.dto.toVeileder
import no.nav.tiltaksarrangor.koordinator.model.Deltaker
import no.nav.tiltaksarrangor.koordinator.model.Deltakerliste
import no.nav.tiltaksarrangor.koordinator.model.KoordinatorFor
import no.nav.tiltaksarrangor.koordinator.model.LeggTilVeiledereRequest
import no.nav.tiltaksarrangor.koordinator.model.MineDeltakerlister
import no.nav.tiltaksarrangor.koordinator.model.TilgjengeligVeileder
import no.nav.tiltaksarrangor.koordinator.model.VeilederFor
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
			deltakere = deltakere.map { it.toDeltaker() }
		)
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

fun DeltakerlisteDto.toDeltakerliste(): KoordinatorFor.Deltakerliste {
	return KoordinatorFor.Deltakerliste(
		id = id,
		type = type,
		navn = navn
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

fun DeltakerDto.toDeltaker(): Deltaker {
	return Deltaker(
		id = id,
		fornavn = fornavn,
		mellomnavn = mellomnavn,
		etternavn = etternavn,
		fodselsnummer = fodselsnummer,
		soktInnDato = registrertDato,
		startDato = startDato,
		sluttDato = sluttDato,
		status = status,
		veiledere = aktiveVeiledere.map { it.toVeileder() },
		aktiveEndringsmeldinger = aktiveEndringsmeldinger.map { it.toEndringsmelding() }
	)
}
