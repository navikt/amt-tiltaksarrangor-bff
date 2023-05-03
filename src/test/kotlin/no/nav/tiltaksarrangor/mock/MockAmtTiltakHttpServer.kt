package no.nav.tiltaksarrangor.mock

import no.nav.tiltaksarrangor.client.dto.ArrangorDto
import no.nav.tiltaksarrangor.client.dto.DeltakerDetaljerDto
import no.nav.tiltaksarrangor.client.dto.DeltakerDto
import no.nav.tiltaksarrangor.client.dto.DeltakerStatusDto
import no.nav.tiltaksarrangor.client.dto.DeltakerlisteDto
import no.nav.tiltaksarrangor.client.dto.DeltakeroversiktDto
import no.nav.tiltaksarrangor.client.dto.EndringsmeldingDto
import no.nav.tiltaksarrangor.client.dto.GjennomforingDto
import no.nav.tiltaksarrangor.client.dto.KoordinatorInfoDto
import no.nav.tiltaksarrangor.client.dto.NavEnhetDto
import no.nav.tiltaksarrangor.client.dto.NavVeilederDto
import no.nav.tiltaksarrangor.client.dto.TilgjengeligVeilederDto
import no.nav.tiltaksarrangor.client.dto.TiltakDto
import no.nav.tiltaksarrangor.client.dto.VeilederDto
import no.nav.tiltaksarrangor.client.dto.VeilederInfoDto
import no.nav.tiltaksarrangor.client.dto.VeiledersDeltakerDto
import no.nav.tiltaksarrangor.koordinator.model.Koordinator
import no.nav.tiltaksarrangor.model.DeltakerStatus
import no.nav.tiltaksarrangor.model.DeltakerStatusAarsak
import no.nav.tiltaksarrangor.model.StatusType
import no.nav.tiltaksarrangor.utils.JsonUtils
import okhttp3.mockwebserver.MockResponse
import java.time.LocalDate
import java.util.UUID

class MockAmtTiltakHttpServer : MockHttpServer(name = "Amt-Tiltak Mock Server") {
	fun addMineRollerResponse() {
		addResponseHandler(
			path = "/api/tiltaksarrangor/ansatt/meg/roller",
			MockResponse()
				.setResponseCode(200)
				.setBody(JsonUtils.objectMapper.writeValueAsString(listOf("KOORDINATOR", "VEILEDER", "KOORDINATOR", "VEILEDER", "VEILEDER")))
		)
	}

	fun addDeltakerResponse(deltakerId: UUID) {
		addResponseHandler(
			path = "/api/tiltaksarrangor/deltaker/$deltakerId",
			MockResponse()
				.setResponseCode(200)
				.setBody(JsonUtils.objectMapper.writeValueAsString(getDeltaker(deltakerId)))
		)
	}

	fun addDeltakerFailureResponse(deltakerId: UUID) {
		addResponseHandler(
			path = "/api/tiltaksarrangor/deltaker/$deltakerId",
			MockResponse().setResponseCode(403)
		)
	}

	fun addVeilederResponse(deltakerId: UUID) {
		addResponseHandler(
			path = "/api/tiltaksarrangor/veiledere?deltakerId=$deltakerId",
			MockResponse()
				.setResponseCode(200)
				.setBody(
					JsonUtils.objectMapper.writeValueAsString(
						listOf(
							VeilederDto(
								id = UUID.fromString("4f2fb9a7-69c7-4524-bc52-2d00344675ab"),
								ansattId = UUID.fromString("2d5fc2f7-a9e6-4830-a987-4ff135a70c10"),
								deltakerId = deltakerId,
								erMedveileder = false,
								fornavn = "Fornavn",
								mellomnavn = null,
								etternavn = "Etternavn"
							),
							VeilederDto(
								id = UUID.fromString("d8e1af61-0c5e-4843-8bca-ecd9b55c44bc"),
								ansattId = UUID.fromString("7c43b43b-43be-4d4b-8057-d907c5f1e5c5"),
								deltakerId = deltakerId,
								erMedveileder = true,
								fornavn = "Per",
								mellomnavn = null,
								etternavn = "Person"
							)
						)
					)
				)
		)
	}

	fun addAktiveEndringsmeldingerResponse(deltakerId: UUID) {
		addResponseHandler(
			path = "/api/tiltaksarrangor/endringsmelding/aktiv?deltakerId=$deltakerId",
			MockResponse()
				.setResponseCode(200)
				.setBody(JsonUtils.objectMapper.writeValueAsString(getAktiveEndringsmeldinger()))
		)
	}

	fun addMineDeltakereResponse(deltakerId: UUID) {
		addResponseHandler(
			path = "/api/tiltaksarrangor/veileder/deltakerliste",
			MockResponse()
				.setResponseCode(200)
				.setBody(JsonUtils.objectMapper.writeValueAsString(getVeiledersDeltakerliste(deltakerId)))
		)
	}

