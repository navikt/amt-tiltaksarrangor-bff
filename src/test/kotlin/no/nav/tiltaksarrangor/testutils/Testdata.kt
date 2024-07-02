package no.nav.tiltaksarrangor.testutils

import no.nav.tiltaksarrangor.ingest.model.AdresseDto
import no.nav.tiltaksarrangor.ingest.model.AnsattRolle
import no.nav.tiltaksarrangor.ingest.model.Bostedsadresse
import no.nav.tiltaksarrangor.ingest.model.EndringsmeldingType
import no.nav.tiltaksarrangor.ingest.model.Innhold
import no.nav.tiltaksarrangor.ingest.model.Kontaktadresse
import no.nav.tiltaksarrangor.ingest.model.Matrikkeladresse
import no.nav.tiltaksarrangor.ingest.model.NavAnsatt
import no.nav.tiltaksarrangor.ingest.model.NavEnhet
import no.nav.tiltaksarrangor.ingest.model.Vegadresse
import no.nav.tiltaksarrangor.ingest.model.VurderingDto
import no.nav.tiltaksarrangor.model.DeltakerlisteStatus
import no.nav.tiltaksarrangor.model.Endringsmelding
import no.nav.tiltaksarrangor.model.StatusType
import no.nav.tiltaksarrangor.model.Vurderingstype
import no.nav.tiltaksarrangor.repositories.model.AnsattDbo
import no.nav.tiltaksarrangor.repositories.model.AnsattRolleDbo
import no.nav.tiltaksarrangor.repositories.model.ArrangorDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerlisteDbo
import no.nav.tiltaksarrangor.repositories.model.EndringsmeldingDbo
import no.nav.tiltaksarrangor.repositories.model.KoordinatorDeltakerlisteDbo
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
		startDato = LocalDate.now().minusDays(1),
		sluttDato = null,
		erKurs = false,
		tilgjengeligForArrangorFraOgMedDato = null,
	)
}

fun getDeltaker(
	deltakerId: UUID,
	deltakerlisteId: UUID = UUID.randomUUID(),
	adressebeskyttet: Boolean = false,
): DeltakerDbo {
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
		skjultDato = null,
		adressebeskyttet = adressebeskyttet,
	)
}

fun getEndringsmelding(deltakerId: UUID): EndringsmeldingDbo {
	return EndringsmeldingDbo(
		id = UUID.randomUUID(),
		deltakerId = deltakerId,
		type = EndringsmeldingType.FORLENG_DELTAKELSE,
		innhold = Innhold.ForlengDeltakelseInnhold(LocalDate.now().plusMonths(2)),
		status = Endringsmelding.Status.AKTIV,
		sendt = LocalDateTime.now(),
	)
}

fun getAdresse(): AdresseDto = AdresseDto(
	bostedsadresse =
		Bostedsadresse(
			coAdressenavn = "C/O Gutterommet",
			vegadresse = null,
			matrikkeladresse =
				Matrikkeladresse(
					tilleggsnavn = "Gården",
					postnummer = "0484",
					poststed = "OSLO",
				),
		),
	oppholdsadresse = null,
	kontaktadresse =
		Kontaktadresse(
			coAdressenavn = null,
			vegadresse =
				Vegadresse(
					husnummer = "1",
					husbokstav = null,
					adressenavn = "Gate",
					tilleggsnavn = null,
					postnummer = "1234",
					poststed = "MOSS",
				),
			postboksadresse = null,
		),
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
			gyldigTil = null,
		),
	)
}

fun getArrangor(
	id: UUID = UUID.randomUUID(),
	navn: String = "Arrangor navn",
	organisasjonsnummer: String = (1000..90000000).random().toString(),
) = ArrangorDbo(
	id = id,
	navn = navn,
	organisasjonsnummer = organisasjonsnummer,
	overordnetArrangorId = null,
)

fun getKoordinator(
	id: UUID = UUID.randomUUID(),
	deltakerlisteId: UUID = UUID.randomUUID(),
	arrangorId: UUID = UUID.randomUUID(),
	personident: String = (10000..9000000).random().toString(),
	fornavn: String = "Fornavn",
	mellomnavn: String? = null,
	etternavn: String = "Etternavn",
) = AnsattDbo(
	id = id,
	personIdent = personident,
	fornavn = fornavn,
	mellomnavn = mellomnavn,
	etternavn = etternavn,
	roller = listOf(AnsattRolleDbo(arrangorId, AnsattRolle.KOORDINATOR)),
	deltakerlister = listOf(KoordinatorDeltakerlisteDbo(deltakerlisteId)),
	veilederDeltakere = emptyList(),
)

fun getNavAnsatt(id: UUID = UUID.randomUUID()) = NavAnsatt(
	id = id,
	navident = (100000..999999).random().toString(),
	navn = "Veileder Veiledersen",
	epost = "epost@nav.no",
	telefon = "99999999",
)

fun getNavEnhet(id: UUID = UUID.randomUUID()) = NavEnhet(
	id = id,
	enhetId = (100000..999999).random().toString(),
	navn = "NAV Grünerløkka",
)
