package no.nav.tiltaksarrangor.repositories

import no.nav.tiltaksarrangor.ingest.model.DeltakerlisteStatus
import no.nav.tiltaksarrangor.repositories.model.DeltakerlisteDbo
import no.nav.tiltaksarrangor.utils.getNullableLocalDate
import no.nav.tiltaksarrangor.utils.sqlParameters
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID

@Component
class DeltakerlisteRepository(
	private val template: NamedParameterJdbcTemplate,
	private val deltakerRepository: DeltakerRepository
) {
	private val log = LoggerFactory.getLogger(javaClass)

	private val deltakerlisteRowMapper = RowMapper { rs, _ ->
		DeltakerlisteDbo(
			id = UUID.fromString(rs.getString("id")),
			navn = rs.getString("navn"),
			status = DeltakerlisteStatus.valueOf(rs.getString("status")),
			arrangorId = UUID.fromString(rs.getString("arrangor_id")),
			tiltakNavn = rs.getString("tiltak_navn"),
			tiltakType = rs.getString("tiltak_type"),
			startDato = rs.getNullableLocalDate("start_dato"),
			sluttDato = rs.getNullableLocalDate("slutt_dato"),
			erKurs = rs.getBoolean("er_kurs")
		)
	}

	fun insertOrUpdateDeltakerliste(deltakerlisteDbo: DeltakerlisteDbo) {
		val sql = """
			INSERT INTO deltakerliste(id, navn, status, arrangor_id, tiltak_navn, tiltak_type, start_dato, slutt_dato, er_kurs)
			VALUES (:id,
					:navn,
					:status,
					:arrangor_id,
					:tiltak_navn,
					:tiltak_type,
					:start_dato,
					:slutt_dato,
					:er_kurs)
			ON CONFLICT (id) DO UPDATE SET
					navn     				= :navn,
					status					= :status,
					arrangor_id 			= :arrangor_id,
					tiltak_navn				= :tiltak_navn,
					tiltak_type				= :tiltak_type,
					start_dato				= :start_dato,
					slutt_dato				= :slutt_dato,
					er_kurs					= :er_kurs
		""".trimIndent()

		template.update(
			sql,
			sqlParameters(
				"id" to deltakerlisteDbo.id,
				"navn" to deltakerlisteDbo.navn,
				"status" to deltakerlisteDbo.status.name,
				"arrangor_id" to deltakerlisteDbo.arrangorId,
				"tiltak_navn" to deltakerlisteDbo.tiltakNavn,
				"tiltak_type" to deltakerlisteDbo.tiltakType,
				"start_dato" to deltakerlisteDbo.startDato,
				"slutt_dato" to deltakerlisteDbo.sluttDato,
				"er_kurs" to deltakerlisteDbo.erKurs
			)
		)
	}

	fun deleteDeltakerlisteOgDeltakere(deltakerlisteId: UUID): Int {
		val slettedeDeltakere = deltakerRepository.deleteDeltakereForDeltakerliste(deltakerlisteId)
		log.info("Slettet $slettedeDeltakere deltakere ved sletting av deltakerliste med id $deltakerlisteId")
		template.update(
			"DELETE FROM koordinator_deltakerliste WHERE deltakerliste_id = :deltakerliste_id",
			sqlParameters("deltakerliste_id" to deltakerlisteId)
		)
		return template.update(
			"DELETE FROM deltakerliste WHERE id = :id",
			sqlParameters("id" to deltakerlisteId)
		)
	}

	fun getDeltakerliste(deltakerlisteId: UUID): DeltakerlisteDbo? {
		return template.query(
			"SELECT * FROM deltakerliste WHERE id = :id",
			sqlParameters("id" to deltakerlisteId),
			deltakerlisteRowMapper
		).firstOrNull()
	}

	fun getDeltakerlister(deltakerlisteIder: List<UUID>): List<DeltakerlisteDbo> {
		if (deltakerlisteIder.isEmpty()) {
			return emptyList()
		}
		return template.query(
			"SELECT * FROM deltakerliste WHERE id in(:ids)",
			sqlParameters("ids" to deltakerlisteIder),
			deltakerlisteRowMapper
		)
	}

	fun getDeltakerlisterSomSkalSlettes(slettesDato: LocalDate): List<UUID> {
		return template.query(
			"SELECT id from deltakerliste WHERE status='AVSLUTTET' AND slutt_dato is not NULL AND slutt_dato < :slettesDato",
			sqlParameters("slettesDato" to slettesDato)
		) { rs, _ ->
			UUID.fromString(rs.getString("id"))
		}
	}
}
