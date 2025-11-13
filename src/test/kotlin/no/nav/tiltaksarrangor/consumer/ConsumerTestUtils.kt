package no.nav.tiltaksarrangor.consumer

import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode
import no.nav.tiltaksarrangor.client.amtarrangor.dto.ArrangorMedOverordnetArrangor
import no.nav.tiltaksarrangor.consumer.model.DeltakerlistePayload
import no.nav.tiltaksarrangor.consumer.model.Oppstartstype
import no.nav.tiltaksarrangor.consumer.model.TiltakstypePayload
import java.time.LocalDate
import java.util.UUID

object ConsumerTestUtils {
	// val test = lagArrangor("Test")

	val arrangorInTest =
		ArrangorMedOverordnetArrangor(
			id = UUID.randomUUID(),
			navn = "Arrangør AS",
			organisasjonsnummer = "987654321",
			overordnetArrangor = null,
		)

	val tiltakstypePayloadInTest = TiltakstypePayload(
		id = UUID.randomUUID(),
		navn = "Navn",
		tiltakskode = Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING.name,
	)

	val deltakerlisteIdInTest: UUID = UUID.randomUUID()

	val deltakerlistePayloadInTest =
		DeltakerlistePayload(
			id = deltakerlisteIdInTest,
			tiltakskode = tiltakstypePayloadInTest.tiltakskode,
			tiltakstype =
				DeltakerlistePayload.Tiltakstype(
					tiltakskode = tiltakstypePayloadInTest.tiltakskode,
				),
			navn = "Gjennomføring av tiltak",
			startDato = LocalDate.now().minusYears(2),
			sluttDato = null,
			status = DeltakerlistePayload.Status.GJENNOMFORES,
			oppstart = Oppstartstype.LOPENDE,
			tilgjengeligForArrangorFraOgMedDato = LocalDate.now(),
			arrangor = DeltakerlistePayload.Arrangor(arrangorInTest.organisasjonsnummer),
		)
}
