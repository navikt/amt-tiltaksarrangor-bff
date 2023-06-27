package no.nav.tiltaksarrangor.repositories.model

import no.nav.tiltaksarrangor.ingest.model.AnsattRolle
import no.nav.tiltaksarrangor.model.Veileder
import no.nav.tiltaksarrangor.model.Veiledertype
import java.util.UUID

data class AnsattDbo(
	val id: UUID,
	val personIdent: String,
	val fornavn: String,
	val mellomnavn: String?,
	val etternavn: String,
	val roller: List<AnsattRolleDbo>,
	val deltakerlister: List<KoordinatorDeltakerlisteDbo>,
	val veilederDeltakere: List<VeilederDeltakerDbo>
)

data class AnsattPersonaliaDbo(
	val id: UUID,
	val personIdent: String,
	val fornavn: String,
	val mellomnavn: String?,
	val etternavn: String
)

data class AnsattRolleDbo(
	val arrangorId: UUID,
	val rolle: AnsattRolle
)

data class KoordinatorDeltakerlisteDbo(
	val deltakerlisteId: UUID
)

data class VeilederDeltakerDbo(
	val deltakerId: UUID,
	val veilederType: Veiledertype
)

data class AnsattVeilederDbo(
	val ansattPersonaliaDbo: AnsattPersonaliaDbo,
	val veilederType: Veiledertype
) {
	fun toVeileder(deltakerId: UUID): Veileder {
		return Veileder(
			ansattId = ansattPersonaliaDbo.id,
			deltakerId = deltakerId,
			veiledertype = veilederType,
			fornavn = ansattPersonaliaDbo.fornavn,
			mellomnavn = ansattPersonaliaDbo.mellomnavn,
			etternavn = ansattPersonaliaDbo.etternavn
		)
	}
}
