package no.nav.tiltaksarrangor.config

import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@EnableOAuth2Client(cacheEnabled = true)
@Configuration
class HttpClientConfig {
	@Bean
	fun amtTiltakHttpClient(
		clientConfigurationProperties: ClientConfigurationProperties,
		oAuth2AccessTokenService: OAuth2AccessTokenService
	): OkHttpClient {
		val registrationName = "amt-tiltak-tokenx"
		val clientProperties = clientConfigurationProperties.registration[registrationName]
			?: throw RuntimeException("Fant ikke config for $registrationName")
		return OkHttpClient.Builder()
			.connectTimeout(5, TimeUnit.SECONDS)
			.readTimeout(5, TimeUnit.SECONDS)
			.followRedirects(false)
			.addInterceptor(bearerTokenInterceptor(clientProperties, oAuth2AccessTokenService))
			.build()
	}

	@Bean
	fun amtArrangorHttpClient(
		clientConfigurationProperties: ClientConfigurationProperties,
		oAuth2AccessTokenService: OAuth2AccessTokenService
	): OkHttpClient {
		val registrationName = "amt-arrangor-tokenx"
		val clientProperties = clientConfigurationProperties.registration[registrationName]
			?: throw RuntimeException("Fant ikke config for $registrationName")
		return OkHttpClient.Builder()
			.connectTimeout(5, TimeUnit.SECONDS)
			.readTimeout(5, TimeUnit.SECONDS)
			.followRedirects(false)
			.addInterceptor(bearerTokenInterceptor(clientProperties, oAuth2AccessTokenService))
			.build()
	}

	@Bean
	fun simpleHttpClient(): OkHttpClient {
		return OkHttpClient.Builder()
			.connectTimeout(5, TimeUnit.SECONDS)
			.readTimeout(5, TimeUnit.SECONDS)
			.followRedirects(false)
			.build()
	}

	private fun bearerTokenInterceptor(
		clientProperties: ClientProperties,
		oAuth2AccessTokenService: OAuth2AccessTokenService
	): Interceptor {
		return Interceptor { chain: Interceptor.Chain ->
			val accessTokenResponse = oAuth2AccessTokenService.getAccessToken(clientProperties)
			val request = chain.request()
			val requestWithToken = request.newBuilder()
				.addHeader("Authorization", "Bearer ${accessTokenResponse.accessToken}")
				.build()
			chain.proceed(requestWithToken)
		}
	}
}
