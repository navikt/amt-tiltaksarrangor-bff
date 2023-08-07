package no.nav.tiltaksarrangor.repositories

import no.nav.tiltaksarrangor.ingest.model.DeltakerlisteStatus
import no.nav.tiltaksarrangor.model.StatusType
import no.nav.tiltaksarrangor.repositories.model.DeltakerDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerMedDeltakerlisteDbo
import no.nav.tiltaksarrangor.repositories.model.DeltakerlisteDbo
import no.nav.tiltaksarrangor.utils.getNullableDouble
import no.nav.tiltaksarrangor.utils.getNullableFloat
import no.nav.tiltaksarrangor.utils.getNullableLocalDate
import no.nav.tiltaksarrangor.utils.getNullableLocalDateTime
import no.nav.tiltaksarrangor.utils.getNullableUUID
import no.nav.tiltaksarrangor.utils.sqlParameters
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID

@Component
class DeltakerRepository(
	private val template: NamedParameterJdbcTemplate
) {
	private val deltakerRowMapper = RowMapper { rs, _ ->
		DeltakerDbo(
			id = UUID.fromString(rs.getString("id")),
			deltakerlisteId = UUID.fromString(rs.getString("deltakerliste_id")),
			personident = rs.getString("personident"),
			fornavn = rs.getString("fornavn"),
			mellomnavn = rs.getString("mellomnavn"),
			etternavn = rs.getString("etternavn"),
			telefonnummer = rs.getString("telefonnummer"),
			epost = rs.getString("epost"),
			erSkjermet = rs.getBoolean("er_skjermet"),
			status = StatusType.valueOf(rs.getString("status")),
			statusGyldigFraDato = rs.getTimestamp("status_gyldig_fra").toLocalDateTime(),
			statusOpprettetDato = rs.getTimestamp("status_opprettet_dato").toLocalDateTime(),
			dagerPerUke = rs.getNullableFloat("dager_per_uke"),
			prosentStilling = rs.getNullableDouble("prosent_stilling"),
			startdato = rs.getNullableLocalDate("start_dato"),
			sluttdato = rs.getNullableLocalDate("slutt_dato"),
			innsoktDato = rs.getDate("innsokt_dato").toLocalDate(),
			bestillingstekst = rs.getString("bestillingstekst"),
			navKontor = rs.getString("navkontor"),
			navVeilederId = rs.getNullableUUID("navveileder_id"),
			navVeilederNavn = rs.getString("navveileder_navn"),
			navVeilederEpost = rs.getString("navveileder_epost"),
			navVeilederTelefon = rs.getString("navveileder_telefon"),
			skjultAvAnsattId = rs.getNullableUUID("skjult_av_ansatt_id"),
			skjultDato = rs.getNullableLocalDateTime("skjult_dato")
		)
	}

	private val deltakerMedDeltakerlisteRowMapper = RowMapper { rs, _ ->
		DeltakerMedDeltakerlisteDbo(
			deltaker = DeltakerDbo(
				id = UUID.fromString(rs.getString("deltakerid")),
				deltakerlisteId = UUID.fromString(rs.getString("deltakerliste_id")),
				personident = rs.getString("personident"),
				fornavn = rs.getString("fornavn"),
				mellomnavn = rs.getString("mellomnavn"),
				etternavn = rs.getString("etternavn"),
				telefonnummer = rs.getString("telefonnummer"),
				epost = rs.getString("epost"),
				erSkjermet = rs.getBoolean("er_skjermet"),
				status = StatusType.valueOf(rs.getString("deltakerstatus")),
				statusGyldigFraDato = rs.getTimestamp("status_gyldig_fra").toLocalDateTime(),
				statusOpprettetDato = rs.getTimestamp("status_opprettet_dato").toLocalDateTime(),
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
				skjultDato = rs.getNullableLocalDateTime("skjult_dato")
			),
			deltakerliste = DeltakerlisteDbo(
				id = UUID.fromString(rs.getString("deltakerliste_id")),
				navn = rs.getString("navn"),
				status = DeltakerlisteStatus.valueOf(rs.getString("deltakerliste_status")),
				arrangorId = UUID.fromString(rs.getString("arrangor_id")),
				_tiltakNavn = rs.getString("tiltak_navn"),
				tiltakType = rs.getString("tiltak_type"),
				startDato = rs.getNullableLocalDate("deltakerliste_start_dato"),
				sluttDato = rs.getNullableLocalDate("delakerliste_slutt_dato"),
				erKurs = rs.getBoolean("er_kurs")
			)
		)
	}

	fun insertOrUpdateDeltaker(deltakerDbo: DeltakerDbo) {
		val sql = """
					INSERT INTO deltaker(id, deltakerliste_id, personident, fornavn, mellomnavn, etternavn, telefonnummer, epost,
										 er_skjermet, status, status_gyldig_fra, status_opprettet_dato, dager_per_uke, prosent_stilling,
										 start_dato, slutt_dato,
										 innsokt_dato, bestillingstekst, navkontor, navveileder_id, navveileder_navn, navveileder_epost,
										 navveileder_telefon, skjult_av_ansatt_id, skjult_dato)
					VALUES (:id,
							:deltakerliste_id,
							:personident,
							:fornavn,
							:mellomnavn,
							:etternavn,
							:telefonnummer,
							:epost,
							:er_skjermet,
							:status,
							:status_gyldig_fra,
							:status_opprettet_dato,
							:dager_per_uke,
							:prosent_stilling,
							:start_dato,
							:slutt_dato,
							:innsokt_dato,
							:bestillingstekst,
							:navkontor,
							:navveileder_id,
							:navveileder_navn,
							:navveileder_epost,
							:navveileder_telefon,
							:skjult_av_ansatt_id,
							:skjult_dato)
					ON CONFLICT (id) DO UPDATE SET deltakerliste_id      = :deltakerliste_id,
												   personident           = :personident,
												   fornavn               = :fornavn,
												   mellomnavn            = :mellomnavn,
												   etternavn             = :etternavn,
												   telefonnummer         = :telefonnummer,
												   epost                 = :epost,
												   er_skjermet           = :er_skjermet,
												   status                = :status,
												   status_gyldig_fra     = :status_gyldig_fra,
												   status_opprettet_dato = :status_opprettet_dato,
												   dager_per_uke         = :dager_per_uke,
												   prosent_stilling      = :prosent_stilling,
												   start_dato            = :start_dato,
												   slutt_dato            = :slutt_dato,
												   innsokt_dato          = :innsokt_dato,
												   bestillingstekst      = :bestillingstekst,
												   navkontor             = :navkontor,
												   navveileder_id        = :navveileder_id,
												   navveileder_navn      = :navveileder_navn,
												   navveileder_epost     = :navveileder_epost,
												   navveileder_telefon   = :navveileder_telefon,
												   skjult_av_ansatt_id   = :skjult_av_ansatt_id,
												   skjult_dato           = :skjult_dato
		""".trimIndent()

		template.update(
			sql,
			sqlParameters(
				"id" to deltakerDbo.id,
				"deltakerliste_id" to deltakerDbo.deltakerlisteId,
				"personident" to deltakerDbo.personident,
				"fornavn" to deltakerDbo.fornavn,
				"mellomnavn" to deltakerDbo.mellomnavn,
				"etternavn" to deltakerDbo.etternavn,
				"telefonnummer" to deltakerDbo.telefonnummer,
				"epost" to deltakerDbo.epost,
				"er_skjermet" to deltakerDbo.erSkjermet,
				"status" to deltakerDbo.status.name,
				"status_gyldig_fra" to deltakerDbo.statusGyldigFraDato,
				"status_opprettet_dato" to deltakerDbo.statusOpprettetDato,
				"dager_per_uke" to deltakerDbo.dagerPerUke,
				"prosent_stilling" to deltakerDbo.prosentStilling,
				"start_dato" to deltakerDbo.startdato,
				"slutt_dato" to deltakerDbo.sluttdato,
				"innsokt_dato" to deltakerDbo.innsoktDato,
				"bestillingstekst" to deltakerDbo.bestillingstekst,
				"navkontor" to deltakerDbo.navKontor,
				"navveileder_id" to deltakerDbo.navVeilederId,
				"navveileder_navn" to deltakerDbo.navVeilederNavn,
				"navveileder_epost" to deltakerDbo.navVeilederEpost,
				"navveileder_telefon" to deltakerDbo.navVeilederTelefon,
				"skjult_av_ansatt_id" to deltakerDbo.skjultAvAnsattId,
				"skjult_dato" to deltakerDbo.skjultDato
			)
		)
	}

	fun deleteDeltaker(deltakerId: UUID): Int {
		template.update(
			"DELETE FROM veileder_deltaker WHERE deltaker_id = :deltaker_id",
			sqlParameters("deltaker_id" to deltakerId)
		)
		template.update(
			"DELETE FROM endringsmelding WHERE deltaker_id = :deltaker_id",
			sqlParameters("deltaker_id" to deltakerId)
		)
		return template.update(
			"DELETE FROM deltaker WHERE id = :id",
			sqlParameters("id" to deltakerId)
		)
	}

	fun getDeltaker(deltakerId: UUID): DeltakerDbo? {
		return template.query(
			"SELECT * FROM deltaker WHERE id = :id",
			sqlParameters("id" to deltakerId),
			deltakerRowMapper
		).firstOrNull()
	}

	fun getDeltakereForDeltakerliste(deltakerlisteId: UUID): List<DeltakerDbo> {
		return template.query(
			"SELECT * FROM deltaker WHERE deltakerliste_id = :deltakerliste_id",
			sqlParameters("deltakerliste_id" to deltakerlisteId),
			deltakerRowMapper
		)
	}

	fun getDeltakereMedDeltakerliste(deltakerIder: List<UUID>): List<DeltakerMedDeltakerlisteDbo> {
		return template.query(
			"""
				SELECT deltaker.id as deltakerid,
						deltakerliste_id,
						personident,
						fornavn,
						mellomnavn,
						etternavn,
						telefonnummer,
						epost,
						er_skjermet,
						deltaker.status as deltakerstatus,
						status_gyldig_fra,
						status_opprettet_dato,
						dager_per_uke,
						prosent_stilling,
						deltaker.start_dato as deltaker_start_dato,
						deltaker.slutt_dato as deltaker_slutt_dato,
						innsokt_dato,
						bestillingstekst,
						navkontor,
						navveileder_id,
						navveileder_navn,
						navveileder_epost,
						navveileder_telefon,
						skjult_av_ansatt_id,
						skjult_dato,
						navn,
						deltakerliste.status as deltakerliste_status,
						arrangor_id,
						tiltak_navn,
						tiltak_type,
						deltakerliste.start_dato as deltakerliste_start_dato,
						deltakerliste.slutt_dato as delakerliste_slutt_dato,
						er_kurs
				FROM deltaker
						 INNER JOIN deltakerliste ON deltakerliste.id = deltaker.deltakerliste_id
				WHERE deltaker.id IN (:ids);
			""".trimIndent(),
			sqlParameters("ids" to deltakerIder),
			deltakerMedDeltakerlisteRowMapper
		)
	}

	fun getDeltakerMedDeltakerliste(deltakerId: UUID): DeltakerMedDeltakerlisteDbo? {
		return template.query(
			"""
				SELECT deltaker.id as deltakerid,
						deltakerliste_id,
						personident,
						fornavn,
						mellomnavn,
						etternavn,
						telefonnummer,
						epost,
						er_skjermet,
						deltaker.status as deltakerstatus,
						status_gyldig_fra,
						status_opprettet_dato,
						dager_per_uke,
						prosent_stilling,
						deltaker.start_dato as deltaker_start_dato,
						deltaker.slutt_dato as deltaker_slutt_dato,
						innsokt_dato,
						bestillingstekst,
						navkontor,
						navveileder_id,
						navveileder_navn,
						navveileder_epost,
						navveileder_telefon,
						skjult_av_ansatt_id,
						skjult_dato,
						navn,
						deltakerliste.status as deltakerliste_status,
						arrangor_id,
						tiltak_navn,
						tiltak_type,
						deltakerliste.start_dato as deltakerliste_start_dato,
						deltakerliste.slutt_dato as delakerliste_slutt_dato,
						er_kurs
				FROM deltaker
						 INNER JOIN deltakerliste ON deltakerliste.id = deltaker.deltakerliste_id
				WHERE deltaker.id = :id;
			""".trimIndent(),
			sqlParameters("id" to deltakerId),
			deltakerMedDeltakerlisteRowMapper
		).firstOrNull()
	}

	fun deleteDeltakereForDeltakerliste(deltakerlisteId: UUID): Int {
		val deltakereSomSkalSlettes = template.query(
			"SELECT id FROM deltaker WHERE deltakerliste_id = :deltakerliste_id",
			sqlParameters("deltakerliste_id" to deltakerlisteId)
		) { rs, _ ->
			UUID.fromString(rs.getString("id"))
		}
		deltakereSomSkalSlettes.forEach { deleteDeltaker(it) }
		return deltakereSomSkalSlettes.size
	}

	fun getDeltakereSomSkalSlettes(slettesDato: LocalDate): List<UUID> {
		return template.query(
			"SELECT id FROM deltaker WHERE skjult_dato IS NOT NULL OR (status IN ('HAR_SLUTTET','IKKE_AKTUELL','AVBRUTT') AND status_gyldig_fra < :slettesDato)",
			sqlParameters("slettesDato" to slettesDato)
		) { rs, _ ->
			UUID.fromString(rs.getString("id"))
		}
	}

	fun skjulDeltaker(deltakerId: UUID, ansattId: UUID) {
		val sql = """
			UPDATE deltaker SET skjult_dato = CURRENT_TIMESTAMP, skjult_av_ansatt_id = :ansattId WHERE id = :deltakerId
		""".trimIndent()

		template.update(
			sql,
			sqlParameters(
				"ansattId" to ansattId,
				"deltakerId" to deltakerId
			)
		)
	}
}
