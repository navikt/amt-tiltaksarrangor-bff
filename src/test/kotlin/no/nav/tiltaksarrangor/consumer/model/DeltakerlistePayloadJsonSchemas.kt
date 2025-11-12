package no.nav.tiltaksarrangor.consumer.model

import io.kotest.assertions.json.schema.jsonSchema
import io.kotest.assertions.json.schema.obj
import io.kotest.assertions.json.schema.string
import io.kotest.matchers.collections.beIn
import no.nav.tiltaksarrangor.consumer.model.DeltakerlistePayload.Companion.ENKELTPLASS_V2_TYPE
import no.nav.tiltaksarrangor.consumer.model.DeltakerlistePayload.Companion.GRUPPE_V2_TYPE

object DeltakerlistePayloadJsonSchemas {
	val arrangorSchema = jsonSchema {
		obj {
			withProperty("organisasjonsnummer") { string() }
			additionalProperties = false
		}
	}

	val tiltakstypeSchema = jsonSchema {
		obj {
			withProperty("tiltakskode") { string() }
			additionalProperties = false
		}
	}

	val deltakerlistePayloadV2Schema = jsonSchema {
		obj {
			withProperty("type") {
				string { beIn(setOf(ENKELTPLASS_V2_TYPE, GRUPPE_V2_TYPE)) }
			}
			withProperty("id") { string() }
			withProperty("tiltakskode", optional = true) { string() }
			withProperty("tiltakstype", optional = true) { tiltakstypeSchema() }
			withProperty("navn") { string() }
			withProperty("startDato") { string() } // ISO-8601 format
			withProperty("sluttDato", optional = true) { string() } // ISO-8601 format
			withProperty("status") { string() }
			withProperty("oppstart") { string() }
			withProperty("tilgjengeligForArrangorFraOgMedDato") { string() } // ISO-8601 format
			withProperty("arrangor") { arrangorSchema() }
			additionalProperties = false
		}
	}
}
