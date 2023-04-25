package no.nav.tiltaksarrangor.ingest.repositories

import no.nav.tiltaksarrangor.ingest.model.DeltakerlisteStatus
import no.nav.tiltaksarrangor.ingest.repositories.model.DeltakerlisteDbo
import no.nav.tiltaksarrangor.utils.getNullableLocalDate
import no.nav.tiltaksarrangor.utils.sqlParameters
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class DeltakerlisteRepository(
	private val template: NamedParameterJdbcTemplate
) {
	private val deltakerlisteRowMapper = RowMapper { rs, _ ->
		DeltakerlisteDbo(
			id = UUID.fromString(rs.getString("id")),
			navn = rs.getString("navn"),
			status = DeltakerlisteStatus.valueOf(rs.getString("status")),
			arrangorId = UUID.fromString(rs.getString("arrangor_id")),
			tiltakNavn = rs.getString("tiltak_navn"),
			tiltakType = rs.getString("tiltak_type"),
			startDato = rs.getNullableLocalDate("start_dato"),
			sluttDato = rs.getNullableLocalDate("slutt_dato")
		)
	}

	fun insertOrUpdateDeltakerliste(deltakerlisteDbo: DeltakerlisteDbo) {
		val sql = """
			INSERT INTO deltakerliste(id, navn, status, arrangor_id, tiltak_navn, tiltak_type, start_dato, slutt_dato)
			VALUES (:id,
					:navn,
					:status,
					:arrangor_id,
					:tiltak_navn,
					:tiltak_type,
					:start_dato,
					:slutt_dato)
			ON CONFLICT (id) DO UPDATE SET
					navn     				= :navn,
					status					= :status,
					arrangor_id 			= :arrangor_id,
					tiltak_navn				= :tiltak_navn,
					tiltak_type				= :tiltak_type,
					start_dato				= :start_dato,
					slutt_dato				= :slutt_dato
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
				"slutt_dato" to deltakerlisteDbo.sluttDato
			)
		)
	}

	fun deleteDeltakerliste(deltakerlisteId: UUID): Int {
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
}
