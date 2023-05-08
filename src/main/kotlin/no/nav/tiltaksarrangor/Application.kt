package no.nav.tiltaksarrangor

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableJwtTokenValidation
@EnableScheduling
class Application

fun main(args: Array<String>) {
	runApplication<Application>(*args)
}
