package no.nav.tiltaksarrangor.testutils

import no.nav.amt.lib.models.deltaker.Deltakelsesinnhold
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
import no.nav.tiltaksarrangor.model.Kilde
import no.nav.tiltaksarrangor.model.StatusType
import no.nav.tiltaksarrangor.model.Veiledertype
import no.nav.tiltaksarrangor.model.Vurderingstype
import no.nav.tiltaksarrangor.repositories.model.AnsattDbo
import no.nav.tiltaksarrangor.repositories.model.AnsattRolleDbo
import no.nav.tiltaksarrangor.repositories.model.ArrangorDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerlisteDbo
import no.nav.tiltaksarrangor.repositories.model.EndringsmeldingDbo
import no.nav.tiltaksarrangor.repositories.model.KoordinatorDeltakerlisteDbo
import no.nav.tiltaksarrangor.repositories.model.VeilederDeltakerDbo
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

fun getDeltakerliste(arrangorId: UUID) = getDeltakerliste(id = UUID.randomUUID(), arrangorId = arrangorId)

fun getDeltakerliste(id: UUID = UUID.randomUUID(), arrangorId: UUID): DeltakerlisteDbo = DeltakerlisteDbo(
	id = id,
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

fun getDeltaker(
	deltakerId: UUID,
	deltakerlisteId: UUID = UUID.randomUUID(),
	adressebeskyttet: Boolean = false,
	status: StatusType = StatusType.DELTAR,
): DeltakerDbo = DeltakerDbo(
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
	status = status,
	statusOpprettetDato = LocalDateTime.now(),
	statusGyldigFraDato = LocalDate.of(2023, 2, 1).atStartOfDay(),
	statusAarsak = null,
	dagerPerUke = null,
	prosentStilling = null,
	startdato = LocalDate.of(2023, 2, 15),
	sluttdato = null,
	innsoktDato = LocalDate.now(),
	bestillingstekst = "tekst",
	navKontor = "1234",
	navVeilederId = UUID.randomUUID(),
	navVeilederEpost = "epost@nav.no",
	navVeilederNavn = "Foo Bar",
	navVeilederTelefon = "1234",
	skjultAvAnsattId = null,
	skjultDato = null,
	adressebeskyttet = adressebeskyttet,
	innhold = Deltakelsesinnhold(
		ledetekst = "Innholdsledetekst...",
		innhold = listOf(
			no.nav.amt.lib.models.deltaker.Innhold(
				tekst = "tekst",
				innholdskode = "kode",
				valgt = true,
				beskrivelse = "beskrivelse",
			),
		),
	),
	kilde = Kilde.ARENA,
	historikk = emptyList(),
)

fun getEndringsmelding(deltakerId: UUID): EndringsmeldingDbo = EndringsmeldingDbo(
	id = UUID.randomUUID(),
	deltakerId = deltakerId,
	type = EndringsmeldingType.FORLENG_DELTAKELSE,
	innhold = Innhold.ForlengDeltakelseInnhold(LocalDate.now().plusMonths(2)),
	status = Endringsmelding.Status.AKTIV,
	sendt = LocalDateTime.now(),
)

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

fun getVurderinger(deltakerId: UUID, gyldigFra: LocalDateTime = LocalDateTime.now()): List<VurderingDto> = listOf(
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

fun getVeileder(
	id: UUID = UUID.randomUUID(),
	deltakerId: UUID = UUID.randomUUID(),
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
	roller = listOf(AnsattRolleDbo(arrangorId, AnsattRolle.VEILEDER)),
	deltakerlister = emptyList(),
	veilederDeltakere = listOf(VeilederDeltakerDbo(deltakerId, Veiledertype.VEILEDER)),
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
	enhetsnummer = (100000..999999).random().toString(),
	navn = "NAV Grünerløkka",
)
