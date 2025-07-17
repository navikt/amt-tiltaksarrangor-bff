package no.nav.tiltaksarrangor

import no.nav.tiltaksarrangor.testutils.DbTestDataUtils.cleanDatabase
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureJdbc
import org.springframework.context.ApplicationContext
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestConstructor
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.utility.DockerImageName
import javax.sql.DataSource

@ActiveProfiles("test")
@AutoConfigureJdbc
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
abstract class RepositoryTestBase {
	@Autowired
	protected lateinit var template: NamedParameterJdbcTemplate

	@Autowired
	protected lateinit var dataSource: DataSource

	@Autowired
	protected lateinit var applicationContext: ApplicationContext

	@AfterEach
	fun cleanDatabase() {
		cleanDatabase(dataSource)
	}

	companion object {
		private const val POSTGRES_DOCKER_IMAGE_NAME = "postgres:14-alpine"

		@Suppress("unused")
		private val postgres = createContainer().apply {
			start()
		}

		@JvmStatic
		@DynamicPropertySource
		@Suppress("unused")
		fun overrideProps(registry: DynamicPropertyRegistry) {
			registry.add("spring.datasource.url", postgres::getJdbcUrl)
			registry.add("spring.datasource.username", postgres::getUsername)
			registry.add("spring.datasource.password", postgres::getPassword)
		}

		private fun createContainer(): PostgreSQLContainer<Nothing> = PostgreSQLContainer<Nothing>(
			DockerImageName
				.parse(POSTGRES_DOCKER_IMAGE_NAME)
				.asCompatibleSubstituteFor("postgres"),
		).apply {
			addEnv("TZ", "Europe/Oslo")
			waitingFor(HostPortWaitStrategy())
		}
	}
}
