package no.nav.tiltaksarrangor.ingest.repositories

import no.nav.tiltaksarrangor.ingest.repositories.model.ArrangorDbo
import no.nav.tiltaksarrangor.utils.DatabaseUtils.sqlParameters
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
			overordnetEnhetNavn = rs.getString("overordnet_enhet_navn"),
			overordnetEnhetOrganisasjonsnummer = rs.getString("overordnet_enhet_organisasjonsnummer")
		)
	}

	fun insertOrUpdateArrangor(arrangorDbo: ArrangorDbo) {
		val sql = """
			INSERT INTO arrangor(id, navn, organisasjonsnummer, overordnet_enhet_navn, overordnet_enhet_organisasjonsnummer)
			VALUES (:id,
					:navn,
					:organisasjonsnummer,
					:overordnet_enhet_navn,
					:overordnet_enhet_organisasjonsnummer)
			ON CONFLICT (id) DO UPDATE SET
					navn     							 = :navn,
					organisasjonsnummer					 = :organisasjonsnummer,
					overordnet_enhet_navn 			     = :overordnet_enhet_navn,
					overordnet_enhet_organisasjonsnummer = :overordnet_enhet_organisasjonsnummer

		""".trimIndent()

		template.update(
			sql,
			sqlParameters(
				"id" to arrangorDbo.id,
				"navn" to arrangorDbo.navn,
				"organisasjonsnummer" to arrangorDbo.organisasjonsnummer,
				"overordnet_enhet_navn" to arrangorDbo.overordnetEnhetNavn,
				"overordnet_enhet_organisasjonsnummer" to arrangorDbo.overordnetEnhetOrganisasjonsnummer
			)
		)
	}

	fun deleteArrangor(arrangorId: UUID): Int {
		val sql = """
			DELETE FROM arrangor WHERE id = :id
		""".trimIndent()

		return template.update(
			sql,
			sqlParameters(
				"id" to arrangorId
			)
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
