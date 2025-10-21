package no.nav.tiltaksarrangor.consumer.model

import com.fasterxml.jackson.annotation.JsonInclude
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.json.schema.shouldMatchSchema
import io.kotest.matchers.shouldBe
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode
import no.nav.amt.lib.utils.objectMapper
import no.nav.tiltaksarrangor.consumer.ConsumerTestUtils.arrangorInTest
import no.nav.tiltaksarrangor.consumer.ConsumerTestUtils.deltakerlistePayloadInTest
import no.nav.tiltaksarrangor.consumer.model.DeltakerlistePayloadJsonSchemas.deltakerlistePayloadV1Schema
import no.nav.tiltaksarrangor.consumer.model.DeltakerlistePayloadJsonSchemas.deltakerlistePayloadV2Schema
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DeltakerlistePayloadTest {
	@Nested
	inner class Organisasjonsnummer {
		@Test
		fun `returnerer virksomhetsnummer for v1`() {
			fullyPopulatedV1PayloadInTest.organisasjonsnummer shouldBe fullyPopulatedV1PayloadInTest.virksomhetsnummer
		}

		@Test
		fun `organisasjonsnummer - kaster feil hvis virksomhetsnummer mangler`() {
			assertThrows<IllegalStateException> {
				fullyPopulatedV1PayloadInTest.copy(virksomhetsnummer = null).organisasjonsnummer
			}
		}

		@Test
		fun `returnerer arrangor-organisasjonsnummer for v2`() {
			fullyPopulatedV2PayloadInTest.organisasjonsnummer shouldBe fullyPopulatedV2PayloadInTest.arrangor!!.organisasjonsnummer
		}

		@Test
		fun `organisasjonsnummer - kaster feil hvis arrangor-organisasjonsnummer mangler`() {
			assertThrows<IllegalStateException> {
				fullyPopulatedV2PayloadInTest.copy(arrangor = null).organisasjonsnummer
			}
		}
	}

	@Nested
	inner class ToModel {
		@Test
		fun `toModel - mapper felter korrekt`() {
			val arrangor = arrangorInTest
			val deltakerlisteDbo = fullyPopulatedV1PayloadInTest.toDeltakerlisteDbo(
				arrangorId = arrangor.id,
				navnTiltakstype = Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING.name,
			)

			assertSoftly(deltakerlisteDbo) {
				id shouldBe fullyPopulatedV1PayloadInTest.id
				navn shouldBe fullyPopulatedV1PayloadInTest.navn
				status shouldBe fullyPopulatedV1PayloadInTest.toDeltakerlisteStatus()
				arrangorId shouldBe arrangor.id
				tiltakNavn shouldBe Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING.name
				tiltakType shouldBe Tiltakskode.valueOf(fullyPopulatedV1PayloadInTest.tiltakstype.tiltakskode).toArenaKode()
				startDato shouldBe fullyPopulatedV1PayloadInTest.startDato
				sluttDato shouldBe fullyPopulatedV1PayloadInTest.sluttDato
				erKurs shouldBe fullyPopulatedV1PayloadInTest.erKurs()
				oppstartstype shouldBe fullyPopulatedV1PayloadInTest.oppstart
				tilgjengeligForArrangorFraOgMedDato shouldBe fullyPopulatedV1PayloadInTest.tilgjengeligForArrangorFraOgMedDato
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

		@Test
		fun `fullt populert V1 skal matche skjema`() {
			val json = objectMapper
				.copy()
				.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
				.writeValueAsString(fullyPopulatedV1PayloadInTest.copy())

			json.shouldMatchSchema(deltakerlistePayloadV1Schema)
		}
	}

	companion object {
		private val fullyPopulatedV1PayloadInTest = deltakerlistePayloadInTest.copy()

		private val fullyPopulatedV2PayloadInTest = deltakerlistePayloadInTest.copy(
			type = DeltakerlistePayload.GRUPPE_V2_TYPE,
			virksomhetsnummer = null,
			arrangor = DeltakerlistePayload.Arrangor("987654321"),
		)
	}
}
