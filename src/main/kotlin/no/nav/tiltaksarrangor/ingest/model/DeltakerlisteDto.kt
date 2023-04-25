package no.nav.tiltaksarrangor.ingest.model

import no.nav.tiltaksarrangor.ingest.repositories.model.DeltakerlisteDbo
import java.time.LocalDate
import java.util.UUID

data class DeltakerlisteDto(
	val id: UUID,
	val navn: String,
	val status: DeltakerlisteStatus,
	val arrangor: DeltakerlisteArrangorDto,
	val tiltak: TiltakDto,
	val startDato: LocalDate?,
	val sluttDato: LocalDate?
)

fun DeltakerlisteDto.toDeltakerlisteDbo(): DeltakerlisteDbo {
	return DeltakerlisteDbo(
		id = id,
		navn = navn,
		status = status,
		arrangorId = arrangor.id,
		tiltakNavn = tiltak.navn,
		tiltakType = tiltak.type,
		startDato = startDato,
		sluttDato = sluttDato
	)
}
