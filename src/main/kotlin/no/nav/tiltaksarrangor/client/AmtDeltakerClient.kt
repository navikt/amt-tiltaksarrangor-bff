package no.nav.tiltaksarrangor.client

import no.nav.tiltaksarrangor.utils.JsonUtils.objectMapper
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class AmtDeltakerClient(
	@Value($$"${amt-deltaker.url}") private val url: String,
	private val amtDeltakerHttpClient: OkHttpClient,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun reproduserDeltakere(deltakere: List<UUID>) {
		val request =
			Request
				.Builder()
				.url("$url/internal/deltakere/reproduser")
				.post(
					objectMapper
						.writeValueAsString(ReproduserDeltakereRequest(deltakere))
						.toRequestBody("application/json".toMediaTypeOrNull()),
				).build()

		amtDeltakerHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				log.error(
					"Noe gikk galt med reprodusering av deltakere. " +
						"Status=${response.code} error=${response.body.string()}",
				)
				error("Noe gikk galt med reprodusering av deltakere i amt-deltaker")
			}
		}
	}
}

data class ReproduserDeltakereRequest(
	val deltakere: List<UUID>,
	val forcedUpdate: Boolean = false,
	val publiserTilDeltakerV1: Boolean = false,
)
