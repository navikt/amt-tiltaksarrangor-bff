package no.nav.tiltaksarrangor.utils

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import java.sql.ResultSet
import java.util.UUID

fun <V> sqlParameters(vararg pairs: Pair<String, V>): MapSqlParameterSource {
	return MapSqlParameterSource().addValues(pairs.toMap())
}

fun ResultSet.getNullableUUID(columnLabel: String): UUID? {
	return this.getString(columnLabel)
		?.let { UUID.fromString(it) }
}