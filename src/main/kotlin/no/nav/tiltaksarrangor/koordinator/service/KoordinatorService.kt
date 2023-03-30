package no.nav.tiltaksarrangor.koordinator.service

import no.nav.tiltaksarrangor.client.AmtTiltakClient
import no.nav.tiltaksarrangor.client.dto.DeltakerlisteDto
import no.nav.tiltaksarrangor.client.dto.DeltakeroversiktDto
import no.nav.tiltaksarrangor.client.dto.TilgjengeligVeilederDto
import no.nav.tiltaksarrangor.koordinator.model.KoordinatorFor
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
