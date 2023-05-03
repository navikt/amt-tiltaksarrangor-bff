package no.nav.tiltaksarrangor.ingest.repositories.model

import no.nav.tiltaksarrangor.ingest.model.EndringsmeldingType
import no.nav.tiltaksarrangor.ingest.model.Innhold
import java.util.UUID

data class EndringsmeldingDbo(
	val id: UUID,
	val deltakerId: UUID,
	val type: EndringsmeldingType,
	val innhold: Innhold?
)
