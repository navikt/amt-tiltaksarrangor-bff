package no.nav.tiltaksarrangor.consumer.model

import com.fasterxml.jackson.annotation.JsonInclude
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.json.schema.shouldMatchSchema
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode
import no.nav.amt.lib.utils.objectMapper
import no.nav.tiltaksarrangor.consumer.ConsumerTestUtils.deltakerlistePayloadInTest
import no.nav.tiltaksarrangor.consumer.ConsumerTestUtils.tiltakstypePayloadInTest
import no.nav.tiltaksarrangor.consumer.model.DeltakerlistePayloadJsonSchemas.deltakerlistePayloadV2Schema
import no.nav.tiltaksarrangor.testutils.getArrangor
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class DeltakerlistePayloadTest {
	@Nested
	inner class Tiltakskodenavn {
		@Test
		fun `kaster feil hvis tiltakskode mangler`() {
			val payload = deltakerlistePayloadInTest.copy(tiltakstype = null, tiltakskode = null)

			shouldThrow<IllegalStateException> {
				payload.effectiveTiltakskode
			}
		}

		@Test
		fun `returnerer tiltakskode fra Tiltakstype`() {
			val payload = deltakerlistePayloadInTest.copy(
				tiltakstype = DeltakerlistePayload.Tiltakstype(tiltakstypePayloadInTest.tiltakskode),
				tiltakskode = null,
			)

			payload.effectiveTiltakskode shouldBe tiltakstypePayloadInTest.tiltakskode
		}

		@Test
		fun `returnerer tiltakskode fra tiltakskode`() {
			val expectedTiltakskode = Tiltakskode.JOBBKLUBB.name

			val payload = deltakerlistePayloadInTest.copy(
				tiltakstype = null,
				tiltakskode = expectedTiltakskode,
			)

			payload.effectiveTiltakskode shouldBe expectedTiltakskode
		}
	}

	@Nested
	inner class ToModel {
		@Test
		fun `toModel - mapper felter korrekt`() {
			val arrangor = arrangorInTest
			val deltakerlisteDbo = deltakerlistePayloadInTest.toDeltakerlisteDbo(
				arrangorId = arrangor.id,
				navnTiltakstype = Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING.name,
			)

			assertSoftly(deltakerlisteDbo) {
				id shouldBe deltakerlistePayloadInTest.id
				navn shouldBe deltakerlistePayloadInTest.navn
				status shouldBe deltakerlistePayloadInTest.toDeltakerlisteStatus()
				arrangorId shouldBe arrangor.id
				tiltaksnavn shouldBe Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING.name
				tiltakskode shouldBe Tiltakskode.valueOf(deltakerlistePayloadInTest.tiltakstype!!.tiltakskode)
				startDato shouldBe deltakerlistePayloadInTest.startDato
				sluttDato shouldBe deltakerlistePayloadInTest.sluttDato
				erKurs shouldBe deltakerlistePayloadInTest.erKurs()
				oppstartstype shouldBe deltakerlistePayloadInTest.oppstart
				tilgjengeligForArrangorFraOgMedDato shouldBe deltakerlistePayloadInTest.tilgjengeligForArrangorFraOgMedDato
			}
		}
	}

	@Nested
	inner class Validate {
		@Test
		fun `fullt populert V2 skal matche skjema`() {
			val json = objectMapper
				.copy()
				.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
				.writeValueAsString(fullyPopulatedV2PayloadInTest.copy())

			json.shouldMatchSchema(deltakerlistePayloadV2Schema)
		}
	}

	companion object {
		private val arrangorInTest = getArrangor()
		private val arrangorDtoInTest = DeltakerlistePayload.Arrangor(arrangorInTest.organisasjonsnummer)

		private val fullyPopulatedV2PayloadInTest = deltakerlistePayloadInTest.copy(
			type = DeltakerlistePayload.GRUPPE_V2_TYPE,
			arrangor = arrangorDtoInTest,
		)
	}
}
