package no.nav.tiltaksarrangor.mock

import no.nav.tiltaksarrangor.client.dto.ArrangorDto
import no.nav.tiltaksarrangor.client.dto.DeltakerDetaljerDto
import no.nav.tiltaksarrangor.client.dto.DeltakerStatusAarsak
import no.nav.tiltaksarrangor.client.dto.EndringsmeldingDto
import no.nav.tiltaksarrangor.client.dto.GjennomforingDto
import no.nav.tiltaksarrangor.client.dto.NavEnhetDto
import no.nav.tiltaksarrangor.client.dto.NavVeilederDto
import no.nav.tiltaksarrangor.client.dto.TiltakDto
import no.nav.tiltaksarrangor.model.DeltakerStatus
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

	fun addAktiveEndringsmeldingerResponse(deltakerId: UUID) {
		addResponseHandler(
			path = "/api/tiltaksarrangor/endringsmelding/aktiv?deltakerId=$deltakerId",
			MockResponse()
				.setResponseCode(200)
				.setBody(JsonUtils.objectMapper.writeValueAsString(getAktiveEndringsmeldinger()))
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
				)
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
			)
		)
	}
}
