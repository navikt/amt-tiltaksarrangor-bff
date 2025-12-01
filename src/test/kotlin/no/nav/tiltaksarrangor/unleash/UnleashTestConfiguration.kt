package no.nav.tiltaksarrangor.unleash

import io.getunleash.FakeUnleash
import io.getunleash.Unleash
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class UnleashTestConfiguration {
	@Bean
	fun unleashClient(): Unleash {
		val fakeUnleash = FakeUnleash()
		fakeUnleash.enableAll()
		return fakeUnleash
	}
}
