package no.nav.tiltaksarrangor.melding.forslag

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.amt.lib.models.arrangor.melding.Forslag
import no.nav.tiltaksarrangor.utils.JsonUtils.objectMapper
import no.nav.tiltaksarrangor.utils.sqlParameters
import org.postgresql.util.PGobject
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ForslagRepository(
	private val template: NamedParameterJdbcTemplate,
) {
	private val rowMapper = RowMapper { rs, _ ->
		Forslag(
			id = UUID.fromString(rs.getString("id")),
			deltakerId = UUID.fromString(rs.getString("deltaker_id")),
			opprettetAvArrangorAnsattId = UUID.fromString(rs.getString("opprettet_av_arrangor_ansatt_id")),
			begrunnelse = rs.getString("begrunnelse"),
			endring = objectMapper.readValue(rs.getString("endring")),
			status = objectMapper.readValue(rs.getString("status")),
			opprettet = rs.getTimestamp("created_at").toLocalDateTime(),
		)
	}

	fun upsert(forslag: Forslag): Forslag {
		val sql =
			"""
			insert into forslag (
				 id,
				 deltaker_id,
				 opprettet_av_arrangor_ansatt_id,
				 begrunnelse,
				 endring,
				 status,
				 created_at
			)
			values (
				:id,
				:deltaker_id,
				:opprettet_av_arrangor_ansatt_id,
				:begrunnelse,
				:endring,
				:status,
				:created_at
			) on conflict (id) do update set
				status = :status,
				modified_at = current_timestamp
			returning *
			""".trimIndent()
		val params = sqlParameters(
			"id" to forslag.id,
			"deltaker_id" to forslag.deltakerId,
			"opprettet_av_arrangor_ansatt_id" to forslag.opprettetAvArrangorAnsattId,
			"begrunnelse" to forslag.begrunnelse,
			"endring" to toPGObject(forslag.endring),
			"status" to toPGObject(forslag.status),
			"created_at" to forslag.opprettet,
		)
		return template.queryForObject(sql, params, rowMapper)
			?: throw NoSuchElementException("Noe gikk galt med upsert av forslag ${forslag.id}")
	}
}

fun toPGObject(value: Any?) = PGobject().also {
	it.type = "json"
	it.value = value?.let { v -> objectMapper.writeValueAsString(v) }
}
