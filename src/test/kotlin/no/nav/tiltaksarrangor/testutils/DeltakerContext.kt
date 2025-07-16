package no.nav.tiltaksarrangor.testutils

import no.nav.tiltaksarrangor.model.StatusType
import no.nav.tiltaksarrangor.repositories.AnsattRepository
import no.nav.tiltaksarrangor.repositories.ArrangorRepository
import no.nav.tiltaksarrangor.repositories.DeltakerRepository
import no.nav.tiltaksarrangor.repositories.DeltakerlisteRepository
import no.nav.tiltaksarrangor.repositories.EndringsmeldingRepository
import no.nav.tiltaksarrangor.repositories.model.AnsattDbo
import no.nav.tiltaksarrangor.repositories.model.ArrangorDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerlisteDbo
import no.nav.tiltaksarrangor.repositories.model.KoordinatorDeltakerlisteDbo
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDateTime
import java.util.UUID

open class DeltakerContext(
	var deltaker: DeltakerDbo = getDeltaker(UUID.randomUUID()),
	val arrangor: ArrangorDbo = getArrangor(),
	val deltakerliste: DeltakerlisteDbo = getDeltakerliste(deltaker.deltakerlisteId, arrangorId = arrangor.id),
	val koordinator: AnsattDbo = getKoordinator(
		id = UUID.randomUUID(),
		arrangorId = arrangor.id,
		deltakerlisteId = deltakerliste.id,
	),
	val veileder: AnsattDbo = getVeileder(
		id = UUID.randomUUID(),
		arrangorId = arrangor.id,
		deltakerId = deltaker.id,
	),
) {
	private val dataSource = SingletonPostgresContainer.getDataSource()
	private val template = NamedParameterJdbcTemplate(dataSource)
	val deltakerRepository = DeltakerRepository(template)
	private val deltakerlisteRepository = DeltakerlisteRepository(template, deltakerRepository)
	private val ansattRepository = AnsattRepository(template)
	private val arrangorRepository = ArrangorRepository(template)
	private val endringsmeldingRepository = EndringsmeldingRepository(template)

	init {
		arrangorRepository.insertOrUpdateArrangor(arrangor)
		deltakerlisteRepository.insertOrUpdateDeltakerliste(deltakerliste)
		ansattRepository.insertOrUpdateAnsatt(koordinator)
		ansattRepository.insertOrUpdateAnsatt(veileder)
		deltakerRepository.insertOrUpdateDeltaker(deltaker)
	}

	fun setKoordinatorDeltakerliste(id: UUID) {
		ansattRepository.insertOrUpdateAnsatt(koordinator.copy(deltakerlister = listOf(KoordinatorDeltakerlisteDbo(id))))
	}

	fun setDeltakerAdressebeskyttet() {
		deltakerRepository.insertOrUpdateDeltaker(deltaker.copy(adressebeskyttet = true))
	}

	fun setDeltakerSkjult() {
		deltakerRepository.insertOrUpdateDeltaker(deltaker.copy(skjultDato = LocalDateTime.now(), skjultAvAnsattId = koordinator.id))
	}

	fun medPersonident(ident: String) {
		deltaker = deltaker.copy(personident = ident)
		deltakerRepository.insertOrUpdateDeltaker(deltaker)
	}

	fun medStatus(status: StatusType, gyldigFraDagerSiden: Long = 1L) {
		deltaker = deltaker.copy(
			status = status,
			statusGyldigFraDato = LocalDateTime.now().minusDays(gyldigFraDagerSiden),
			statusOpprettetDato = LocalDateTime.now().minusDays(gyldigFraDagerSiden),
		)
		deltakerRepository.insertOrUpdateDeltaker(deltaker)
	}

	fun medEndringsmelding() {
		endringsmeldingRepository.insertOrUpdateEndringsmelding(
			getEndringsmelding(
				deltaker.id,
			),
		)
	}

	fun setVenterPaOppstart() {
		deltaker = deltaker.copy(
			startdato = null,
			sluttdato = null,
			status = StatusType.VENTER_PA_OPPSTART,
		)
		deltakerRepository.insertOrUpdateDeltaker(deltaker)
	}
}
