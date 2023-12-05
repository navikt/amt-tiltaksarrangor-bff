package no.nav.tiltaksarrangor.ingest.model

data class DeltakerPersonaliaDto(
	val personident: String,
	val navn: NavnDto,
	val kontaktinformasjon: DeltakerKontaktinformasjonDto,
	val skjermet: Boolean,
	val adresse: AdresseDto?,
	val adressebeskyttelse: String? = null,
)
