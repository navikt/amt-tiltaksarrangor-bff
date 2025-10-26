package no.nav.tiltaksarrangor.unleash

import io.getunleash.Unleash
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode
import no.nav.tiltaksarrangor.unleash.UnleashToggle.Companion.ENABLE_KOMET_DELTAKERE
import no.nav.tiltaksarrangor.unleash.UnleashToggle.Companion.LES_GJENNOMFORINGER_V2
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class UnleashToggleTest {
	private val unleashClient: Unleash = mockk(relaxed = true)
	private val sut = UnleashToggle(unleashClient)

	@Nested
	inner class ErKometMasterForTiltakstype {
		@ParameterizedTest
		@EnumSource(
			value = Tiltakskode::class,
			names = [
				"ARBEIDSFORBEREDENDE_TRENING",
				"OPPFOLGING",
				"AVKLARING",
				"ARBEIDSRETTET_REHABILITERING",
				"DIGITALT_OPPFOLGINGSTILTAK",
				"VARIG_TILRETTELAGT_ARBEID_SKJERMET",
				"GRUPPE_ARBEIDSMARKEDSOPPLAERING",
				"JOBBKLUBB",
				"GRUPPE_FAG_OG_YRKESOPPLAERING",
			],
		)
		fun `returnerer true for tiltakstyper som Komet alltid er master for`(kode: Tiltakskode) {
			sut.erKometMasterForTiltakstype(kode) shouldBe true
		}

		@ParameterizedTest
		@EnumSource(
			value = Tiltakskode::class,
			names = [
				"ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING",
				"ENKELTPLASS_FAG_OG_YRKESOPPLAERING",
				"HOYERE_UTDANNING",
			],
		)
		fun `returnerer false hvis toggle ENABLE_KOMET_DELTAKERE er av`(kode: Tiltakskode) {
			every { unleashClient.isEnabled(ENABLE_KOMET_DELTAKERE) } returns false

			sut.erKometMasterForTiltakstype(kode) shouldBe false
		}

		@ParameterizedTest
		@EnumSource(
			value = Tiltakskode::class,
			names = [
				"ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING",
				"ENKELTPLASS_FAG_OG_YRKESOPPLAERING",
				"HOYERE_UTDANNING",
			],
		)
		fun `returnerer false for enkeltplass tiltak`(kode: Tiltakskode) {
			sut.erKometMasterForTiltakstype(kode) shouldBe false
		}
	}

	@Nested
	inner class SkalLeseGjennomforingerV2 {
		@Test
		fun `returnerer true nar toggle LES_GJENNOMFORINGER_V2 er pa`() {
			every { unleashClient.isEnabled(LES_GJENNOMFORINGER_V2) } returns true
			sut.skalLeseGjennomforingerV2() shouldBe true
		}

		@Test
		fun `returnerer false nar toggle LES_GJENNOMFORINGER_V2 er av`() {
			every { unleashClient.isEnabled(LES_GJENNOMFORINGER_V2) } returns false
			sut.skalLeseGjennomforingerV2() shouldBe false
		}
	}
}
