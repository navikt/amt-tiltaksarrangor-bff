package no.nav.tiltaksarrangor.repositories

import no.nav.amt.lib.models.deltakerliste.tiltakstype.ArenaKode
import no.nav.tiltaksarrangor.consumer.model.Oppstartstype
import no.nav.tiltaksarrangor.model.DeltakerlisteStatus
import no.nav.tiltaksarrangor.repositories.model.ArrangorDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerlisteDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerlisteMedArrangorDbo
import no.nav.tiltaksarrangor.utils.getNullableLocalDate
import no.nav.tiltaksarrangor.utils.getNullableUUID
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
	private val deltakerRepository: DeltakerRepository,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	private val deltakerlisteRowMapper =
		RowMapper { rs, _ ->
			DeltakerlisteDbo(
				id = UUID.fromString(rs.getString("id")),
				navn = rs.getString("navn"),
				status = DeltakerlisteStatus.valueOf(rs.getString("status")),
				arrangorId = UUID.fromString(rs.getString("arrangor_id")),
				tiltakNavn = rs.getString("tiltak_navn"),
				tiltakType = rs.getString("tiltak_type").let { ArenaKode.valueOf(it) },
				startDato = rs.getNullableLocalDate("start_dato"),
				sluttDato = rs.getNullableLocalDate("slutt_dato"),
				erKurs = rs.getBoolean("er_kurs"),
				oppstartstype = Oppstartstype.valueOf(rs.getString("oppstartstype")),
				tilgjengeligForArrangorFraOgMedDato = rs.getNullableLocalDate("tilgjengelig_fom"),
			)
		}

	private val deltakerlisteMedArrangorRowMapper =
		RowMapper { rs, _ ->
			DeltakerlisteMedArrangorDbo(
				deltakerlisteDbo =
					DeltakerlisteDbo(
						id = UUID.fromString(rs.getString("deltakerliste_id")),
						navn = rs.getString("deltakerliste_navn"),
						status = DeltakerlisteStatus.valueOf(rs.getString("status")),
						arrangorId = UUID.fromString(rs.getString("arrangor_id")),
						tiltakNavn = rs.getString("tiltak_navn"),
						tiltakType = rs.getString("tiltak_type").let { ArenaKode.valueOf(it) },
						startDato = rs.getNullableLocalDate("start_dato"),
						sluttDato = rs.getNullableLocalDate("slutt_dato"),
						erKurs = rs.getBoolean("er_kurs"),
						oppstartstype = Oppstartstype.valueOf(rs.getString("oppstartstype")),
						tilgjengeligForArrangorFraOgMedDato = rs.getNullableLocalDate("tilgjengelig_fom"),
					),
				arrangorDbo =
					ArrangorDbo(
						id = UUID.fromString(rs.getString("arrangor_id")),
						navn = rs.getString("arrangor_navn"),
						organisasjonsnummer = rs.getString("organisasjonsnummer"),
						overordnetArrangorId = rs.getNullableUUID("overordnet_arrangor_id"),
					),
			)
		}

	fun insertOrUpdateDeltakerliste(deltakerlisteDbo: DeltakerlisteDbo) {
		val sql =
			"""
			INSERT INTO deltakerliste(id, navn, status, arrangor_id, tiltak_navn, tiltak_type, start_dato, slutt_dato, er_kurs, oppstartstype, tilgjengelig_fom)
			VALUES (:id,
					:navn,
					:status,
					:arrangor_id,
					:tiltak_navn,
					:tiltak_type,
					:start_dato,
					:slutt_dato,
					:er_kurs,
					:oppstartstype,
					:tilgjengelig_fom)
			ON CONFLICT (id) DO UPDATE SET
					navn     				= :navn,
					status					= :status,
					arrangor_id 			= :arrangor_id,
					tiltak_navn				= :tiltak_navn,
					tiltak_type				= :tiltak_type,
					start_dato				= :start_dato,
					slutt_dato				= :slutt_dato,
					er_kurs					= :er_kurs,
					oppstartstype			= :oppstartstype,
					tilgjengelig_fom		= :tilgjengelig_fom
			""".trimIndent()

		template.update(
			sql,
			sqlParameters(
				"id" to deltakerlisteDbo.id,
				"navn" to deltakerlisteDbo.navn,
				"status" to deltakerlisteDbo.status.name,
				"arrangor_id" to deltakerlisteDbo.arrangorId,
				"tiltak_navn" to deltakerlisteDbo.tiltakNavn,
				"tiltak_type" to deltakerlisteDbo.tiltakType.name,
				"start_dato" to deltakerlisteDbo.startDato,
				"slutt_dato" to deltakerlisteDbo.sluttDato,
				"er_kurs" to deltakerlisteDbo.erKurs,
				"oppstartstype" to deltakerlisteDbo.oppstartstype.name,
				"tilgjengelig_fom" to deltakerlisteDbo.tilgjengeligForArrangorFraOgMedDato,
			),
		)
	}

	fun deleteDeltakerlisteOgDeltakere(deltakerlisteId: UUID): Int {
		val slettedeDeltakere = deltakerRepository.deleteDeltakereForDeltakerliste(deltakerlisteId)
		log.info("Slettet $slettedeDeltakere deltakere ved sletting av deltakerliste med id $deltakerlisteId")
		template.update(
			"DELETE FROM koordinator_deltakerliste WHERE deltakerliste_id = :deltakerliste_id",
			sqlParameters("deltakerliste_id" to deltakerlisteId),
		)
		return template.update(
			"DELETE FROM deltakerliste WHERE id = :id",
			sqlParameters("id" to deltakerlisteId),
		)
	}

	fun getDeltakerliste(deltakerlisteId: UUID): DeltakerlisteDbo? = template
		.query(
			"SELECT * FROM deltakerliste WHERE id = :id",
			sqlParameters("id" to deltakerlisteId),
			deltakerlisteRowMapper,
		).firstOrNull()

	fun getDeltakerlister(deltakerlisteIder: List<UUID>): List<DeltakerlisteDbo> {
		if (deltakerlisteIder.isEmpty()) {
			return emptyList()
		}
		return template.query(
			"SELECT * FROM deltakerliste WHERE id in(:ids)",
			sqlParameters("ids" to deltakerlisteIder),
			deltakerlisteRowMapper,
		)
	}

	fun getDeltakerlisteMedArrangor(deltakerlisteId: UUID): DeltakerlisteMedArrangorDbo? = template
		.query(
			"""
			SELECT deltakerliste.id as deltakerliste_id,
					deltakerliste.navn as deltakerliste_navn,
					status,
					arrangor_id,
					tiltak_navn,
					tiltak_type,
					start_dato,
					slutt_dato,
					er_kurs,
					oppstartstype,
					tilgjengelig_fom,
					a.navn as arrangor_navn,
					a.organisasjonsnummer,
					a.overordnet_arrangor_id
			FROM deltakerliste
					 INNER JOIN arrangor a ON a.id = deltakerliste.arrangor_id
			WHERE deltakerliste.id = :id;
			""".trimIndent(),
			sqlParameters("id" to deltakerlisteId),
			deltakerlisteMedArrangorRowMapper,
		).firstOrNull()

	fun getDeltakerlisterMedArrangor(arrangorIder: List<UUID>): List<DeltakerlisteMedArrangorDbo> {
		if (arrangorIder.isEmpty()) {
			return emptyList()
		}
		return template.query(
			"""
			SELECT deltakerliste.id as deltakerliste_id,
					deltakerliste.navn as deltakerliste_navn,
					status,
					arrangor_id,
					tiltak_navn,
					tiltak_type,
					start_dato,
					slutt_dato,
					er_kurs,
					oppstartstype,
					tilgjengelig_fom,
					a.navn as arrangor_navn,
					a.organisasjonsnummer,
					a.overordnet_arrangor_id
			FROM deltakerliste
					 INNER JOIN arrangor a ON a.id = deltakerliste.arrangor_id
			WHERE a.id in (:arrangorIds);
			""".trimIndent(),
			sqlParameters("arrangorIds" to arrangorIder),
			deltakerlisteMedArrangorRowMapper,
		)
	}

	fun getDeltakerlisterSomSkalSlettes(slettesDato: LocalDate): List<UUID> = template.query(
		"SELECT id from deltakerliste WHERE status='AVSLUTTET' AND slutt_dato is not NULL AND slutt_dato < :slettesDato",
		sqlParameters("slettesDato" to slettesDato),
	) { rs, _ ->
		UUID.fromString(rs.getString("id"))
	}
}
