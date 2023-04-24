package no.nav.tiltaksarrangor.ingest.repositories

import no.nav.tiltaksarrangor.ingest.repositories.model.ArrangorDbo
import no.nav.tiltaksarrangor.utils.getNullableUUID
import no.nav.tiltaksarrangor.utils.sqlParameters
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ArrangorRepository(
	private val template: NamedParameterJdbcTemplate
) {
	private val rowMapper = RowMapper { rs, _ ->
		ArrangorDbo(
			id = UUID.fromString(rs.getString("id")),
			navn = rs.getString("navn"),
			organisasjonsnummer = rs.getString("organisasjonsnummer"),
			overordnetArrangorId = rs.getNullableUUID("overordnet_arrangor_id")
		)
	}

	fun insertOrUpdateArrangor(arrangorDbo: ArrangorDbo) {
		val sql = """
			INSERT INTO arrangor(id, navn, organisasjonsnummer, overordnet_arrangor_id)
			VALUES (:id,
					:navn,
					:organisasjonsnummer,
					:overordnet_arrangor_id)
			ON CONFLICT (organisasjonsnummer) DO UPDATE SET
					navn     							 = :navn,
					organisasjonsnummer					 = :organisasjonsnummer,
					overordnet_arrangor_id 			     = :overordnet_arrangor_id
		""".trimIndent()

		template.update(
			sql,
			sqlParameters(
				"id" to arrangorDbo.id,
				"navn" to arrangorDbo.navn,
				"organisasjonsnummer" to arrangorDbo.organisasjonsnummer,
				"overordnet_arrangor_id" to arrangorDbo.overordnetArrangorId
			)
		)
	}

	fun deleteArrangor(arrangorId: UUID): Int {
		return template.update(
			"DELETE FROM arrangor WHERE id = :id",
			sqlParameters("id" to arrangorId)
		)
	}

	fun getArrangor(arrangorId: UUID): ArrangorDbo? {
		return template.query(
			"SELECT * FROM arrangor WHERE id = :id",
			sqlParameters("id" to arrangorId),
			rowMapper
		).firstOrNull()
	}
}
