package no.nav.tiltaksarrangor.consumer

import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode
import no.nav.tiltaksarrangor.client.amtarrangor.dto.ArrangorMedOverordnetArrangor
import no.nav.tiltaksarrangor.consumer.model.DeltakerlistePayload
import no.nav.tiltaksarrangor.consumer.model.Oppstartstype
import no.nav.tiltaksarrangor.consumer.model.TiltakstypePayload
import java.time.LocalDate
import java.util.UUID

object ConsumerTestUtils {
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
			tiltakstype =
				DeltakerlistePayload.Tiltakstype(
					id = tiltakstypePayloadInTest.id,
					tiltakskode = tiltakstypePayloadInTest.tiltakskode,
				),
			navn = "Gjennomføring av tiltak",
			startDato = LocalDate.now().minusYears(2),
			sluttDato = null,
			status = DeltakerlistePayload.Status.GJENNOMFORES,
			virksomhetsnummer = arrangorInTest.organisasjonsnummer,
			oppstart = Oppstartstype.LOPENDE,
			tilgjengeligForArrangorFraOgMedDato = LocalDate.now(),
		)

	val deltakerlisteV2PayloadInTest = deltakerlistePayloadInTest.copy(
		virksomhetsnummer = null,
		arrangor = DeltakerlistePayload.Arrangor(arrangorInTest.organisasjonsnummer),
	)
}
