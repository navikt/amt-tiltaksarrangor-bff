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
	val sluttDato: LocalDate?,
	val erKurs: Boolean
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
		sluttDato = sluttDato,
		erKurs = erKurs
	)
}

fun DeltakerlisteDto.skalLagres(): Boolean {
	if (status == DeltakerlisteStatus.GJENNOMFORES) {
		return true
	} else if (status == DeltakerlisteStatus.AVSLUTTET && sluttDato != null && LocalDate.now()
		.isBefore(sluttDato.plusDays(15))
	) {
		return true
	}
	return false
}
