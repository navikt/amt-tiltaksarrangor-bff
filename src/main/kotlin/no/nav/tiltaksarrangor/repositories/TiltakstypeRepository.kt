package no.nav.tiltaksarrangor.repositories

import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode
import no.nav.tiltaksarrangor.repositories.model.TiltakstypeDbo
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class TiltakstypeRepository(
	private val template: NamedParameterJdbcTemplate,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun getById(id: UUID): TiltakstypeDbo? = template
		.query(
			"SELECT id, navn, tiltakskode FROM tiltakstype WHERE id = :id",
			mapOf("id" to id),
			rowMapper,
		).firstOrNull()

	fun upsert(tiltakstype: TiltakstypeDbo) {
		val sql =
			"""
			INSERT INTO tiltakstype(id, navn, tiltakskode)
			VALUES (:id,
					:navn,
					:tiltakskode)
			ON CONFLICT (id) DO UPDATE SET
				navn = :navn,
				tiltakskode = :tiltakskode,
				modified_at = CURRENT_TIMESTAMP
			""".trimIndent()

		template.update(
			sql,
			mapOf(
				"id" to tiltakstype.id,
				"navn" to tiltakstype.navn,
				"tiltakskode" to tiltakstype.tiltakskode.name,
			),
		)

		log.info("Upsertet tiltakstype med id ${tiltakstype.id}")
	}

	companion object {
		private val rowMapper = RowMapper { resultSet, _ ->
			TiltakstypeDbo(
				id = UUID.fromString(resultSet.getString("id")),
				navn = resultSet.getString("navn"),
				tiltakskode = Tiltakskode.valueOf(resultSet.getString("tiltakskode")),
			)
		}
	}
}
