package no.nav.tiltaksarrangor.client.dto

data class DeltakerStatusAarsak(
	val type: Type,
	val beskrivelse: String? = null
) {
	enum class Type {
		SYK, FATT_JOBB, TRENGER_ANNEN_STOTTE, FIKK_IKKE_PLASS, UTDANNING, FERDIG, AVLYST_KONTRAKT, IKKE_MOTT, FEILREGISTRERT, ANNET;
	}
}
