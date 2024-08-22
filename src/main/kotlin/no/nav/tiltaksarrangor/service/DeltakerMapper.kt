package no.nav.tiltaksarrangor.service

import no.nav.tiltaksarrangor.melding.forslag.AktivtForslagResponse
import no.nav.tiltaksarrangor.melding.forslag.ForslagService
import no.nav.tiltaksarrangor.melding.forslag.tilAktivtForslagResponse
import no.nav.tiltaksarrangor.model.Deltaker
import no.nav.tiltaksarrangor.model.DeltakerStatus
import no.nav.tiltaksarrangor.model.NavInformasjon
import no.nav.tiltaksarrangor.model.NavVeileder
import no.nav.tiltaksarrangor.model.Veileder
import no.nav.tiltaksarrangor.model.Vurdering
import no.nav.tiltaksarrangor.repositories.EndringsmeldingRepository
import no.nav.tiltaksarrangor.repositories.model.AnsattDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerlisteDbo
import no.nav.tiltaksarrangor.repositories.model.EndringsmeldingDbo
import org.springframework.stereotype.Service

@Service
class DeltakerMapper(
	private val ansattService: AnsattService,
	private val forslagService: ForslagService,
	private val endringsmeldingRepository: EndringsmeldingRepository,
) {
	fun map(
		deltaker: DeltakerDbo,
		deltakerliste: DeltakerlisteDbo,
		ansatt: AnsattDbo,
	): Deltaker {
		val ansattErVeileder = ansattService.erVeilederForDeltaker(
			deltakerId = deltaker.id,
			deltakerlisteArrangorId = deltakerliste.arrangorId,
			ansattDbo = ansatt,
		)

		val aktiveForslag = forslagService.getAktiveForslag(deltaker.id).map { it.tilAktivtForslagResponse() }

		val endringsmeldinger = if (deltaker.adressebeskyttet && !ansattErVeileder) {
			emptyList()
		} else {
			endringsmeldingRepository.getEndringsmeldingerForDeltaker(deltaker.id)
		}
		val veiledere = ansattService.getVeiledereForDeltaker(deltaker.id)

		return tilDeltaker(
			deltaker,
			deltakerliste,
			veiledere,
			endringsmeldinger,
			aktiveForslag,
			ansattErVeileder,
		)
	}
}

private fun tilDeltaker(
	deltakerDbo: DeltakerDbo,
	deltakerliste: DeltakerlisteDbo,
	veiledere: List<Veileder>,
	endringsmeldinger: List<EndringsmeldingDbo>,
	aktiveForslag: List<AktivtForslagResponse>,
	ansattErVeileder: Boolean,
): Deltaker {
	val adressebeskyttet = deltakerDbo.adressebeskyttet
	val deltaker = Deltaker(
		id = deltakerDbo.id,
		deltakerliste =
			Deltaker.Deltakerliste(
				id = deltakerliste.id,
				startDato = deltakerliste.startDato,
				sluttDato = deltakerliste.sluttDato,
				erKurs = deltakerliste.erKurs,
				tiltakstype = deltakerliste.tiltakType,
			),
		fornavn = deltakerDbo.fornavn,
		mellomnavn = deltakerDbo.mellomnavn,
		etternavn = deltakerDbo.etternavn,
		fodselsnummer = deltakerDbo.personident,
		telefonnummer = deltakerDbo.telefonnummer,
		epost = deltakerDbo.epost,
		status =
			DeltakerStatus(
				type = deltakerDbo.status,
				endretDato = deltakerDbo.statusOpprettetDato,
				aarsak = deltakerDbo.statusAarsak,
			),
		startDato = deltakerDbo.startdato,
		sluttDato = deltakerDbo.sluttdato,
		deltakelseProsent = deltakerDbo.prosentStilling?.toInt(),
		dagerPerUke = deltakerDbo.dagerPerUke,
		soktInnPa = deltakerliste.navn,
		soktInnDato = deltakerDbo.innsoktDato.atStartOfDay(),
		tiltakskode = deltakerliste.tiltakType,
		bestillingTekst = deltakerDbo.bestillingstekst,
		innhold = deltakerDbo.innhold,
		fjernesDato = deltakerDbo.skalFjernesDato(),
		navInformasjon =
			NavInformasjon(
				navkontor = deltakerDbo.navKontor,
				navVeileder =
					deltakerDbo.navVeilederId?.let {
						NavVeileder(
							navn = deltakerDbo.navVeilederNavn ?: "",
							epost = deltakerDbo.navVeilederEpost,
							telefon = deltakerDbo.navVeilederTelefon,
						)
					},
			),
		veiledere = veiledere,
		aktiveForslag = aktiveForslag,
		aktiveEndringsmeldinger = endringsmeldinger.filter { it.erAktiv() }.sortedBy { it.sendt }.map { it.toEndringsmelding() },
		historiskeEndringsmeldinger = endringsmeldinger
			.filter { !it.erAktiv() }
			.sortedByDescending { it.sendt }
			.map { it.toEndringsmelding() },
		adresse = if (adressebeskyttet) null else deltakerDbo.getAdresse(deltakerliste),
		gjeldendeVurderingFraArrangor = deltakerDbo.getGjeldendeVurdering(),
		historiskeVurderingerFraArrangor = deltakerDbo.getHistoriskeVurderinger(),
		adressebeskyttet = adressebeskyttet,
	)

	return if (adressebeskyttet && !ansattErVeileder) {
		deltaker.utenPersonligInformasjon()
	} else {
		deltaker
	}
}

fun Deltaker.utenPersonligInformasjon() = this.copy(
	fornavn = "",
	mellomnavn = null,
	etternavn = "",
	fodselsnummer = "",
	telefonnummer = null,
	epost = null,
	deltakelseProsent = null,
	dagerPerUke = null,
	bestillingTekst = null,
	navInformasjon = NavInformasjon(
		navkontor = null,
		navVeileder = null,
	),
	aktiveForslag = emptyList(),
	aktiveEndringsmeldinger = emptyList(),
	historiskeEndringsmeldinger = emptyList(),
	gjeldendeVurderingFraArrangor = null,
	historiskeVurderingerFraArrangor = null,
)

fun DeltakerDbo.getAdresse(deltakerliste: DeltakerlisteDbo) = if (deltakerliste.skalViseAdresseForDeltaker()) {
	this.adresse?.kontaktadresse?.toAdresse()
		?: this.adresse?.oppholdsadresse?.toAdresse()
		?: this.adresse?.bostedsadresse?.toAdresse()
} else {
	null
}

fun DeltakerDbo.getGjeldendeVurdering(): Vurdering? = vurderingerFraArrangor?.firstOrNull { it.gyldigTil == null }?.toVurdering()

fun DeltakerDbo.getHistoriskeVurderinger(): List<Vurdering>? = vurderingerFraArrangor
	?.filter {
		it.gyldigTil != null
	}?.map { it.toVurdering() }
