package no.nav.tiltaksarrangor.service

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Service

private const val innlogging_metric = "tiltaksarrangorbff_innlogging"

@Service
class MetricsService(
	registry: MeterRegistry
) {
	private val reg = registry

	private val innloggetKoordinatorCounter = registry.counter(innlogging_metric, "rolle", RollePermutasjon.KOORDINATOR.name)
	private val innloggetVeilederCounter = registry.counter(innlogging_metric, "rolle", RollePermutasjon.VEILEDER.name)
	private val innloggetKoordinatorOgVeilederCounter = registry.counter(innlogging_metric, "rolle", RollePermutasjon.KOORDINATOR_OG_VEILEDER.name)
	private val innloggetTotaltCounter = registry.counter(innlogging_metric, "rolle", RollePermutasjon.TOTALT.name)

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

	enum class RollePermutasjon {
		KOORDINATOR, VEILEDER, KOORDINATOR_OG_VEILEDER, TOTALT
	}
}
