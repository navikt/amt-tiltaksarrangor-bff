package no.nav.tiltaksarrangor.melding.forslag

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import no.nav.amt.lib.models.arrangor.melding.Forslag
import no.nav.amt.lib.testing.AsyncUtils
import no.nav.tiltaksarrangor.kafka.stringStringConsumer
import no.nav.tiltaksarrangor.melding.MELDING_TOPIC
import no.nav.tiltaksarrangor.repositories.model.AnsattDbo
import no.nav.tiltaksarrangor.repositories.model.ArrangorDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerlisteDbo
import no.nav.tiltaksarrangor.testutils.DeltakerContext
import no.nav.tiltaksarrangor.testutils.getArrangor
import no.nav.tiltaksarrangor.testutils.getDeltaker
import no.nav.tiltaksarrangor.testutils.getDeltakerliste
import no.nav.tiltaksarrangor.testutils.getKoordinator
import no.nav.tiltaksarrangor.utils.JsonUtils.objectMapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.reflect.KClass

class ForslagCtx(
	val forslag: Forslag,
	arrangor: ArrangorDbo = getArrangor(),
	deltakerliste: DeltakerlisteDbo = getDeltakerliste(arrangorId = arrangor.id),
	koordinator: AnsattDbo = getKoordinator(
		id = forslag.opprettetAvArrangorAnsattId ?: UUID.randomUUID(),
		arrangorId = arrangor.id,
		deltakerlisteId = deltakerliste.id,
	),
	deltaker: DeltakerDbo = getDeltaker(forslag.deltakerId, deltakerlisteId = deltakerliste.id),
) : DeltakerContext(
		arrangor = arrangor,
		deltakerliste = deltakerliste,
		koordinator = koordinator,
		deltaker = deltaker,
	)

fun forlengDeltakelseForslag(sluttdato: LocalDate = LocalDate.now(), begrunnelse: String = "Fordi...") = Forslag(
	id = UUID.randomUUID(),
	deltakerId = UUID.randomUUID(),
	opprettetAvArrangorAnsattId = UUID.randomUUID(),
	opprettet = LocalDateTime.now(),
	begrunnelse = begrunnelse,
	endring = Forslag.ForlengDeltakelse(sluttdato),
	status = Forslag.Status.VenterPaSvar,
)

fun <T : Forslag.Endring> assertProducedForslag(forslagId: UUID, endringstype: KClass<T>) {
	val cache = mutableMapOf<UUID, Forslag>()

	val consumer = stringStringConsumer(MELDING_TOPIC) { k, v ->
		cache[UUID.fromString(k)] = objectMapper.readValue(v)
	}

	consumer.run()

	AsyncUtils.eventually {
		val cachedForslag = cache[forslagId]!!
		cachedForslag.id shouldBe forslagId
		cachedForslag.endring::class shouldBe endringstype
	}

	consumer.stop()
}
