package no.nav.tiltaksarrangor.testutils

import no.nav.tiltaksarrangor.ingest.model.DeltakerlisteStatus
import no.nav.tiltaksarrangor.model.StatusType
import no.nav.tiltaksarrangor.repositories.model.DeltakerDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerlisteDbo
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

fun getDeltakerliste(arrangorId: UUID): DeltakerlisteDbo {
	return DeltakerlisteDbo(
		id = UUID.randomUUID(),
		navn = "Gjennomføring 1",
		status = DeltakerlisteStatus.GJENNOMFORES,
		arrangorId = arrangorId,
		tiltakNavn = "Tiltaksnavnet",
		tiltakType = "ARBFORB",
		startDato = null,
		sluttDato = null,
		erKurs = false
	)
}

fun getDeltaker(deltakerId: UUID, deltakerlisteId: UUID = UUID.randomUUID()): DeltakerDbo {
	return DeltakerDbo(
		id = deltakerId,
		deltakerlisteId = deltakerlisteId,
		personident = UUID.randomUUID().toString(),
		fornavn = "Fornavn",
		mellomnavn = null,
		etternavn = "Etternavn",
		telefonnummer = null,
		epost = null,
		erSkjermet = false,
		status = StatusType.DELTAR,
		statusOpprettetDato = LocalDateTime.now(),
		statusGyldigFraDato = LocalDate.of(2023, 2, 1).atStartOfDay(),
		dagerPerUke = null,
		prosentStilling = null,
		startdato = LocalDate.of(2023, 2, 15),
		sluttdato = null,
		innsoktDato = LocalDate.now(),
		bestillingstekst = "tekst",
		navKontor = null,
		navVeilederId = null,
		navVeilederEpost = null,
		navVeilederNavn = null,
		navVeilederTelefon = null,
		skjultAvAnsattId = null,
		skjultDato = null
	)
}
