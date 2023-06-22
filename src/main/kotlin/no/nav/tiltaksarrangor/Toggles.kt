package no.nav.tiltaksarrangor

import java.util.UUID

fun erPilot(deltakerlisteId: UUID): Boolean {
	return deltakerlisteId.toString() in listOf("69afc1b8-50b9-472a-8b92-254dec821c3a", "e41ef5c5-2c2e-41f6-97a2-36fca4902b86", "67b63927-3c6f-494b-ad9b-5fff08b8d196")
}

fun isDev(): Boolean {
	val cluster = System.getenv("NAIS_CLUSTER_NAME") ?: "Ikke dev"
	return cluster == "dev-gcp"
}
