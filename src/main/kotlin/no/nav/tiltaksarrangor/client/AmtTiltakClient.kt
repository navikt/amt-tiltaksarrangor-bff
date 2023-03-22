package no.nav.tiltaksarrangor.client

import no.nav.tiltaksarrangor.utils.JsonUtils.fromJsonString
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class AmtTiltakClient(
	@Value("\${amttiltak.url}") private val amtTiltakUrl: String,
	private val amtTiltakHttpClient: OkHttpClient
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun getMineRoller(): List<String> {
		val request = Request.Builder()
			.url("$amtTiltakUrl/api/tiltaksarrangor/ansatt/meg/roller")
			.get()
			.build()

		amtTiltakHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				log.error("Klarte ikke hente ansatts roller fra amt-tiltak, responsekode: ${response.code}")
				throw RuntimeException("Klarte ikke å hente roller for innlogget ansatt")
			}
			val body = response.body?.string() ?: throw RuntimeException("Tom responsbody")

			val roller = fromJsonString<List<String>>(body)

			return roller.distinct()
		}
	}
}
