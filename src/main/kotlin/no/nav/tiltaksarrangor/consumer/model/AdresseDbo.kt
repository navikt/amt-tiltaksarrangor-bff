package no.nav.tiltaksarrangor.consumer.model

import no.nav.amt.lib.models.person.address.Oppholdsadresse
import no.nav.tiltaksarrangor.model.Adresse
import no.nav.tiltaksarrangor.model.Adressetype
import no.nav.tiltaksarrangor.utils.JsonUtils.objectMapper
import org.postgresql.util.PGobject

// Alt dette er duplikater fra lib men iom at dette brukes til å lagre json i databasen så beholder jeg datastrukturene
// for å unngå farlige situasjoner om modellen endres i lib uten å ta høyde for json formatet
data class AdresseDbo(
	val bostedsadresse: BostedsadresseDbo?,
	val oppholdsadresse: OppholdsadresseDbo?,
	val kontaktadresse: KontaktadresseDbo?,
) {
	fun toPGObject() = PGobject().also {
		it.type = "json"
		it.value = objectMapper.writeValueAsString(this)
	}

	companion object {
		fun fromModel(adresse: no.nav.amt.lib.models.person.address.Adresse) = AdresseDbo(
			bostedsadresse = adresse.bostedsadresse?.let { BostedsadresseDbo.fromModel(bostedsadresse = it) },
			oppholdsadresse = adresse.oppholdsadresse?.let { OppholdsadresseDbo.fromModel(oppholdsadresse = it) },
			kontaktadresse = adresse.kontaktadresse?.let { KontaktadresseDbo.fromModel(kontakadresse = it) },
		)
	}
}

data class BostedsadresseDbo(
	val coAdressenavn: String?,
	val vegadresse: VegadresseDbo?,
	val matrikkeladresse: MatrikkeladresseDbo?,
) {
	fun toAdresse(): Adresse {
		if (vegadresse != null) {
			return Adresse(
				adressetype = Adressetype.BOSTEDSADRESSE,
				postnummer = vegadresse.postnummer,
				poststed = vegadresse.poststed,
				tilleggsnavn = vegadresse.tilleggsnavn,
				adressenavn = vegadresse.tilAdressenavn(coAdressenavn),
			)
		} else if (matrikkeladresse != null) {
			return Adresse(
				adressetype = Adressetype.BOSTEDSADRESSE,
				postnummer = matrikkeladresse.postnummer,
				poststed = matrikkeladresse.poststed,
				tilleggsnavn = matrikkeladresse.tilleggsnavn,
				adressenavn = coAdressenavn,
			)
		} else {
			throw IllegalStateException("Bostedsadresse må ha enten veiadresse eller matrikkeladresse")
		}
	}

	companion object {
		fun fromModel(bostedsadresse: no.nav.amt.lib.models.person.address.Bostedsadresse) = BostedsadresseDbo(
			coAdressenavn = bostedsadresse.coAdressenavn,
			vegadresse = bostedsadresse.vegadresse?.let { VegadresseDbo.fromModel(it) },
			matrikkeladresse = bostedsadresse.matrikkeladresse?.let { MatrikkeladresseDbo.fromModel(it) },
		)
	}
}

data class OppholdsadresseDbo(
	val coAdressenavn: String?,
	val vegadresse: VegadresseDbo?,
	val matrikkeladresse: MatrikkeladresseDbo?,
) {
	fun toAdresse(): Adresse {
		if (vegadresse != null) {
			return Adresse(
				adressetype = Adressetype.OPPHOLDSADRESSE,
				postnummer = vegadresse.postnummer,
				poststed = vegadresse.poststed,
				tilleggsnavn = vegadresse.tilleggsnavn,
				adressenavn = vegadresse.tilAdressenavn(coAdressenavn),
			)
		} else if (matrikkeladresse != null) {
			return Adresse(
				adressetype = Adressetype.OPPHOLDSADRESSE,
				postnummer = matrikkeladresse.postnummer,
				poststed = matrikkeladresse.poststed,
				tilleggsnavn = matrikkeladresse.tilleggsnavn,
				adressenavn = coAdressenavn,
			)
		} else {
			throw IllegalStateException("Oppholdsadresse må ha enten veiadresse eller matrikkeladresse")
		}
	}

	companion object {
		fun fromModel(oppholdsadresse: Oppholdsadresse) = OppholdsadresseDbo(
			coAdressenavn = oppholdsadresse.coAdressenavn,
			vegadresse = oppholdsadresse.vegadresse?.let {
				VegadresseDbo(
					it.husnummer,
					it.husbokstav,
					it.adressenavn,
					it.tilleggsnavn,
					it.postnummer,
					it.poststed,
				)
			},
			matrikkeladresse = oppholdsadresse.matrikkeladresse?.let { MatrikkeladresseDbo(it.tilleggsnavn, it.postnummer, it.poststed) },
		)
	}
}

