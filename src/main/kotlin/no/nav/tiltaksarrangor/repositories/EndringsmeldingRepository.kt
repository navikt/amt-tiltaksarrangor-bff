package no.nav.tiltaksarrangor.repositories

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import no.nav.amt.lib.models.deltaker.Kilde
import no.nav.amt.lib.models.deltakerliste.tiltakstype.ArenaKode
import no.nav.amt.lib.utils.objectMapper
import no.nav.tiltaksarrangor.consumer.model.EndringsmeldingType
import no.nav.tiltaksarrangor.consumer.model.Innhold
import no.nav.tiltaksarrangor.consumer.model.Oppstartstype
import no.nav.tiltaksarrangor.model.DeltakerlisteStatus
import no.nav.tiltaksarrangor.model.Endringsmelding
import no.nav.tiltaksarrangor.repositories.model.DeltakerDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerlisteDbo
import no.nav.tiltaksarrangor.repositories.model.EndringsmeldingDbo
import no.nav.tiltaksarrangor.repositories.model.EndringsmeldingMedDeltakerOgDeltakerliste
import no.nav.tiltaksarrangor.utils.getNullableDouble
import no.nav.tiltaksarrangor.utils.getNullableFloat
import no.nav.tiltaksarrangor.utils.getNullableLocalDate
import no.nav.tiltaksarrangor.utils.getNullableLocalDateTime
import no.nav.tiltaksarrangor.utils.getNullableUUID
import no.nav.tiltaksarrangor.utils.sqlParameters
import org.postgresql.util.PGobject
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class EndringsmeldingRepository(
	private val template: NamedParameterJdbcTemplate,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	private val endringsmeldingRowMapper =
		RowMapper { rs, _ ->
			val type = EndringsmeldingType.valueOf(rs.getString("type"))
			EndringsmeldingDbo(
				id = UUID.fromString(rs.getString("id")),
				deltakerId = UUID.fromString(rs.getString("deltaker_id")),
				type = type,
				innhold = parseInnholdJson(rs.getString("innhold"), type),
				status = Endringsmelding.Status.valueOf(rs.getString("status")),
				sendt = rs.getTimestamp("sendt").toLocalDateTime(),
			)
		}

	private val endringsmeldingMedDeltakerOgDeltakerlisteRowMapper =
		RowMapper { rs, _ ->
			val type = EndringsmeldingType.valueOf(rs.getString("type"))
			EndringsmeldingMedDeltakerOgDeltakerliste(
				endringsmeldingDbo =
					EndringsmeldingDbo(
						id = UUID.fromString(rs.getString("endringsmeldingid")),
						deltakerId = UUID.fromString(rs.getString("deltakerid")),
						type = type,
						innhold = parseInnholdJson(rs.getString("e.innhold"), type),
						status = Endringsmelding.Status.valueOf(rs.getString("em_status")),
						sendt = rs.getTimestamp("sendt").toLocalDateTime(),
					),
				deltakerDbo =
					DeltakerDbo(
						id = UUID.fromString(rs.getString("deltakerid")),
						deltakerlisteId = UUID.fromString(rs.getString("deltakerliste_id")),
						personident = rs.getString("personident"),
						fornavn = rs.getString("fornavn"),
						mellomnavn = rs.getString("mellomnavn"),
						etternavn = rs.getString("etternavn"),
						telefonnummer = rs.getString("telefonnummer"),
						epost = rs.getString("epost"),
						erSkjermet = rs.getBoolean("er_skjermet"),
						adresse = rs.getString("adresse")?.let { objectMapper.readValue(it) },
						vurderingerFraArrangor = rs.getString("vurderinger")?.let { objectMapper.readValue(it) },
						status = DeltakerStatus.Type.valueOf(rs.getString("deltakerstatus")),
						statusGyldigFraDato = rs.getTimestamp("status_gyldig_fra").toLocalDateTime(),
						statusOpprettetDato = rs.getTimestamp("status_opprettet_dato").toLocalDateTime(),
						statusAarsak = rs.getString("aarsak")?.let { objectMapper.readValue(it) },
						dagerPerUke = rs.getNullableFloat("dager_per_uke"),
						prosentStilling = rs.getNullableDouble("prosent_stilling"),
						startdato = rs.getNullableLocalDate("deltaker_start_dato"),
						sluttdato = rs.getNullableLocalDate("deltaker_slutt_dato"),
						innsoktDato = rs.getDate("innsokt_dato").toLocalDate(),
						bestillingstekst = rs.getString("bestillingstekst"),
						navKontor = rs.getString("navkontor"),
						navVeilederId = rs.getNullableUUID("navveileder_id"),
						navVeilederNavn = rs.getString("navveileder_navn"),
						navVeilederEpost = rs.getString("navveileder_epost"),
						navVeilederTelefon = rs.getString("navveileder_telefon"),
						skjultAvAnsattId = rs.getNullableUUID("skjult_av_ansatt_id"),
						skjultDato = rs.getNullableLocalDateTime("skjult_dato"),
						adressebeskyttet = rs.getBoolean("adressebeskyttet"),
						innhold = rs.getString("deltaker.innhold")?.let { objectMapper.readValue(it) },
						kilde = rs.getString("kilde")?.let { Kilde.valueOf(it) },
						historikk = objectMapper.readValue(rs.getString("historikk")),
						sistEndret = rs.getTimestamp("modified_at").toLocalDateTime(),
						forsteVedtakFattet = rs.getNullableLocalDate("forste_vedtak_fattet"),
						erManueltDeltMedArrangor = rs.getBoolean("er_manuelt_delt_med_arrangor"),
					),
				deltakerlisteDbo =
					DeltakerlisteDbo(
						id = UUID.fromString(rs.getString("deltakerliste_id")),
						navn = rs.getString("navn"),
						status = DeltakerlisteStatus.valueOf(rs.getString("deltakerliste_status")),
						arrangorId = UUID.fromString(rs.getString("arrangor_id")),
						tiltakNavn = rs.getString("tiltak_navn"),
						tiltakType = rs.getString("tiltak_type").let { ArenaKode.valueOf(it) },
						startDato = rs.getNullableLocalDate("deltakerliste_start_dato"),
						sluttDato = rs.getNullableLocalDate("delakerliste_slutt_dato"),
						erKurs = rs.getBoolean("er_kurs"),
						tilgjengeligForArrangorFraOgMedDato = rs.getNullableLocalDate("tilgjengelig_fom"),
						oppstartstype = Oppstartstype.valueOf(rs.getString("oppstartstype")),
					),
			)
		}

	fun insertOrUpdateEndringsmelding(endringsmeldingDbo: EndringsmeldingDbo) {
		val sql =
			"""
			INSERT INTO endringsmelding(id, deltaker_id, type, innhold, status, sendt)
			VALUES (:id,
					:deltaker_id,
					:type,
					:innhold,
					:status,
					:sendt)
			ON CONFLICT (id) DO UPDATE SET
					deltaker_id     	= :deltaker_id,
					type				= :type,
					innhold 			= :innhold,
					status				= :status,
					sendt				= :sendt
			""".trimIndent()

		template.update(
			sql,
			sqlParameters(
				"id" to endringsmeldingDbo.id,
				"deltaker_id" to endringsmeldingDbo.deltakerId,
				"type" to endringsmeldingDbo.type.name,
				"innhold" to endringsmeldingDbo.innhold?.toPGObject(),
				"status" to endringsmeldingDbo.status.name,
				"sendt" to endringsmeldingDbo.sendt,
			),
		)
	}

	fun deleteEndringsmelding(endringsmeldingId: UUID): Int = template.update(
		"DELETE FROM endringsmelding WHERE id = :id",
		sqlParameters("id" to endringsmeldingId),
	)

	fun tilbakekallEndringsmelding(endringsmeldingId: UUID): Int = template.update(
		"UPDATE endringsmelding SET status = 'TILBAKEKALT', modified_at = CURRENT_TIMESTAMP WHERE id = :id",
		sqlParameters("id" to endringsmeldingId),
	)

	fun lagreNyOgMerkAktiveEndringsmeldingMedSammeTypeSomUtfort(endringsmeldingDbo: EndringsmeldingDbo) {
		template.update(
			"""
			UPDATE endringsmelding SET status = 'UTDATERT', modified_at = CURRENT_TIMESTAMP
			WHERE deltaker_id = :deltaker_id AND type = :type AND status = 'AKTIV'
			""".trimIndent(),
			sqlParameters(
				"deltaker_id" to endringsmeldingDbo.deltakerId,
				"type" to endringsmeldingDbo.type.name,
			),
		)
		insertOrUpdateEndringsmelding(endringsmeldingDbo)
	}

	fun getEndringsmelding(endringsmeldingId: UUID): EndringsmeldingDbo? = template
		.query(
			"SELECT * FROM endringsmelding WHERE id = :id",
			sqlParameters("id" to endringsmeldingId),
			endringsmeldingRowMapper,
		).firstOrNull()

	fun getEndringsmeldingMedDeltakerOgDeltakerliste(endringsmeldingId: UUID): EndringsmeldingMedDeltakerOgDeltakerliste? = template
		.query(
			"""
			SELECT endringsmelding.id as endringsmeldingid,
					type,
					endringsmelding.innhold as "e.innhold",
					endringsmelding.status as em_status,
					sendt,
					deltaker.id as deltakerid,
					deltakerliste_id,
					personident,
					fornavn,
					mellomnavn,
					etternavn,
					telefonnummer,
					epost,
					er_skjermet,
					adresse,
					vurderinger,
					deltaker.status as deltakerstatus,
					status_gyldig_fra,
					status_opprettet_dato,
					aarsak,
					dager_per_uke,
					prosent_stilling,
					deltaker.start_dato as deltaker_start_dato,
					deltaker.slutt_dato as deltaker_slutt_dato,
					innsokt_dato,
					bestillingstekst,
					deltaker.innhold as "deltaker.innhold",
					kilde,
					historikk,
					navkontor,
					navveileder_id,
					navveileder_navn,
					navveileder_epost,
					navveileder_telefon,
					skjult_av_ansatt_id,
					skjult_dato,
					adressebeskyttet,
					navn,
					deltakerliste.status as deltakerliste_status,
					arrangor_id,
					tiltak_navn,
					tiltak_type,
					deltakerliste.start_dato as deltakerliste_start_dato,
					deltakerliste.slutt_dato as delakerliste_slutt_dato,
					er_kurs,
					oppstartstype,
					tilgjengelig_fom,
					deltaker.modified_at as modified_at,
					forste_vedtak_fattet,
					er_manuelt_delt_med_arrangor
			FROM endringsmelding
			         INNER JOIN deltaker ON deltaker.id = endringsmelding.deltaker_id
			         INNER JOIN deltakerliste ON deltakerliste.id = deltaker.deltakerliste_id
			WHERE endringsmelding.id = :id AND endringsmelding.status = 'AKTIV'
			""".trimIndent(),
			sqlParameters("id" to endringsmeldingId),
			endringsmeldingMedDeltakerOgDeltakerlisteRowMapper,
		).firstOrNull()

	fun getEndringsmeldingerForDeltakere(deltakerIder: List<UUID>): List<EndringsmeldingDbo> {
		if (deltakerIder.isEmpty()) {
			return emptyList()
		}
		return template.query(
			"SELECT * FROM endringsmelding WHERE deltaker_id in(:ids) AND status = 'AKTIV'",
			sqlParameters("ids" to deltakerIder),
			endringsmeldingRowMapper,
		)
	}

	fun getEndringsmeldingerForDeltaker(deltakerId: UUID): List<EndringsmeldingDbo> = template.query(
		"SELECT * FROM endringsmelding WHERE deltaker_id = :deltaker_id",
		sqlParameters("deltaker_id" to deltakerId),
		endringsmeldingRowMapper,
	)

	private fun parseInnholdJson(innholdJson: String?, type: EndringsmeldingType): Innhold {
		if (innholdJson == null) {
			log.error("Kan ikke lese endringsmelding med type $type som mangler innhold")
			throw IllegalStateException("Endringsmelding med type $type må ha innhold")
		}

		return when (type) {
			EndringsmeldingType.LEGG_TIL_OPPSTARTSDATO -> objectMapper.readValue<Innhold.LeggTilOppstartsdatoInnhold>(innholdJson)
			EndringsmeldingType.ENDRE_OPPSTARTSDATO -> objectMapper.readValue<Innhold.EndreOppstartsdatoInnhold>(innholdJson)
			EndringsmeldingType.FORLENG_DELTAKELSE -> objectMapper.readValue<Innhold.ForlengDeltakelseInnhold>(innholdJson)
			EndringsmeldingType.AVSLUTT_DELTAKELSE -> objectMapper.readValue<Innhold.AvsluttDeltakelseInnhold>(innholdJson)
			EndringsmeldingType.DELTAKER_IKKE_AKTUELL -> objectMapper.readValue<Innhold.DeltakerIkkeAktuellInnhold>(innholdJson)
			EndringsmeldingType.ENDRE_DELTAKELSE_PROSENT -> objectMapper.readValue<Innhold.EndreDeltakelseProsentInnhold>(innholdJson)
			EndringsmeldingType.ENDRE_SLUTTDATO -> objectMapper.readValue<Innhold.EndreSluttdatoInnhold>(innholdJson)
			EndringsmeldingType.ENDRE_SLUTTAARSAK -> objectMapper.readValue<Innhold.EndreSluttaarsakInnhold>(innholdJson)
		}
	}
}

fun Innhold.toPGObject() = PGobject().also {
	it.type = "json"
	it.value = objectMapper.writeValueAsString(this)
}
