package no.nav.tiltaksarrangor.ingest.model

import no.nav.tiltaksarrangor.repositories.model.AnsattDbo
import no.nav.tiltaksarrangor.repositories.model.AnsattRolleDbo
import no.nav.tiltaksarrangor.repositories.model.KoordinatorDeltakerlisteDbo
import no.nav.tiltaksarrangor.repositories.model.VeilederDeltakerDbo
import java.util.UUID

data class AnsattDto(
	val id: UUID,
	val personalia: AnsattPersonaliaDto,
	val arrangorer: List<TilknyttetArrangorDto>,
)

fun AnsattDto.toAnsattDbo(): AnsattDbo {
	return AnsattDbo(
		id = id,
		personIdent = personalia.personident,
		fornavn = personalia.navn.fornavn,
		mellomnavn = personalia.navn.mellomnavn,
		etternavn = personalia.navn.etternavn,
		roller = arrangorer.flatMap { it.tilAnsattRolleDbo() },
		deltakerlister = arrangorer.flatMap { it.tilKoordinatorDeltakerlisteDbo() },
		veilederDeltakere = arrangorer.flatMap { it.tilVeilederDeltakerDbo() },
	)
}

fun TilknyttetArrangorDto.tilAnsattRolleDbo(): List<AnsattRolleDbo> {
	return roller.map { AnsattRolleDbo(arrangorId, it) }
}

fun TilknyttetArrangorDto.tilKoordinatorDeltakerlisteDbo(): List<KoordinatorDeltakerlisteDbo> {
	return koordinator.map { KoordinatorDeltakerlisteDbo(it) }
}

fun TilknyttetArrangorDto.tilVeilederDeltakerDbo(): List<VeilederDeltakerDbo> {
	return veileder.map { VeilederDeltakerDbo(deltakerId = it.deltakerId, veilederType = it.type) }
}
