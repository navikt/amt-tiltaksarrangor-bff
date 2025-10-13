package no.nav.tiltaksarrangor.utils

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper

/**
 * Inkluderer type informasjon som er definert av @JsonTypeInfo i lister og andre samlinger
 *
 * Hvis man bruker `writeValueAsString` på en `List<GeneriskType>` så vil den ikke inkludere `type`.
 */
inline fun <reified T> ObjectMapper.writePolymorphicListAsString(value: T): String =
	this.writerFor(object : TypeReference<T>() {}).writeValueAsString(value)
