package no.nav.tiltaksarrangor.model

import no.nav.tiltaksarrangor.repositories.model.Deltakelsesinnhold
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Vedtak(
	val id: UUID,
	val deltakerId: UUID,
	val fattet: LocalDateTime?,
	val gyldigTil: LocalDateTime?,
	val deltakerVedVedtak: DeltakerVedVedtak,
	val fattetAvNav: Boolean,
	val opprettet: LocalDateTime,
	val opprettetAv: UUID,
	val opprettetAvEnhet: UUID,
	val sistEndret: LocalDateTime,
	val sistEndretAv: UUID,
	val sistEndretAvEnhet: UUID,
)

data class DeltakerVedVedtak(
	val id: UUID,
	val startdato: LocalDate?,
	val sluttdato: LocalDate?,
	val dagerPerUke: Float?,
	val deltakelsesprosent: Float?,
	val bakgrunnsinformasjon: String?,
	val deltakelsesinnhold: Deltakelsesinnhold?,
	val status: HistorikkStatus,
)

data class HistorikkStatus(
	val id: UUID,
	val type: Type,
	val aarsak: Aarsak?,
	val gyldigFra: LocalDateTime,
	val gyldigTil: LocalDateTime?,
	val opprettet: LocalDateTime,
) {
	data class Aarsak(
		val type: Type,
		val beskrivelse: String?,
	) {
		init {
			if (beskrivelse != null && type != Type.ANNET) {
				error("Aarsak $type skal ikke ha beskrivelse")
			}
		}

		enum class Type {
			SYK,
			FATT_JOBB,
			TRENGER_ANNEN_STOTTE,
			FIKK_IKKE_PLASS,
			IKKE_MOTT,
			ANNET,
			AVLYST_KONTRAKT,
			UTDANNING,
			SAMARBEIDET_MED_ARRANGOREN_ER_AVBRUTT,
		}
	}

	enum class Type {
		KLADD,
		UTKAST_TIL_PAMELDING,
		AVBRUTT_UTKAST,
		VENTER_PA_OPPSTART,
		DELTAR,
		HAR_SLUTTET,
		IKKE_AKTUELL,
		FEILREGISTRERT,
		SOKT_INN,
		VURDERES,
		VENTELISTE,
		AVBRUTT,
		FULLFORT,
		PABEGYNT_REGISTRERING,
	}
}
