package no.nav.tiltaksarrangor.mock

import no.nav.tiltaksarrangor.client.amttiltak.dto.ArrangorDto
import no.nav.tiltaksarrangor.client.amttiltak.dto.DeltakerDto
import no.nav.tiltaksarrangor.client.amttiltak.dto.DeltakerStatusDto
import no.nav.tiltaksarrangor.client.amttiltak.dto.EndringsmeldingDto
import no.nav.tiltaksarrangor.client.amttiltak.dto.GjennomforingDto
import no.nav.tiltaksarrangor.client.amttiltak.dto.StatusType
import no.nav.tiltaksarrangor.client.amttiltak.dto.TilgjengeligVeilederDto
import no.nav.tiltaksarrangor.client.amttiltak.dto.TiltakDto
import no.nav.tiltaksarrangor.koordinator.model.Koordinator
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

	fun addKoordinatorerResponse(deltakerlisteId: UUID) {
		addResponseHandler(
			path = "/api/tiltaksarrangor/gjennomforing/$deltakerlisteId/koordinatorer",
			MockResponse()
				.setResponseCode(200)
				.setBody(JsonUtils.objectMapper.writeValueAsString(getKoordinatorer()))
		)
	}

	fun addGjennomforingResponse(deltakerlisteId: UUID) {
		addResponseHandler(
			path = "/api/tiltaksarrangor/gjennomforing/$deltakerlisteId",
			MockResponse()
				.setResponseCode(200)
				.setBody(JsonUtils.objectMapper.writeValueAsString(getGjennomforing(deltakerlisteId)))
		)
	}

	fun addDeltakerePaGjennomforingResponse(deltakerlisteId: UUID) {
		addResponseHandler(
			path = "/api/tiltaksarrangor/deltaker?gjennomforingId=$deltakerlisteId",
			MockResponse()
				.setResponseCode(200)
				.setBody(JsonUtils.objectMapper.writeValueAsString(getDeltakerePaGjennomforing()))
		)
	}

	fun addGetDeltakerlisterLagtTilResponse(deltakerlisteId: UUID) {
		addResponseHandler(
			path = "/api/tiltaksarrangor/gjennomforing",
			MockResponse()
				.setResponseCode(200)
				.setBody(JsonUtils.objectMapper.writeValueAsString(listOf(getGjennomforing(deltakerlisteId))))
		)
	}

	fun addGetTilgjengeligeDeltakerlisterResponse(deltakerlisteId: UUID) {
		addResponseHandler(
			path = "/api/tiltaksarrangor/gjennomforing/tilgjengelig",
			MockResponse()
				.setResponseCode(200)
				.setBody(JsonUtils.objectMapper.writeValueAsString(getGjennomforinger(deltakerlisteId)))
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

	private fun getKoordinatorer(): List<Koordinator> {
		return listOf(
			Koordinator(
				fornavn = "Fornavn1",
				mellomnavn = null,
				etternavn = "Etternavn1"
			),
			Koordinator(
				fornavn = "Fornavn2",
				mellomnavn = null,
				etternavn = "Etternavn2"
			)
		)
	}

	private fun getGjennomforing(deltakerlisteId: UUID): GjennomforingDto {
		return GjennomforingDto(
			id = deltakerlisteId,
			navn = "Gjennomføring 1",
			startDato = LocalDate.of(2023, 2, 1),
			sluttDato = null,
			status = GjennomforingDto.Status.GJENNOMFORES,
			tiltak = TiltakDto(
				tiltakskode = "ARBFORB",
				tiltaksnavn = "Navn på tiltak"
			),
			arrangor = ArrangorDto(
				virksomhetNavn = "Arrangør AS",
				organisasjonNavn = null,
				virksomhetOrgnr = "88888888"
			),
			erKurs = false
		)
	}

	private fun getGjennomforinger(deltakerlisteId: UUID): List<GjennomforingDto> {
		return listOf(
			getGjennomforing(deltakerlisteId),
			GjennomforingDto(
				id = UUID.fromString("fd70758a-44c5-4868-bdcb-b1ddd26cb5e9"),
				navn = "Gjennomføring 2",
				startDato = LocalDate.of(2023, 5, 1),
				sluttDato = LocalDate.of(2023, 6, 1),
				status = GjennomforingDto.Status.GJENNOMFORES,
				tiltak = TiltakDto(
					tiltakskode = "INDOPPFAG",
					tiltaksnavn = "Annet tiltak"
				),
				arrangor = ArrangorDto(
					virksomhetNavn = "Arrangør AS",
					organisasjonNavn = null,
					virksomhetOrgnr = "88888888"
				),
				erKurs = false
			)
		)
	}

	private fun getDeltakerePaGjennomforing(): List<DeltakerDto> {
		return listOf(
			DeltakerDto(
				id = UUID.fromString("252428ac-37a6-4341-bb17-5bad412c9409"),
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				fodselsnummer = "10987654321",
				startDato = LocalDate.of(2023, 2, 1),
				sluttDato = null,
				registrertDato = LocalDate.of(2023, 1, 15).atStartOfDay(),
				status = DeltakerStatusDto(
					type = StatusType.DELTAR,
					endretDato = LocalDate.of(2023, 2, 1).atStartOfDay()
				),
				aktiveEndringsmeldinger = emptyList(),
				aktiveVeiledere = emptyList(),
				navKontor = "NAV Testheim"
			)
		)
	}
}
