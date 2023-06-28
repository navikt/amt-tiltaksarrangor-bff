package no.nav.tiltaksarrangor.client.amtarrangor

import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.tiltaksarrangor.ingest.model.AnsattDto
import no.nav.tiltaksarrangor.model.exceptions.UnauthorizedException
import no.nav.tiltaksarrangor.utils.CacheUtils
import no.nav.tiltaksarrangor.utils.JsonUtils
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID

@Component
class AmtArrangorClient(
	@Value("\${amt-arrangor.url}") private val amtArrangorUrl: String,
	private val amtArrangorHttpClient: OkHttpClient
) {
	private val log = LoggerFactory.getLogger(javaClass)
	private val mediaTypeJson = "application/json".toMediaType()

	private val personidentToAnsattCache = Caffeine.newBuilder()
		.expireAfterWrite(Duration.ofHours(1))
		.build<String, AnsattDto>()

	fun getAnsatt(personIdent: String): AnsattDto? {
		return CacheUtils.tryCacheFirstNullable(personidentToAnsattCache, personIdent) {
			val request = Request.Builder()
				.url("$amtArrangorUrl/api/ansatt")
				.get()
				.build()

			amtArrangorHttpClient.newCall(request).execute().use { response ->
				if (!response.isSuccessful) {
					when (response.code) {
						401 -> throw UnauthorizedException("Ikke tilgang til å hente ansatt")
						403 -> throw UnauthorizedException("Ikke tilgang til å hente ansatt")
						404 -> return@tryCacheFirstNullable null
						else -> {
							log.error("Kunne ikke hente ansatt fra amt-arrangør, responsekode: ${response.code}")
							throw RuntimeException("Kunne ikke hente ansatt")
						}
					}
				}
				val body = response.body?.string() ?: throw RuntimeException("Tom responsbody")

				return@tryCacheFirstNullable JsonUtils.fromJsonString<AnsattDto>(body)
			}
		}
	}

	fun leggTilDeltakerlisteForKoordinator(ansattId: UUID, deltakerlisteId: UUID, arrangorId: UUID) {
		val request = Request.Builder()
			.url("$amtArrangorUrl/api/ansatt/koordinator/$arrangorId/$deltakerlisteId")
			.post("".toRequestBody(mediaTypeJson))
			.build()

		amtArrangorHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				when (response.code) {
					401 -> throw UnauthorizedException("Ikke tilgang til å legge til deltakerliste i amt-arrangør")
					403 -> throw UnauthorizedException("Ikke tilgang til å legge til deltakerliste i amt-arrangør")
					else -> {
						log.error("Kunne ikke legge til deltakerliste $deltakerlisteId i amt-arrangør, responsekode: ${response.code}")
						throw RuntimeException("Kunne ikke legge til deltakerliste")
					}
				}
			}
		}
		log.info("Oppdatert amt-arrangor med deltakerliste $deltakerlisteId for ansatt $ansattId")
	}

	fun fjernDeltakerlisteForKoordinator(ansattId: UUID, deltakerlisteId: UUID, arrangorId: UUID) {
		val request = Request.Builder()
			.url("$amtArrangorUrl/api/ansatt/koordinator/$arrangorId/$deltakerlisteId")
			.delete()
			.build()

		amtArrangorHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				when (response.code) {
					401 -> throw UnauthorizedException("Ikke tilgang til å fjerne deltakerliste i amt-arrangør")
					403 -> throw UnauthorizedException("Ikke tilgang til å fjerne deltakerliste i amt-arrangør")
					else -> {
						log.error("Kunne ikke fjerne deltakerliste $deltakerlisteId i amt-arrangør, responsekode: ${response.code}")
						throw RuntimeException("Kunne ikke fjerne deltakerliste")
					}
				}
			}
		}
		log.info("Fjernet amt-arrangor deltakerliste $deltakerlisteId for ansatt $ansattId")
	}
}