data class KontaktadresseDbo(
	val coAdressenavn: String?,
	val vegadresse: VegadresseDbo?,
	val postboksadresse: PostboksadresseDbo?,
) {
	fun toAdresse(): Adresse {
		if (vegadresse != null) {
			return Adresse(
				adressetype = Adressetype.KONTAKTADRESSE,
				postnummer = vegadresse.postnummer,
				poststed = vegadresse.poststed,
				tilleggsnavn = vegadresse.tilleggsnavn,
				adressenavn = vegadresse.tilAdressenavn(coAdressenavn),
			)
		} else if (postboksadresse != null) {
			return Adresse(
				adressetype = Adressetype.KONTAKTADRESSE,
				postnummer = postboksadresse.postnummer,
				poststed = postboksadresse.poststed,
				tilleggsnavn = null,
				adressenavn = postboksadresse.tilAdressenavn(coAdressenavn),
			)
		} else {
			throw IllegalStateException("Kontaktadresse må ha enten veiadresse eller postboksadresse")
		}
	}

	companion object {
		fun fromModel(kontakadresse: no.nav.amt.lib.models.person.address.Kontaktadresse) = KontaktadresseDbo(
			coAdressenavn = kontakadresse.coAdressenavn,
			vegadresse = kontakadresse.vegadresse?.let { VegadresseDbo.fromModel(it) },
			postboksadresse = kontakadresse.postboksadresse?.let { PostboksadresseDbo.fromModel(it) },
		)
	}
}

data class VegadresseDbo(
	val husnummer: String?,
	val husbokstav: String?,
	val adressenavn: String?,
	val tilleggsnavn: String?,
	val postnummer: String,
	val poststed: String,
) {
	fun tilAdressenavn(coAdressenavn: String?): String {
		val adressenavn = "${(adressenavn ?: "")} ${(husnummer ?: "")}${(husbokstav ?: "")}"
		if (coAdressenavn.isNullOrEmpty()) {
			return adressenavn
		}
		return "$coAdressenavn, $adressenavn"
	}

	companion object {
		fun fromModel(vegadresse: no.nav.amt.lib.models.person.address.Vegadresse) = VegadresseDbo(
			husnummer = vegadresse.husnummer,
			husbokstav = vegadresse.husbokstav,
			adressenavn = vegadresse.adressenavn,
			tilleggsnavn = vegadresse.tilleggsnavn,
			postnummer = vegadresse.postnummer,
			poststed = vegadresse.poststed,
		)
	}
}

data class MatrikkeladresseDbo(
	val tilleggsnavn: String?,
	val postnummer: String,
	val poststed: String,
) {
	companion object {
		fun fromModel(matrikkeladresse: no.nav.amt.lib.models.person.address.Matrikkeladresse) = MatrikkeladresseDbo(
			tilleggsnavn = matrikkeladresse.tilleggsnavn,
			postnummer = matrikkeladresse.postnummer,
			poststed = matrikkeladresse.poststed,
		)
	}
}

data class PostboksadresseDbo(
	val postboks: String,
	val postnummer: String,
	val poststed: String,
) {
	fun tilAdressenavn(coAdressenavn: String?): String {
		if (coAdressenavn.isNullOrEmpty()) {
			return "Postboks $postboks"
		}
		return "$coAdressenavn, postboks $postboks"
	}

	companion object {
		fun fromModel(postboksadresse: no.nav.amt.lib.models.person.address.Postboksadresse) = PostboksadresseDbo(
			postboks = postboksadresse.postboks,
			postnummer = postboksadresse.postnummer,
			poststed = postboksadresse.poststed,
		)
	}
}
