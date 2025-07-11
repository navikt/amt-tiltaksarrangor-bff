package no.nav.tiltaksarrangor

import no.nav.tiltaksarrangor.testutils.DbTestDataUtils.cleanDatabase
import no.nav.tiltaksarrangor.testutils.SingletonPostgresContainer.postgresContainer
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureJdbc
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.ApplicationContext
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.ActiveProfiles
import javax.sql.DataSource

@ActiveProfiles("test")
@AutoConfigureJdbc
abstract class RepositoryTestBase {
	@Autowired
	private lateinit var dataSource: DataSource

	@Autowired
	protected lateinit var template: NamedParameterJdbcTemplate

	@Autowired
	protected lateinit var applicationContext: ApplicationContext

	@AfterEach
	fun cleanDatabase() = cleanDatabase(dataSource)

	companion object {
		@ServiceConnection
		@Suppress("unused")
		private val container = postgresContainer
	}
}
