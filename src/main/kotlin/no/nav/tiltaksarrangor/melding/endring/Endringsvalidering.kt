package no.nav.tiltaksarrangor.melding.endring

import no.nav.tiltaksarrangor.repositories.model.DeltakerDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerlisteDbo
import java.time.Duration
import java.time.LocalDate
import java.time.temporal.ChronoUnit

fun validerLeggTilOppstartsdato(
	startdato: LocalDate,
	sluttdato: LocalDate?,
	deltaker: DeltakerDbo,
	deltakerliste: DeltakerlisteDbo,
) {
	require(deltaker.startdato == null) {
		"Kan ikke legge til ny oppstartsdato når startdato finnes fra før"
	}

	validerOppstartsdato(startdato, sluttdato, deltakerliste)
}

private fun validerOppstartsdato(
	startdato: LocalDate,
	sluttdato: LocalDate?,
	deltakerliste: DeltakerlisteDbo,
) {
	require(!startdato.isBefore(deltakerliste.startDato)) {
		"Startdato kan ikke være tidligere enn deltakerlistens startdato"
	}
	sluttdato?.let { validerSluttdato(it, startdato, deltakerliste) }
}

private fun validerSluttdato(
	sluttdato: LocalDate,
	startdato: LocalDate?,
	deltakerliste: DeltakerlisteDbo,
) {
	require(deltakerliste.sluttDato == null || !sluttdato.isAfter(deltakerliste.sluttDato)) {
		"Sluttdato kan ikke være senere enn deltakerlistens sluttdato"
	}
	require(startdato == null || !sluttdato.isBefore(startdato)) {
		"Sluttdato må være etter startdato"
	}

	startdato?.let { validerVarighet(it, sluttdato, maxVarighet(deltakerliste)) }
}

private fun validerVarighet(
	startdato: LocalDate,
	sluttdato: LocalDate,
	maxVarighet: Duration?,
) {
	if (maxVarighet == null) return

	val senesteSluttdato = startdato.plusDays(maxVarighet.toDays())

	require(!sluttdato.isAfter(senesteSluttdato)) {
		"Sluttdato $sluttdato er etter seneste mulige sluttdato $senesteSluttdato"
	}
}

val HELLIGDAGER: Duration = weeks(1)

private fun maxVarighet(deltakerliste: DeltakerlisteDbo): Duration? = when (deltakerliste.tiltakType) {
	"GRUPPEAMO",
	"GRUFAGYRKE",
	-> years(3)

	"DIGIOPPARB" -> weeks(8) + HELLIGDAGER
	"ARBFORB" -> years(2)

	"AVKLARAG",
	"ARBRRHDAG",
	-> weeks(12)

	"INDOPPFAG" -> years(1)

	"VASV",
	"JOBBK",
	-> null

	else -> throw NotImplementedError("maxVarighet for tiltakstype: ${deltakerliste.tiltakType} er ikke implementert")
}

private fun years(n: Long) = Duration.of(n * 365, ChronoUnit.DAYS)

private fun months(n: Long) = Duration.of(n * 30, ChronoUnit.DAYS)

private fun weeks(n: Long) = Duration.of(n * 7, ChronoUnit.DAYS)
