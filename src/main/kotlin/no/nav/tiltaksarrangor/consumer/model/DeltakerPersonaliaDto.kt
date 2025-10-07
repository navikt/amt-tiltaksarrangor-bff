package no.nav.tiltaksarrangor.consumer.model

import no.nav.amt.lib.models.deltaker.Kontaktinformasjon

data class DeltakerPersonaliaDto(
	val personident: String,
	val navn: NavnDto,
	val kontaktinformasjon: Kontaktinformasjon,
	val skjermet: Boolean,
	val adresse: AdresseDbo?,
	val adressebeskyttelse: String? = null,
)
