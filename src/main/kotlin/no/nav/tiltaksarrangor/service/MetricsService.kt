package no.nav.tiltaksarrangor.service

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import no.nav.tiltaksarrangor.model.Vurderingstype
import org.springframework.stereotype.Service

private const val innlogging_metric = "tiltaksarrangorbff_innlogging"
private const val fjernet_deltaker_metric = "tiltaksarrangorbff_fjernet_deltaker"
private const val lagt_til_deltakerliste_metric = "tiltaksarrangorbff_lagttil_deltakerliste"
private const val fjernet_deltakerliste_metric = "tiltaksarrangorbff_fjernet_deltakerliste"
private const val tildelt_veileder_metric = "tiltaksarrangorbff_tildelt_veileder"
private const val tilbakekalt_em_metric = "tiltaksarrangorbff_tilbakekalt_em"
private const val vurdering_opprettet_metric = "tiltaksarrangorbff_vurdering_opprettet"

@Service
class MetricsService(
	private val registry: MeterRegistry
) {
	private val innloggetKoordinatorCounter = registry.counter(innlogging_metric, "rolle", RollePermutasjon.KOORDINATOR.name)
	private val innloggetVeilederCounter = registry.counter(innlogging_metric, "rolle", RollePermutasjon.VEILEDER.name)
	private val innloggetKoordinatorOgVeilederCounter = registry.counter(innlogging_metric, "rolle", RollePermutasjon.KOORDINATOR_OG_VEILEDER.name)
	private val innloggetTotaltCounter = registry.counter(innlogging_metric, "rolle", RollePermutasjon.TOTALT.name)
	private val fjernetDeltakerCounter = registry.counter(fjernet_deltaker_metric)
	private val lagtTilDeltakerlisteCounter = registry.counter(lagt_til_deltakerliste_metric)
	private val fjernetDeltakerlisteCounter = registry.counter(fjernet_deltakerliste_metric)
	private val tildeltVeilederCounter = registry.counter(tildelt_veileder_metric)
	private val tilbakekaltEndringsmeldingCounter = registry.counter(tilbakekalt_em_metric)

	fun incInnloggetAnsatt(roller: List<String>) {
		if (roller.isEmpty()) {
			return
		}
		if (roller.size == 1) {
			if (roller.contains("KOORDINATOR")) {
				innloggetKoordinatorCounter.increment()
			} else {
				innloggetVeilederCounter.increment()
			}
		} else {
			innloggetKoordinatorOgVeilederCounter.increment()
		}
		innloggetTotaltCounter.increment()
	}

	fun incFjernetDeltaker() {
		fjernetDeltakerCounter.increment()
	}

	fun incLagtTilDeltakerliste() {
		lagtTilDeltakerlisteCounter.increment()
	}

	fun incFjernetDeltakerliste() {
		fjernetDeltakerlisteCounter.increment()
	}

	fun incTildeltVeileder() {
		tildeltVeilederCounter.increment()
	}

	fun incTilbakekaltEndringsmelding() {
		tilbakekaltEndringsmeldingCounter.increment()
	}

	fun incVurderingOpprettet(vurderingstype: Vurderingstype) {
		Counter.builder(vurdering_opprettet_metric)
			.tags("vurderingstype", vurderingstype.name)
			.register(registry)
			.increment()
	}

	enum class RollePermutasjon {
		KOORDINATOR, VEILEDER, KOORDINATOR_OG_VEILEDER, TOTALT
	}
}
