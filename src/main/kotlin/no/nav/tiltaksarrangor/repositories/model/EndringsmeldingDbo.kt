package no.nav.tiltaksarrangor.repositories.model

import no.nav.tiltaksarrangor.ingest.model.EndringsmeldingType
import no.nav.tiltaksarrangor.ingest.model.Innhold
import no.nav.tiltaksarrangor.ingest.model.toEndringsmeldingInnhold
import no.nav.tiltaksarrangor.model.Endringsmelding
import java.time.LocalDateTime
import java.util.UUID

data class EndringsmeldingDbo(
	val id: UUID,
	val deltakerId: UUID,
	val type: EndringsmeldingType,
	val innhold: Innhold?,
	val status: Endringsmelding.Status,
	val sendt: LocalDateTime
) {
	fun toEndringsmelding(): Endringsmelding {
		return Endringsmelding(
			id = id,
			innhold = innhold?.toEndringsmeldingInnhold(),
			type = Endringsmelding.Type.valueOf(type.name),
			status = status,
			sendt = sendt.toLocalDate()
		)
	}

	fun erAktiv(): Boolean {
		return status == Endringsmelding.Status.AKTIV
	}
}
