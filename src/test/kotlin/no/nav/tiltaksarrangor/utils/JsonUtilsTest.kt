package no.nav.tiltaksarrangor.utils

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.readValue

class JsonUtilsTest {
	@Disabled
	@Test
	fun toJson() {
		val expected = listOf(
			MyInterface.A("testing A"),
			MyInterface.B("testing B"),
		)

		val json = objectMapper.writeValueAsString(expected)

		// json shouldBe expected

		val deserialized = objectMapper.readValue<List<MyInterface>>(
			json,
			// object : com.fasterxml.jackson.core.type.TypeReference<List<MyInterface>>() {},
		)

		deserialized shouldBe expected
	}

	companion object {
/*
		@JsonTypeInfo(
			use = JsonTypeInfo.Id.NAME,
			include = JsonTypeInfo.As.PROPERTY,
			property = "type",
		)
*/
		sealed interface MyInterface {
			data class A(
				val a: String,
			) : MyInterface

			data class B(
				val b: String,
			) : MyInterface
		}
	}
}
