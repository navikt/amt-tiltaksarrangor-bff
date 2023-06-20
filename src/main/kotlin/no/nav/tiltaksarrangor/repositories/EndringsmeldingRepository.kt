package no.nav.tiltaksarrangor.repositories

import no.nav.tiltaksarrangor.ingest.model.EndringsmeldingType
import no.nav.tiltaksarrangor.ingest.model.Innhold
import no.nav.tiltaksarrangor.ingest.model.typerUtenInnhold
import no.nav.tiltaksarrangor.repositories.model.EndringsmeldingDbo
import no.nav.tiltaksarrangor.utils.JsonUtils.fromJsonString
import no.nav.tiltaksarrangor.utils.JsonUtils.objectMapper
import no.nav.tiltaksarrangor.utils.sqlParameters
import org.postgresql.util.PGobject
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class EndringsmeldingRepository(
	private val template: NamedParameterJdbcTemplate
) {
	private val log = LoggerFactory.getLogger(javaClass)

	private val endringsmeldingRowMapper = RowMapper { rs, _ ->
		val type = EndringsmeldingType.valueOf(rs.getString("type"))
		EndringsmeldingDbo(
			id = UUID.fromString(rs.getString("id")),
			deltakerId = UUID.fromString(rs.getString("deltaker_id")),
			type = type,
			innhold = parseInnholdJson(rs.getString("innhold"), type)
		)
	}

	fun insertOrUpdateEndringsmelding(endringsmeldingDbo: EndringsmeldingDbo) {
		val sql = """
			INSERT INTO endringsmelding(id, deltaker_id, type, innhold)
			VALUES (:id,
					:deltaker_id,
					:type,
					:innhold)
			ON CONFLICT (id) DO UPDATE SET
					deltaker_id     	= :deltaker_id,
					type				= :type,
					innhold 			= :innhold
		""".trimIndent()

		template.update(
			sql,
			sqlParameters(
				"id" to endringsmeldingDbo.id,
				"deltaker_id" to endringsmeldingDbo.deltakerId,
				"type" to endringsmeldingDbo.type.name,
				"innhold" to endringsmeldingDbo.innhold?.toPGObject()
			)
		)
	}

	fun deleteEndringsmelding(endringsmeldingId: UUID): Int {
		return template.update(
			"DELETE FROM endringsmelding WHERE id = :id",
			sqlParameters("id" to endringsmeldingId)
		)
	}

	fun getEndringsmelding(endringsmeldingId: UUID): EndringsmeldingDbo? {
		return template.query(
			"SELECT * FROM endringsmelding WHERE id = :id",
			sqlParameters("id" to endringsmeldingId),
			endringsmeldingRowMapper
		).firstOrNull()
	}

	private fun parseInnholdJson(innholdJson: String?, type: EndringsmeldingType): Innhold? {
		if (innholdJson == null && type !in typerUtenInnhold) {
			log.error("Kan ikke lese endringsmelding med type $type som mangler innhold")
			throw IllegalStateException("Endringsmelding med type $type må ha innhold")
		}
		return when (type) {
			EndringsmeldingType.LEGG_TIL_OPPSTARTSDATO ->
				innholdJson?.let { fromJsonString<Innhold.LeggTilOppstartsdatoInnhold>(it) }

			EndringsmeldingType.ENDRE_OPPSTARTSDATO ->
				innholdJson?.let { fromJsonString<Innhold.EndreOppstartsdatoInnhold>(it) }

			EndringsmeldingType.FORLENG_DELTAKELSE ->
				innholdJson?.let { fromJsonString<Innhold.ForlengDeltakelseInnhold>(it) }

			EndringsmeldingType.AVSLUTT_DELTAKELSE ->
				innholdJson?.let { fromJsonString<Innhold.AvsluttDeltakelseInnhold>(it) }

			EndringsmeldingType.DELTAKER_IKKE_AKTUELL ->
				innholdJson?.let { fromJsonString<Innhold.DeltakerIkkeAktuellInnhold>(it) }

			EndringsmeldingType.ENDRE_DELTAKELSE_PROSENT ->
				innholdJson?.let { fromJsonString<Innhold.EndreDeltakelseProsentInnhold>(it) }

			EndringsmeldingType.ENDRE_SLUTTDATO ->
				innholdJson?.let { fromJsonString<Innhold.EndreSluttdatoInnhold>(it) }

			EndringsmeldingType.DELTAKER_ER_AKTUELL -> null
		}
	}
}

fun Innhold.toPGObject() = PGobject().also {
	it.type = "json"
	it.value = objectMapper.writeValueAsString(this)
}
