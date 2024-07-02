package no.nav.tiltaksarrangor

import io.getunleash.FakeUnleash
import io.getunleash.Unleash
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.tiltaksarrangor.mock.MockAmtArrangorHttpServer
import no.nav.tiltaksarrangor.mock.MockAmtPersonHttpServer
import no.nav.tiltaksarrangor.mock.MockAmtTiltakHttpServer
import no.nav.tiltaksarrangor.testutils.DbTestDataUtils
import no.nav.tiltaksarrangor.testutils.SingletonPostgresContainer
import no.nav.tiltaksarrangor.utils.Issuer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.util.UUID

@ActiveProfiles("test")
@ExtendWith(SpringExtension::class)
@EnableMockOAuth2Server
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IntegrationTest {
	@Autowired
	lateinit var mockOAuth2Server: MockOAuth2Server

	@LocalServerPort
	private var port: Int = 0

	fun serverUrl() = "http://localhost:$port"

	val client =
		OkHttpClient.Builder()
			.callTimeout(Duration.ofMinutes(5))
			.build()

	@AfterEach
	fun cleanDatabase() {
		DbTestDataUtils.cleanDatabase(postgresDataSource)
	}

	companion object {
		val mockAmtTiltakServer = MockAmtTiltakHttpServer()
		val mockAmtArrangorServer = MockAmtArrangorHttpServer()
		val mockAmtPersonServer = MockAmtPersonHttpServer()
		val postgresDataSource = SingletonPostgresContainer.getDataSource()

		@JvmStatic
		@DynamicPropertySource
		fun registerProperties(registry: DynamicPropertyRegistry) {
			mockAmtTiltakServer.start()
			registry.add("amt-tiltak.url", mockAmtTiltakServer::serverUrl)
			mockAmtArrangorServer.start()
			registry.add("amt-arrangor.url", mockAmtArrangorServer::serverUrl)
			mockAmtPersonServer.start()
			registry.add("amt-person.url", mockAmtPersonServer::serverUrl)

			val container = SingletonPostgresContainer.getContainer()

			KafkaContainer(DockerImageName.parse(getKafkaImage())).apply {
				start()
				System.setProperty("KAFKA_BROKERS", bootstrapServers)
			}

			registry.add("spring.datasource.url") { container.jdbcUrl }
			registry.add("spring.datasource.username") { container.username }
			registry.add("spring.datasource.password") { container.password }
			registry.add("spring.datasource.hikari.maximum-pool-size") { 3 }
		}
	}

	fun sendRequest(
		method: String,
		path: String,
		body: RequestBody? = null,
		headers: Map<String, String> = emptyMap(),
	): Response {
		val reqBuilder =
			Request.Builder()
				.url("${serverUrl()}$path")
				.method(method, body)

		headers.forEach {
			reqBuilder.addHeader(it.key, it.value)
		}

		return client.newCall(reqBuilder.build()).execute()
	}

	fun getTokenxToken(
		fnr: String,
		audience: String = "amt-tiltaksarrangor-bff-client-id",
		issuerId: String = Issuer.TOKEN_X,
		clientId: String = "amt-tiltaksarrangor-flate",
		claims: Map<String, Any> =
			mapOf(
				"acr" to "Level4",
				"idp" to "idporten",
				"client_id" to clientId,
				"pid" to fnr,
			),
	): String {
		return mockOAuth2Server.issueToken(
			issuerId,
			clientId,
			DefaultOAuth2TokenCallback(
				issuerId = issuerId,
				subject = UUID.randomUUID().toString(),
				audience = listOf(audience),
				claims = claims,
				expiry = 3600,
			),
		).serialize()
	}

	fun emptyRequest(): RequestBody {
		val mediaTypeHtml = "application/json".toMediaType()
		return "".toRequestBody(mediaTypeHtml)
	}
}

private fun getKafkaImage(): String {
	val tag =
		when (System.getProperty("os.arch")) {
			"aarch64" -> "7.2.2-1-ubi8.arm64"
			else -> "7.2.2"
		}

	return "confluentinc/cp-kafka:$tag"
}

@Profile("test")
@Configuration
class UnleashConfig {
	@Bean
	open fun unleashClient(): Unleash {
		val fakeUnleash = FakeUnleash()
		fakeUnleash.enableAll()
		return fakeUnleash
	}
}