	fun addAvsluttDeltakelseResponse(deltakerId: UUID) {
		addResponseHandler(
			path = "/api/tiltaksarrangor/deltaker/$deltakerId/avslutt-deltakelse",
			MockResponse()
				.setResponseCode(200)
		)
	}

	fun addTilbyPlassResponse(deltakerId: UUID) {
		addResponseHandler(
			path = "/api/tiltaksarrangor/deltaker/$deltakerId/tilby-plass",
			MockResponse()
				.setResponseCode(200)
		)
	}
	fun addSettPaaVentelisteResponse(deltakerId: UUID) {
		addResponseHandler(
			path = "/api/tiltaksarrangor/deltaker/$deltakerId/sett-paa-venteliste",
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

	fun addMineDeltakerlisterResponse() {
		addResponseHandler(
			path = "/api/tiltaksarrangor/deltakeroversikt",
			MockResponse()
				.setResponseCode(200)
				.setBody(JsonUtils.objectMapper.writeValueAsString(getMineDeltakerlister()))
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

	private fun getDeltaker(deltakerId: UUID): DeltakerDetaljerDto {
		return DeltakerDetaljerDto(
			id = deltakerId,
			fornavn = "Fornavn",
			mellomnavn = null,
			etternavn = "Etternavn",
			fodselsnummer = "10987654321",
			telefonnummer = "90909090",
			epost = "mail@test.no",
			deltakelseProsent = null,
			navEnhet = NavEnhetDto(
				navn = "Nav Oslo"
			),
			navVeileder = NavVeilederDto(
				navn = "Veileder Veiledersen",
				telefon = "56565656",
				epost = "epost@nav.no"
			),
			startDato = LocalDate.of(2023, 2, 1),
			sluttDato = null,
			registrertDato = LocalDate.of(2023, 1, 15).atStartOfDay(),
			status = DeltakerStatus(
				type = StatusType.DELTAR,
				endretDato = LocalDate.of(2023, 2, 1).atStartOfDay()
			),
			gjennomforing = GjennomforingDto(
				id = UUID.fromString("9987432c-e336-4b3b-b73e-b7c781a0823a"),
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
			),
			fjernesDato = null,
			innsokBegrunnelse = "Tror deltakeren vil ha nytte av dette"
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
				innhold = EndringsmeldingDto.Innhold.TilbyPlassInnhold(),
				type = "TILBY_PLASS"
			),
			EndringsmeldingDto(
				id = UUID.fromString("51fcf41a-115c-4a14-bfa9-bee70e86b240"),
				innhold = EndringsmeldingDto.Innhold.SettPaaVentelisteInnhold(),
				type = "SETT_PAA_VENTELISTE"
			),
			EndringsmeldingDto(
				id = UUID.fromString("362c7fdd-04e7-4f43-9e56-0939585856eb"),
				innhold = EndringsmeldingDto.Innhold.EndreSluttdatoInnhold(
					sluttdato = LocalDate.now()
				),
				type = "ENDRE_SLUTTDATO"
			)
		)
	}

	private fun getVeiledersDeltakerliste(deltakerId: UUID): List<VeiledersDeltakerDto> {
		return listOf(
			VeiledersDeltakerDto(
				id = deltakerId,
				fornavn = "Fornavn",
				mellomnavn = null,
				etternavn = "Etternavn",
				fodselsnummer = "10987654321",
				startDato = LocalDate.of(2023, 2, 15),
				sluttDato = null,
				status = DeltakerStatus(
					type = StatusType.DELTAR,
					endretDato = LocalDate.of(2023, 2, 1).atStartOfDay()
				),
				deltakerliste = DeltakerlisteDto(
					id = UUID.fromString("9987432c-e336-4b3b-b73e-b7c781a0823a"),
					navn = "Gjennomføring 1",
					type = "ARBFORB"
				),
				erMedveilederFor = false,
				aktiveEndringsmeldinger = emptyList()
			)
		)
	}

	private fun getMineDeltakerlister(): DeltakeroversiktDto {
		return DeltakeroversiktDto(
			veilederInfo = VeilederInfoDto(
				veilederFor = 4,
				medveilederFor = 7
			),
			koordinatorInfo = KoordinatorInfoDto(
				deltakerlister = listOf(
					DeltakerlisteDto(
						id = UUID.fromString("9987432c-e336-4b3b-b73e-b7c781a0823a"),
						type = "ARBFORB",
						navn = "Gjennomføring 1"
					)
				)
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
					type = no.nav.tiltaksarrangor.client.dto.StatusType.DELTAR,
					endretDato = LocalDate.of(2023, 2, 1).atStartOfDay()
				),
				aktiveEndringsmeldinger = emptyList(),
				aktiveVeiledere = emptyList()
			)
		)
	}
}
