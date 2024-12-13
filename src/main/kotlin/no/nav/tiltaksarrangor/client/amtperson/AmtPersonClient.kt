package no.nav.tiltaksarrangor.client.amtperson

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.tiltaksarrangor.ingest.model.NavEnhet
import no.nav.tiltaksarrangor.utils.JsonUtils.objectMapper
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class AmtPersonClient(
	@Value("\${amt-person.url}") private val url: String,
	private val amtPersonAADHttpClient: OkHttpClient,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun hentEnhet(id: UUID): NavEnhet {
		val request =
			Request
				.Builder()
				.url("$url/api/nav-enhet/$id")
				.get()
				.build()

		amtPersonAADHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				log.error(
					"Kunne ikke hente nav-enhet med id $id fra amt-person-service. " +
						"Status=${response.code} error=${response.body?.string()}",
				)
				error("Kunne ikke hente NAV-enhet fra amt-person-service")
			}
			val body = response.body ?: error("Body manglet i response")

			return objectMapper.readValue<NavEnhetDto>(body.string()).toNavEnhet()
		}
	}

	fun hentNavAnsatt(id: UUID): NavAnsattResponse {
		val request =
			Request
				.Builder()
				.url("$url/api/nav-ansatt/$id")
				.get()
				.build()

		amtPersonAADHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				log.error(
					"Kunne ikke hente nav-ansatt med id $id fra amt-person-service. " +
						"Status=${response.code} error=${response.body?.string()}",
				)
				error("Kunne ikke hente NAV-ansatt fra amt-person-service")
			}
			val body = response.body ?: error("Body manglet i response")

			return objectMapper.readValue(body.string())
		}
	}
}

data class NavAnsattResponse(
	val id: UUID,
	val navIdent: String,
	val navn: String,
	val epost: String?,
	val telefon: String?,
)

data class NavEnhetDto(
	val id: UUID,
	val enhetId: String,
	val navn: String,
) {
	fun toNavEnhet(): NavEnhet = NavEnhet(
		id = id,
		enhetsnummer = enhetId,
		navn = navn,
	)
}
