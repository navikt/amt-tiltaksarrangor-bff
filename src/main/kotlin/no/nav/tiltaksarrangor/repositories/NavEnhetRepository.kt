package no.nav.tiltaksarrangor.repositories

import no.nav.tiltaksarrangor.ingest.model.NavEnhet
import no.nav.tiltaksarrangor.utils.sqlParameters
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class NavEnhetRepository(private val template: NamedParameterJdbcTemplate) {
	private val rowMapper = RowMapper { rs, _ ->
		NavEnhet(
			id = UUID.fromString(rs.getString("id")),
			enhetId = rs.getString("nav_enhet_nummer"),
			navn = rs.getString("navn"),
		)
	}

	fun upsert(enhet: NavEnhet) {
		val sql =
			"""
			insert into nav_enhet (id, nav_enhet_nummer, navn)
			values (:id,
					:nav_enhet_nummer,
					:navn)
			on conflict (id) do update set
				navn = :navn,
				nav_enhet_nummer = :nav_enhet_nummer,
				modified_at = current_timestamp
			""".trimIndent()
		val params = sqlParameters(
			"id" to enhet.id,
			"nav_enhet_nummer" to enhet.enhetId,
			"navn" to enhet.navn,
		)

		template.update(sql, params)
	}

	fun get(id: UUID): NavEnhet? {
		val sql =
			"""
			select * from nav_enhet where id = :id
			""".trimIndent()
		val params = sqlParameters("id" to id)

		return template.query(sql, params, rowMapper).firstOrNull()
	}
}
