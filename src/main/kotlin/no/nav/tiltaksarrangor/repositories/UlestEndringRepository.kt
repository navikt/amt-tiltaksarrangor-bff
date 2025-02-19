package no.nav.tiltaksarrangor.repositories

import no.nav.tiltaksarrangor.model.Oppdatering
import no.nav.tiltaksarrangor.model.UlestEndring
import no.nav.tiltaksarrangor.utils.sqlParameters
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.util.UUID
import no.nav.tiltaksarrangor.melding.forslag.toPGObject
import no.nav.tiltaksarrangor.utils.JsonUtils.fromJsonString

@Component
class UlestEndringRepository(
	private val template: NamedParameterJdbcTemplate,
) {
	private val rowMapper = RowMapper { rs, _ ->
		UlestEndring(
			id = UUID.fromString(rs.getString("id")),
			deltakerId = UUID.fromString(rs.getString("deltaker_id")),
			oppdatering = fromJsonString<Oppdatering>(rs.getString("oppdatering")),
		)
	}

	fun insert(deltakerId: UUID, oppdatering: Oppdatering): UlestEndring {
		val sql =
			"""
			insert into ulest_endring
				(id,
				deltaker_id,
				oppdatering)
			values (
			 	:id,
				:deltakerId,
			 	:oppdatering)
			returning *
			""".trimIndent()
		val params = sqlParameters(
			"id" to UUID.randomUUID(),
			"deltakerId" to deltakerId,
			"oppdatering" to toPGObject(oppdatering),
		)

		return template.queryForObject(sql, params, rowMapper) ?: throw RuntimeException("Failed to insert ulest endring")
	}

	fun getMany(deltakerId: UUID): List<UlestEndring> {
		val sql = "select * from ulest_endring where deltaker_id = :deltakerId"
		val params = sqlParameters("deltakerId" to deltakerId)
		return template.query(sql, params, rowMapper)
	}

	fun delete(id: UUID): Int {
		val sql = "delete from ulest_endring where id = :id"
		val params = sqlParameters("id" to id)
		return template.update(sql, params)
	}

	fun getUlesteForslagForDeltakere(deltakerIder: List<UUID>): List<UlestEndring> {
		if (deltakerIder.isEmpty()) {
			return emptyList()
		}

		val sql = "select * from ulest_endring where deltaker_id in(:ids)"
		val params = sqlParameters("ids" to deltakerIder)
		return template.query(sql, params, rowMapper)
	}
}
