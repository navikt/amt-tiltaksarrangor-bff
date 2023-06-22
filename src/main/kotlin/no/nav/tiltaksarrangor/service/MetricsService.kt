package no.nav.tiltaksarrangor.service

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import org.springframework.stereotype.Service

private const val innlogging_metric = "tiltaksarrangorbff_innlogging"

@Service
class MetricsService(
	registry: MeterRegistry
) {
	private val reg = registry

	fun incInnloggetAnsatt(count: Int = 1, roller: List<String>) {
		if (roller.isEmpty()) {
			return
		}
		if (roller.size == 1) {
			if (roller.contains("KOORDINATOR")) {
				reg.gauge(innlogging_metric, Tags.of("rolle", RollePermutasjon.KOORDINATOR.name), count.toDouble())
			} else {
				reg.gauge(innlogging_metric, Tags.of("rolle", RollePermutasjon.VEILEDER.name), count.toDouble())
			}
		} else {
			reg.gauge(innlogging_metric, Tags.of("rolle", RollePermutasjon.KOORDINATOR_OG_VEILEDER.name), count.toDouble())
		}
		reg.gauge(innlogging_metric, Tags.of("rolle", RollePermutasjon.TOTALT.name), count.toDouble())
	}

	enum class RollePermutasjon {
		KOORDINATOR, VEILEDER, KOORDINATOR_OG_VEILEDER, TOTALT
	}
}
