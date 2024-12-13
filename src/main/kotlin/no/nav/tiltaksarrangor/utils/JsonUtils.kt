package no.nav.tiltaksarrangor.utils

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

object JsonUtils {
	val objectMapper: ObjectMapper =
		ObjectMapper()
			.registerKotlinModule()
			.registerModule(JavaTimeModule())
			.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

	inline fun <reified T> fromJsonString(jsonStr: String): T = objectMapper.readValue(jsonStr)
}

/**
 * Inkluderer type informasjon som er definert av @JsonTypeInfo i lister og andre samlinger
 *
 * Hvis man bruker `writeValueAsString` på en `List<GeneriskType>` så vil den ikke inkludere `type`.
 */
inline fun <reified T> ObjectMapper.writePolymorphicListAsString(value: T): String =
	this.writerFor(object : TypeReference<T>() {}).writeValueAsString(value)
