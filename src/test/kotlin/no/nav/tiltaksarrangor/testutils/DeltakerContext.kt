package no.nav.tiltaksarrangor.testutils

import no.nav.tiltaksarrangor.repositories.AnsattRepository
import no.nav.tiltaksarrangor.repositories.ArrangorRepository
import no.nav.tiltaksarrangor.repositories.DeltakerRepository
import no.nav.tiltaksarrangor.repositories.DeltakerlisteRepository
import no.nav.tiltaksarrangor.repositories.model.AnsattDbo
import no.nav.tiltaksarrangor.repositories.model.ArrangorDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerlisteDbo
import no.nav.tiltaksarrangor.repositories.model.KoordinatorDeltakerlisteDbo
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDateTime
import java.util.UUID

open class DeltakerContext(
	val arrangor: ArrangorDbo = getArrangor(),
	val deltakerliste: DeltakerlisteDbo = getDeltakerliste(arrangorId = arrangor.id),
	val koordinator: AnsattDbo = getKoordinator(
		id = UUID.randomUUID(),
		arrangorId = arrangor.id,
		deltakerlisteId = deltakerliste.id,
	),
	val deltaker: DeltakerDbo = getDeltaker(UUID.randomUUID(), deltakerlisteId = deltakerliste.id),
) {
	private val dataSource = SingletonPostgresContainer.getDataSource()
	private val template = NamedParameterJdbcTemplate(dataSource)
	private val deltakerRepository = DeltakerRepository(template)
	private val deltakerlisteRepository = DeltakerlisteRepository(template, deltakerRepository)
	private val ansattRepository = AnsattRepository(template)
	private val arrangorRepository = ArrangorRepository(template)

	init {
		arrangorRepository.insertOrUpdateArrangor(arrangor)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		ansattRepository.insertOrUpdateAnsatt(koordinator)
		deltakerRepository.insertOrUpdateDeltaker(deltaker)
	}

	fun setKoordinatorDeltakerliste(id: UUID) {
		ansattRepository.insertOrUpdateAnsatt(koordinator.copy(deltakerlister = listOf(KoordinatorDeltakerlisteDbo(id))))
	}

	fun setDeltakerAdressebeskyttet() {
		deltakerRepository.insertOrUpdateDeltaker(deltaker.copy(adressebeskyttet = true))
	}

	fun setDeltakerSkjult() {
		deltakerRepository.insertOrUpdateDeltaker(deltaker.copy(skjultDato = LocalDateTime.now()))
	}
}
