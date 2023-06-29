package no.nav.tiltaksarrangor.mock

import no.nav.tiltaksarrangor.client.amttiltak.dto.EndringsmeldingDto
import no.nav.tiltaksarrangor.client.amttiltak.dto.TilgjengeligVeilederDto
import no.nav.tiltaksarrangor.model.DeltakerStatusAarsak
import no.nav.tiltaksarrangor.utils.JsonUtils
import okhttp3.mockwebserver.MockResponse
import java.time.LocalDate
import java.util.UUID

class MockAmtTiltakHttpServer : MockHttpServer(name = "Amt-Tiltak Mock Server") {

	fun addAktiveEndringsmeldingerResponse(deltakerId: UUID) {
		addResponseHandler(
			path = "/api/tiltaksarrangor/endringsmelding/aktiv?deltakerId=$deltakerId",
			MockResponse()
				.setResponseCode(200)
				.setBody(JsonUtils.objectMapper.writeValueAsString(getAktiveEndringsmeldinger()))
		)
	}

	fun addAvsluttDeltakelseResponse(deltakerId: UUID) {
		addResponseHandler(
			path = "/api/tiltaksarrangor/deltaker/$deltakerId/avslutt-deltakelse",
			MockResponse()
				.setResponseCode(200)
		)
	}

	fun addDeltakerErAktuellResponse(deltakerId: UUID) {
		addResponseHandler(
			path = "/api/tiltaksarrangor/deltaker/$deltakerId/er-aktuell",
			MockResponse()
				.setResponseCode(200)
		)
	}

	fun addEndreSluttdatoResponse(deltakerId: UUID) {
		addResponseHandler(
			path = "/api/tiltaksarrangor/deltaker/$deltakerId/endre-sluttdato",
			MockResponse()
				.setResponseCode(200)
		)
	}

	fun addTilbakekallEndringsmeldingResponse(endringsmeldingId: UUID) {
		addResponseHandler(
			path = "/api/tiltaksarrangor/endringsmelding/$endringsmeldingId/tilbakekall",
			MockResponse()
				.setResponseCode(200)
		)
	}

	fun addTilgjengeligeVeiledereResponse(deltakerlisteId: UUID) {
		addResponseHandler(
			path = "/api/tiltaksarrangor/veiledere/tilgjengelig?gjennomforingId=$deltakerlisteId",
			MockResponse()
				.setResponseCode(200)
				.setBody(JsonUtils.objectMapper.writeValueAsString(getTilgjengeligeVeiledere()))
		)
	}

	fun addTildelVeiledereForDeltakerResponse(deltakerId: UUID) {
		addResponseHandler(
			path = "/api/tiltaksarrangor/veiledere?deltakerId=$deltakerId",
			MockResponse()
				.setResponseCode(200)
		)
	}

	fun addSkjulDeltakerResponse(deltakerId: UUID) {
		addResponseHandler(
			path = "/api/tiltaksarrangor/deltaker/$deltakerId/skjul",
			MockResponse()
				.setResponseCode(200)
		)
	}

	fun addOpprettEllerFjernTilgangTilGjennomforingResponse(deltakerlisteId: UUID) {
		addResponseHandler(
			path = "/api/tiltaksarrangor/gjennomforing/$deltakerlisteId/tilgang",
			MockResponse()
				.setResponseCode(200)
		)
	}

	private fun getAktiveEndringsmeldinger(): List<EndringsmeldingDto> {
		return listOf(
			EndringsmeldingDto(
				id = UUID.fromString("27446cc8-30ad-4030-94e3-de438c2af3c6"),
				innhold = EndringsmeldingDto.Innhold.AvsluttDeltakelseInnhold(
					sluttdato = LocalDate.of(2023, 3, 30),
					aarsak = DeltakerStatusAarsak(
						type = DeltakerStatusAarsak.Type.SYK,
						beskrivelse = "har blitt syk"
					)
				),
				type = "AVSLUTT_DELTAKELSE"
			),
			EndringsmeldingDto(
				id = UUID.fromString("5029689f-3de6-4d97-9cfa-552f75625ef2"),
				innhold = EndringsmeldingDto.Innhold.DeltakerErAktuellInnhold(),
				type = "DELTAKER_ER_AKTUELL"
			),
			EndringsmeldingDto(
				id = UUID.fromString("362c7fdd-04e7-4f43-9e56-0939585856eb"),
				innhold = EndringsmeldingDto.Innhold.EndreSluttdatoInnhold(
					sluttdato = LocalDate.of(2023, 5, 3)
				),
				type = "ENDRE_SLUTTDATO"
			)
		)
	}

	private fun getTilgjengeligeVeiledere(): List<TilgjengeligVeilederDto> {
		return listOf(
			TilgjengeligVeilederDto(
				ansattId = UUID.fromString("29bf6799-bb56-4a86-857b-99b529b3dfc4"),
				fornavn = "Fornavn1",
				mellomnavn = null,
				etternavn = "Etternavn1"
			),
			TilgjengeligVeilederDto(
				ansattId = UUID.fromString("e824dbfe-5317-491b-82ed-03b870eed963"),
				fornavn = "Fornavn2",
				mellomnavn = null,
				etternavn = "Etternavn2"
			)
		)
	}
}
