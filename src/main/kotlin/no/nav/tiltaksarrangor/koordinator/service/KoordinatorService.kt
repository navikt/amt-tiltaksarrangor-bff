package no.nav.tiltaksarrangor.koordinator.service

import no.nav.tiltaksarrangor.client.AmtTiltakClient
import no.nav.tiltaksarrangor.client.dto.DeltakerlisteDto
import no.nav.tiltaksarrangor.client.dto.DeltakeroversiktDto
import no.nav.tiltaksarrangor.koordinator.model.KoordinatorFor
import no.nav.tiltaksarrangor.koordinator.model.MineDeltakerlister
import no.nav.tiltaksarrangor.koordinator.model.VeilederFor
import org.springframework.stereotype.Component

@Component
class KoordinatorService(
	private val amtTiltakClient: AmtTiltakClient
) {
	fun getMineDeltakerlister(): MineDeltakerlister {
		return amtTiltakClient.getMineDeltakerlister().toMineDeltakerlister()
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
