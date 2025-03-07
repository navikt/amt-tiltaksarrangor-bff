package no.nav.tiltaksarrangor.consumer.model

import no.nav.tiltaksarrangor.model.Adresse
import no.nav.tiltaksarrangor.model.Adressetype
import no.nav.tiltaksarrangor.utils.JsonUtils.objectMapper
import org.postgresql.util.PGobject

data class AdresseDto(
	val bostedsadresse: Bostedsadresse?,
	val oppholdsadresse: Oppholdsadresse?,
	val kontaktadresse: Kontaktadresse?,
) {
	fun toPGObject() = PGobject().also {
		it.type = "json"
		it.value = objectMapper.writeValueAsString(this)
	}
}

data class Bostedsadresse(
	val coAdressenavn: String?,
	val vegadresse: Vegadresse?,
	val matrikkeladresse: Matrikkeladresse?,
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
}

data class Oppholdsadresse(
	val coAdressenavn: String?,
	val vegadresse: Vegadresse?,
	val matrikkeladresse: Matrikkeladresse?,
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
}

data class Kontaktadresse(
	val coAdressenavn: String?,
	val vegadresse: Vegadresse?,
	val postboksadresse: Postboksadresse?,
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
}

data class Vegadresse(
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
}

data class Matrikkeladresse(
	val tilleggsnavn: String?,
	val postnummer: String,
	val poststed: String,
)

data class Postboksadresse(
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
}
