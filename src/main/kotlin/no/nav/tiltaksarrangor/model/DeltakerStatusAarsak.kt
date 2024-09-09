package no.nav.tiltaksarrangor.model

data class DeltakerStatusAarsak(
	val type: Type,
	val beskrivelse: String?,
) {
	enum class Type {
		SYK,
		FATT_JOBB,
		TRENGER_ANNEN_STOTTE,
		FIKK_IKKE_PLASS,
		UTDANNING,
		FERDIG,
		AVLYST_KONTRAKT,
		IKKE_MOTT,
		FEILREGISTRERT,
		ANNET,
		SAMARBEIDET_MED_ARRANGOREN_ER_AVBRUTT,
	}
}
