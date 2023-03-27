package no.nav.tiltaksarrangor.client

import no.nav.tiltaksarrangor.client.dto.DeltakerDetaljerDto
import no.nav.tiltaksarrangor.client.dto.EndringsmeldingDto
import no.nav.tiltaksarrangor.client.dto.VeiledersDeltakerDto
import no.nav.tiltaksarrangor.model.exceptions.UnauthorizedException
import no.nav.tiltaksarrangor.utils.JsonUtils.fromJsonString
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class AmtTiltakClient(
	@Value("\${amt-tiltak.url}") private val amtTiltakUrl: String,
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

	fun getDeltaker(deltakerId: UUID): DeltakerDetaljerDto {
		val request = Request.Builder()
			.url("$amtTiltakUrl/api/tiltaksarrangor/deltaker/$deltakerId")
			.get()
			.build()

		amtTiltakHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				when (response.code) {
					401 -> throw UnauthorizedException("Ikke tilgang til deltaker")
					403 -> throw UnauthorizedException("Ikke tilgang til deltaker")
					404 -> throw NoSuchElementException("Fant ikke deltaker med id $deltakerId")
					else -> {
						log.error("Kunne ikke hente deltaker med id $deltakerId fra amt-tiltak, responsekode: ${response.code}")
						throw RuntimeException("Kunne ikke hente deltaker")
					}
				}
			}
			val body = response.body?.string() ?: throw RuntimeException("Tom responsbody")

			return fromJsonString<DeltakerDetaljerDto>(body)
		}
	}

	fun getAktiveEndringsmeldinger(deltakerId: UUID): List<EndringsmeldingDto> {
		val request = Request.Builder()
			.url("$amtTiltakUrl/api/tiltaksarrangor/endringsmelding/aktiv?deltakerId=$deltakerId")
			.get()
			.build()

		amtTiltakHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				when (response.code) {
					401 -> throw UnauthorizedException("Ikke tilgang til deltaker")
					403 -> throw UnauthorizedException("Ikke tilgang til deltaker")
					404 -> throw NoSuchElementException("Fant ikke deltaker med id $deltakerId")
					else -> {
						log.error("Kunne ikke hente endringsmeldinger for deltaker med id $deltakerId fra amt-tiltak, responsekode: ${response.code}")
						throw RuntimeException("Kunne ikke hente endringsmeldinger for deltaker")
					}
				}
			}
			val body = response.body?.string() ?: throw RuntimeException("Tom responsbody")

			return fromJsonString<List<EndringsmeldingDto>>(body)
		}
	}

	fun getVeiledersDeltakere(): List<VeiledersDeltakerDto> {
		val request = Request.Builder()
			.url("$amtTiltakUrl/api/tiltaksarrangor/veileder/deltakerliste")
			.get()
			.build()

		amtTiltakHttpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				when (response.code) {
					401 -> throw UnauthorizedException("Ikke tilgang til mine deltakere")
					403 -> throw UnauthorizedException("Ikke tilgang til mine deltakere")
					else -> {
						log.error("Kunne ikke hente mine deltakere fra amt-tiltak, responsekode: ${response.code}")
						throw RuntimeException("Kunne ikke hente mine deltakere")
					}
				}
			}
			val body = response.body?.string() ?: throw RuntimeException("Tom responsbody")

			return fromJsonString<List<VeiledersDeltakerDto>>(body)
		}
	}
}
