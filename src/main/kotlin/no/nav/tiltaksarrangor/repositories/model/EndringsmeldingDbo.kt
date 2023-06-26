package no.nav.tiltaksarrangor.repositories.model

import no.nav.tiltaksarrangor.ingest.model.EndringsmeldingType
import no.nav.tiltaksarrangor.ingest.model.Innhold
import no.nav.tiltaksarrangor.ingest.model.toEndringsmeldingInnhold
import no.nav.tiltaksarrangor.model.Endringsmelding
import java.util.UUID

data class EndringsmeldingDbo(
	val id: UUID,
	val deltakerId: UUID,
	val type: EndringsmeldingType,
	val innhold: Innhold?
) {
	fun toEndringsmelding(): Endringsmelding {
		return Endringsmelding(
			id = id,
			innhold = innhold?.toEndringsmeldingInnhold(),
			type = Endringsmelding.Type.valueOf(type.name)
		)
	}
}
