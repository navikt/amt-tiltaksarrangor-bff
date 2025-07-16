package no.nav.tiltaksarrangor.config

import org.flywaydb.core.api.configuration.FluentConfiguration
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FlywayConfiguration {
	@Bean
	fun flywayCustomizer(): FlywayConfigurationCustomizer? = FlywayConfigurationCustomizer { configuration: FluentConfiguration ->
		configuration.configuration(
			mapOf("flyway.postgresql.transactional.lock" to "false"),
		)
	}
}
