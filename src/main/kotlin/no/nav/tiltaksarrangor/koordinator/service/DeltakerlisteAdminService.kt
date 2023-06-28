package no.nav.tiltaksarrangor.koordinator.service

import no.nav.tiltaksarrangor.client.amttiltak.AmtTiltakClient
import no.nav.tiltaksarrangor.koordinator.model.AdminDeltakerliste
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class DeltakerlisteAdminService(
	private val amtTiltakClient: AmtTiltakClient
) {
	fun getAlleDeltakerlister(): List<AdminDeltakerliste> {
		val deltakerlisterLagtTil = amtTiltakClient.getDeltakerlisterLagtTil()
		val tilgjengeligeDeltakerlister = amtTiltakClient.getTilgjengeligeDeltakerlister()

		return tilgjengeligeDeltakerlister.map {
			AdminDeltakerliste(
				id = it.id,
				navn = it.navn,
				tiltaksnavn = it.tiltak.tiltaksnavn,
				arrangorNavn = if (it.arrangor.organisasjonNavn.isNullOrEmpty()) it.arrangor.virksomhetNavn else it.arrangor.organisasjonNavn,
				arrangorOrgnummer = it.arrangor.virksomhetOrgnr,
				arrangorParentNavn = it.arrangor.virksomhetNavn,
				startDato = it.startDato,
				sluttDato = it.sluttDato,
				lagtTil = deltakerlisterLagtTil.find { gjennomforingDto -> gjennomforingDto.id == it.id } != null
			)
		}
	}

	fun leggTilDeltakerliste(deltakerlisteId: UUID) {
		amtTiltakClient.opprettTilgangTilGjennomforing(deltakerlisteId)
	}

	fun fjernDeltakerliste(deltakerlisteId: UUID) {
		amtTiltakClient.fjernTilgangTilGjennomforing(deltakerlisteId)
	}
}
