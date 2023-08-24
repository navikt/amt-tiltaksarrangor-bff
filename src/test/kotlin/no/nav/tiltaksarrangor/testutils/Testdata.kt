package no.nav.tiltaksarrangor.testutils

import no.nav.tiltaksarrangor.ingest.model.AdresseDto
import no.nav.tiltaksarrangor.ingest.model.Bostedsadresse
import no.nav.tiltaksarrangor.ingest.model.DeltakerlisteStatus
import no.nav.tiltaksarrangor.ingest.model.EndringsmeldingType
import no.nav.tiltaksarrangor.ingest.model.Innhold
import no.nav.tiltaksarrangor.ingest.model.Kontaktadresse
import no.nav.tiltaksarrangor.ingest.model.Matrikkeladresse
import no.nav.tiltaksarrangor.ingest.model.Vegadresse
import no.nav.tiltaksarrangor.ingest.model.VurderingDto
import no.nav.tiltaksarrangor.model.StatusType
import no.nav.tiltaksarrangor.model.Vurderingstype
import no.nav.tiltaksarrangor.repositories.model.DeltakerDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerlisteDbo
import no.nav.tiltaksarrangor.repositories.model.EndringsmeldingDbo
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
		adresse = getAdresse(),
		vurderingerFraArrangor = getVurderinger(deltakerId),
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

fun getEndringsmelding(deltakerId: UUID): EndringsmeldingDbo {
	return EndringsmeldingDbo(
		id = UUID.randomUUID(),
		deltakerId = deltakerId,
		type = EndringsmeldingType.FORLENG_DELTAKELSE,
		innhold = Innhold.ForlengDeltakelseInnhold(LocalDate.now().plusMonths(2))
	)
}

fun getAdresse(): AdresseDto =
	AdresseDto(
		bostedsadresse = Bostedsadresse(
			coAdressenavn = "C/O Gutterommet",
			vegadresse = null,
			matrikkeladresse = Matrikkeladresse(
				tilleggsnavn = "Gården",
				postnummer = "0484",
				poststed = "OSLO"
			)
		),
		oppholdsadresse = null,
		kontaktadresse = Kontaktadresse(
			coAdressenavn = null,
			vegadresse = Vegadresse(
				husnummer = "1",
				husbokstav = null,
				adressenavn = "Gate",
				tilleggsnavn = null,
				postnummer = "1234",
				poststed = "MOSS"
			),
			postboksadresse = null
		)
	)

fun getVurderinger(deltakerId: UUID, gyldigFra: LocalDateTime = LocalDateTime.now()): List<VurderingDto> {
	return listOf(
		VurderingDto(
			id = UUID.randomUUID(),
			deltakerId = deltakerId,
			vurderingstype = Vurderingstype.OPPFYLLER_IKKE_KRAVENE,
			begrunnelse = "Mangler førerkort",
			opprettetAvArrangorAnsattId = UUID.randomUUID(),
			gyldigFra = gyldigFra,
			gyldigTil = null
		)
	)
}
