package no.nav.tiltaksarrangor.repositories

import no.nav.tiltaksarrangor.ingest.model.AnsattRolle
import no.nav.tiltaksarrangor.model.Veiledertype
import no.nav.tiltaksarrangor.repositories.model.AnsattDbo
import no.nav.tiltaksarrangor.repositories.model.AnsattPersonaliaDbo
import no.nav.tiltaksarrangor.repositories.model.AnsattRolleDbo
import no.nav.tiltaksarrangor.repositories.model.AnsattVeilederDbo
import no.nav.tiltaksarrangor.repositories.model.KoordinatorDeltakerlisteDbo
import no.nav.tiltaksarrangor.repositories.model.VeilederDeltakerDbo
import no.nav.tiltaksarrangor.utils.sqlParameters
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class AnsattRepository(
	private val template: NamedParameterJdbcTemplate
) {
	private val ansattPersonaliaRowMapper = RowMapper { rs, _ ->
		AnsattPersonaliaDbo(
			id = UUID.fromString(rs.getString("id")),
			personIdent = rs.getString("personident"),
			fornavn = rs.getString("fornavn"),
			mellomnavn = rs.getString("mellomnavn"),
			etternavn = rs.getString("etternavn")
		)
	}

	private val veilederDeltakerRowMapper = RowMapper { rs, _ ->
		VeilederDeltakerDbo(
			deltakerId = UUID.fromString(rs.getString("deltaker_id")),
			veilederType = Veiledertype.valueOf(rs.getString("veiledertype"))
		)
	}

	private val koordinatorDeltakerlisterRowMapper = RowMapper { rs, _ ->
		KoordinatorDeltakerlisteDbo(
			deltakerlisteId = UUID.fromString(rs.getString("deltakerliste_id"))
		)
	}

	private val ansattRolleRowMapper = RowMapper { rs, _ ->
		AnsattRolleDbo(
			arrangorId = UUID.fromString(rs.getString("arrangor_id")),
			rolle = AnsattRolle.valueOf(rs.getString("rolle"))
		)
	}

	private val ansattVeilederRowMapper = RowMapper { rs, _ ->
		AnsattVeilederDbo(
				ansattPersonaliaDbo = AnsattPersonaliaDbo(id = UUID.fromString(rs.getString("id")),
					personIdent = rs.getString("personident"),
					fornavn = rs.getString("fornavn"),
					mellomnavn = rs.getString("mellomnavn"),
					etternavn = rs.getString("etternavn")
				),
			veilederType = Veiledertype.valueOf(rs.getString("veiledertype"))
		)
	}

	fun updateSistInnlogget(ansattId: UUID) {
		val sql = """
			UPDATE ansatt SET sist_innlogget = CURRENT_TIMESTAMP WHERE id = :ansattId
		""".trimIndent()

		template.update(sql, sqlParameters("ansattId" to ansattId))
	}

	fun insertOrUpdateAnsatt(ansattDbo: AnsattDbo) {
		val sql = """
			INSERT INTO ansatt(id, personident, fornavn, mellomnavn, etternavn)
			VALUES (:id,
					:personident,
					:fornavn,
					:mellomnavn,
					:etternavn)
			ON CONFLICT (id) DO UPDATE SET
					personident 		= :personident,
					fornavn				= :fornavn,
					mellomnavn 			= :mellomnavn,
					etternavn			= :etternavn
		""".trimIndent()

		template.update(
			sql,
			sqlParameters(
				"id" to ansattDbo.id,
				"personident" to ansattDbo.personIdent,
				"fornavn" to ansattDbo.fornavn,
				"mellomnavn" to ansattDbo.mellomnavn,
				"etternavn" to ansattDbo.etternavn
			)
		)

		insertOrUpdateAnsattRolle(ansattDbo.id, ansattDbo.roller)
		insertOrUpdateKoordinatorDeltakerliste(ansattDbo.id, ansattDbo.deltakerlister)
		insertOrUpdateVeilederDeltaker(ansattDbo.id, ansattDbo.veilederDeltakere.distinct())
	}

	private fun insertOrUpdateAnsattRolle(ansattId: UUID, roller: List<AnsattRolleDbo>) {
		template.update(
			"DELETE FROM ansatt_rolle WHERE ansatt_id = :ansatt_id",
			sqlParameters("ansatt_id" to ansattId)
		)
		roller.forEach {
			val sql = """
			INSERT INTO ansatt_rolle(ansatt_id, arrangor_id, rolle)
			VALUES (:ansatt_id,
					:arrangor_id,
					:rolle)
			""".trimIndent()

			template.update(
				sql,
				sqlParameters(
					"ansatt_id" to ansattId,
					"arrangor_id" to it.arrangorId,
					"rolle" to it.rolle.name
				)
			)
		}
	}

	private fun insertOrUpdateKoordinatorDeltakerliste(ansattId: UUID, deltakerlister: List<KoordinatorDeltakerlisteDbo>) {
		template.update(
			"DELETE FROM koordinator_deltakerliste WHERE ansatt_id = :ansatt_id",
			sqlParameters("ansatt_id" to ansattId)
		)
		deltakerlister.forEach {
			val sql = """
			INSERT INTO koordinator_deltakerliste(ansatt_id, deltakerliste_id)
			VALUES (:ansatt_id,
					:deltakerliste_id)
			""".trimIndent()

			template.update(
				sql,
				sqlParameters(
					"ansatt_id" to ansattId,
					"deltakerliste_id" to it.deltakerlisteId
				)
			)
		}
	}

	private fun insertOrUpdateVeilederDeltaker(ansattId: UUID, veilederDeltakere: List<VeilederDeltakerDbo>) {
		template.update(
			"DELETE FROM veileder_deltaker WHERE ansatt_id = :ansatt_id",
			sqlParameters("ansatt_id" to ansattId)
		)
		veilederDeltakere.forEach {
			val sql = """
			INSERT INTO veileder_deltaker(ansatt_id, deltaker_id, veiledertype)
			VALUES (:ansatt_id,
					:deltaker_id,
					:veiledertype)
			""".trimIndent()

			template.update(
				sql,
				sqlParameters(
					"ansatt_id" to ansattId,
					"deltaker_id" to it.deltakerId,
					"veiledertype" to it.veilederType.name
				)
			)
		}
	}

	fun deleteAnsatt(ansattId: UUID): Int {
		return template.update(
			"DELETE FROM ansatt WHERE id = :id",
			sqlParameters("id" to ansattId)
		)
	}

	fun getAnsatt(ansattId: UUID): AnsattDbo? {
		val ansattRolleListe = getAnsattRolleListe(ansattId)
		val koordinatorDeltakerlisteDboListe = getKoordinatorDeltakerlisteDboListe(ansattId)
		val veilederDeltakerDboListe = getVeilederDeltakerDboListe(ansattId)
		val ansattPersonaliaDbo = getAnsattPersonaliaDbo(ansattId)

		return ansattPersonaliaDbo?.let {
			AnsattDbo(
				id = it.id,
				personIdent = it.personIdent,
				fornavn = it.fornavn,
				mellomnavn = it.mellomnavn,
				etternavn = it.etternavn,
				roller = ansattRolleListe,
				deltakerlister = koordinatorDeltakerlisteDboListe,
				veilederDeltakere = veilederDeltakerDboListe
			)
		}
	}

	fun getAnsatt(personIdent: String): AnsattDbo? {
		val ansattPersonaliaDbo = getAnsattPersonaliaDbo(personIdent) ?: return null
		val ansattRolleListe = getAnsattRolleListe(ansattPersonaliaDbo.id)
		val unikeRoller = ansattRolleListe.map { it.rolle }.distinct()

		val koordinatorDeltakerlisteDboListe = unikeRoller.find { it == AnsattRolle.KOORDINATOR }
			?.let { getKoordinatorDeltakerlisteDboListe(ansattPersonaliaDbo.id) } ?: emptyList()
		val veilederDeltakerDboListe = unikeRoller.find { it == AnsattRolle.VEILEDER }
			?.let { getVeilederDeltakerDboListe(ansattPersonaliaDbo.id) } ?: emptyList()

		return AnsattDbo(
			id = ansattPersonaliaDbo.id,
			personIdent = ansattPersonaliaDbo.personIdent,
			fornavn = ansattPersonaliaDbo.fornavn,
			mellomnavn = ansattPersonaliaDbo.mellomnavn,
			etternavn = ansattPersonaliaDbo.etternavn,
			roller = ansattRolleListe,
			deltakerlister = koordinatorDeltakerlisteDboListe,
			veilederDeltakere = veilederDeltakerDboListe
		)
	}

	fun getVeiledereForDeltaker(deltakerId: UUID): List<AnsattVeilederDbo> {
		return template.query(
			"""
				SELECT *
				FROM veileder_deltaker
						 INNER JOIN ansatt a ON a.id = veileder_deltaker.ansatt_id
				WHERE veileder_deltaker.deltaker_id = :deltaker_id;
			""".trimIndent(),
			sqlParameters("deltaker_id" to deltakerId),
			ansattVeilederRowMapper
		)
	}

	fun getAnsattRolleListe(ansattId: UUID): List<AnsattRolleDbo> {
		return template.query(
			"SELECT * FROM ansatt_rolle WHERE ansatt_id = :ansatt_id",
			sqlParameters("ansatt_id" to ansattId),
			ansattRolleRowMapper
		)
	}

	fun getKoordinatorDeltakerlisteDboListe(ansattId: UUID): List<KoordinatorDeltakerlisteDbo> {
		return template.query(
			"SELECT * FROM koordinator_deltakerliste WHERE ansatt_id = :ansatt_id",
			sqlParameters("ansatt_id" to ansattId),
			koordinatorDeltakerlisterRowMapper
		)
	}

	fun getVeilederDeltakerDboListe(ansattId: UUID): List<VeilederDeltakerDbo> {
		return template.query(
			"""
				SELECT *
				FROM veileder_deltaker
				         INNER JOIN deltaker d ON d.id = veileder_deltaker.deltaker_id
				WHERE skjult_dato is NULL AND ansatt_id = :ansatt_id;
			""".trimIndent(),
			sqlParameters("ansatt_id" to ansattId),
			veilederDeltakerRowMapper
		)
	}

	private fun getAnsattPersonaliaDbo(personIdent: String): AnsattPersonaliaDbo? {
		return template.query(
			"SELECT * FROM ansatt WHERE personident = :personIdent",
			sqlParameters("personIdent" to personIdent),
			ansattPersonaliaRowMapper
		).firstOrNull()
	}

	private fun getAnsattPersonaliaDbo(ansattId: UUID): AnsattPersonaliaDbo? {
		return template.query(
			"SELECT * FROM ansatt WHERE id = :id",
			sqlParameters("id" to ansattId),
			ansattPersonaliaRowMapper
		).firstOrNull()
	}
}
