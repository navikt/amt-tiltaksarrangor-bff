package no.nav.tiltaksarrangor.client.amtperson

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.amt.lib.models.deltaker.Kontaktinformasjon
import no.nav.amt.lib.utils.objectMapper
import no.nav.tiltaksarrangor.consumer.model.NavEnhet
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class AmtPersonClient(
	@Value($$"${amt-person.url}") private val url: String,
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
						"Status=${response.code} error=${response.body.string()}",
				)
				error("Kunne ikke hente NAV-enhet fra amt-person-service")
			}
			val body = response.body

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
						"Status=${response.code} error=${response.body.string()}",
				)
				error("Kunne ikke hente NAV-ansatt fra amt-person-service")
			}
			val body = response.body

			return objectMapper.readValue(body.string())
		}
	}

	fun hentOppdatertKontaktinfo(personident: String): Result<Kontaktinformasjon> = hentOppdatertKontaktinfo(setOf(personident)).mapCatching {
		it[personident] ?: throw NoSuchElementException("Klarte ikke hente kontaktinformasjon for person med ident")
	}

	fun hentOppdatertKontaktinfo(personidenter: Set<String>): Result<KontakinformasjonForPersoner> {
		val request =
			Request
				.Builder()
				.url("$url/api/nav-bruker/kontaktinformasjon")
				.post(objectMapper.writeValueAsString(personidenter).toRequestBody("application/json".toMediaType()))
				.build()

		amtPersonAADHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				val feilmelding = "Kunne ikke hente kontakinformasjon fra amt-person-service."
				log.error(
					feilmelding +
						" Status=${response.code} error=${response.body.string()}",
				)
				return Result.failure(RuntimeException(feilmelding))
			}
			return runCatching { objectMapper.readValue(response.body.string()) }
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

typealias KontakinformasjonForPersoner = Map<String, Kontaktinformasjon>
