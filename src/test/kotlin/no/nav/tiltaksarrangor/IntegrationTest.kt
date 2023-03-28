package no.nav.tiltaksarrangor

import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.tiltaksarrangor.mock.MockAmtTiltakHttpServer
import no.nav.tiltaksarrangor.utils.Issuer
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
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

	private val client = OkHttpClient.Builder()
		.callTimeout(Duration.ofMinutes(5))
		.build()

	companion object {
		val mockAmtTiltakServer = MockAmtTiltakHttpServer()

		@JvmStatic
		@DynamicPropertySource
		fun registerProperties(registry: DynamicPropertyRegistry) {
			mockAmtTiltakServer.start()
			registry.add("amt-tiltak.url", mockAmtTiltakServer::serverUrl)
		}
	}

	fun sendRequest(
		method: String,
		path: String,
		body: RequestBody? = null,
		headers: Map<String, String> = emptyMap()
	): Response {
		val reqBuilder = Request.Builder()
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
		claims: Map<String, Any> = mapOf(
			"acr" to "Level4",
			"idp" to "idporten",
			"client_id" to clientId,
			"pid" to fnr
		)
	): String {
		return mockOAuth2Server.issueToken(
			issuerId,
			clientId,
			DefaultOAuth2TokenCallback(
				issuerId = issuerId,
				subject = UUID.randomUUID().toString(),
				audience = listOf(audience),
				claims = claims,
				expiry = 3600
			)
		).serialize()
	}
}
