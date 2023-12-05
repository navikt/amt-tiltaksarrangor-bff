package no.nav.tiltaksarrangor.client.amttiltak.request

import no.nav.tiltaksarrangor.model.DeltakerStatusAarsak

data class DeltakerIkkeAktuellRequest(
	val aarsak: DeltakerStatusAarsak,
)
