package no.nav.tiltaksarrangor.utils

import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

val objectMapper: ObjectMapper = JsonMapper
	.builder()
	.apply { addModule(KotlinModule.Builder().build()) }
	.build()

/**
 * Inkluderer type informasjon som er definert av @JsonTypeInfo i lister og andre samlinger
 *
 * Hvis man bruker `writeValueAsString` på en `List<GeneriskType>` så vil den ikke inkludere `type`.
 */
inline fun <reified T : Any> ObjectMapper.writePolymorphicListAsString(value: T): String =
	this.writerFor(object : TypeReference<T>() {}).writeValueAsString(value)
