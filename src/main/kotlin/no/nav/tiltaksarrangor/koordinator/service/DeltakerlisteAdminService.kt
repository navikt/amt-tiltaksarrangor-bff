package no.nav.tiltaksarrangor.koordinator.service

import no.nav.tiltaksarrangor.client.amttiltak.AmtTiltakClient
import no.nav.tiltaksarrangor.ingest.model.AnsattRolle
import no.nav.tiltaksarrangor.koordinator.model.AdminDeltakerliste
import no.nav.tiltaksarrangor.model.exceptions.UnauthorizedException
import no.nav.tiltaksarrangor.repositories.ArrangorRepository
import no.nav.tiltaksarrangor.repositories.DeltakerlisteRepository
import no.nav.tiltaksarrangor.repositories.model.ArrangorDbo
import no.nav.tiltaksarrangor.service.AnsattService
import no.nav.tiltaksarrangor.utils.erPilot
import no.nav.tiltaksarrangor.utils.isDev
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class DeltakerlisteAdminService(
	private val amtTiltakClient: AmtTiltakClient,
	private val ansattService: AnsattService,
	private val deltakerlisteRepository: DeltakerlisteRepository,
	private val arrangorRepository: ArrangorRepository
) {
	fun getAlleDeltakerlister(personIdent: String): List<AdminDeltakerliste> {
		val ansatt = ansattService.getAnsatt(personIdent) ?: throw UnauthorizedException("Ansatt finnes ikke")
		if (!ansattService.erKoordinator(ansatt.roller)) {
			throw UnauthorizedException("Ansatt ${ansatt.id} er ikke koordinator hos noen arrangører")
		}

		val koordinatorHosArrangorer = ansatt.roller.filter { it.rolle == AnsattRolle.KOORDINATOR }.map { it.arrangorId }
		val alleDeltakerlister = deltakerlisteRepository.getDeltakerlisterMedArrangor(koordinatorHosArrangorer)
			.filter { !it.deltakerlisteDbo.erKurs || isDev() || erPilot(it.deltakerlisteDbo.id) }

		val unikeOverordnedeArrangorIder = alleDeltakerlister.mapNotNull { it.arrangorDbo.overordnetArrangorId }.distinct()
		val overordnedeArrangorer = arrangorRepository.getArrangorer(unikeOverordnedeArrangorIder)

		return alleDeltakerlister.map {
			AdminDeltakerliste(
				id = it.deltakerlisteDbo.id,
				navn = it.deltakerlisteDbo.navn,
				tiltaksnavn = it.deltakerlisteDbo.tiltakNavn,
				arrangorNavn = it.arrangorDbo.navn,
				arrangorOrgnummer = it.arrangorDbo.organisasjonsnummer,
				arrangorParentNavn = it.arrangorDbo.overordnetArrangorId?.let { overordnetArrangorId ->
					finnOverordnetArrangorNavn(overordnetArrangorId, overordnedeArrangorer)
				} ?: it.arrangorDbo.navn,
				startDato = it.deltakerlisteDbo.startDato,
				sluttDato = it.deltakerlisteDbo.sluttDato,
				lagtTil = ansatt.deltakerlister.find { koordinatorDeltakerliste -> koordinatorDeltakerliste.deltakerlisteId == it.deltakerlisteDbo.id } != null
			)
		}
	}

	fun leggTilDeltakerliste(deltakerlisteId: UUID) {
		amtTiltakClient.opprettTilgangTilGjennomforing(deltakerlisteId)
	}

	fun fjernDeltakerliste(deltakerlisteId: UUID) {
		amtTiltakClient.fjernTilgangTilGjennomforing(deltakerlisteId)
	}

	private fun finnOverordnetArrangorNavn(overordnetArrangorId: UUID, overordnedeArrangorer: List<ArrangorDbo>): String? {
		return overordnedeArrangorer.find { it.id == overordnetArrangorId }?.navn
	}
}
