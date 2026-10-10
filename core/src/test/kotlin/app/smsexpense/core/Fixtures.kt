package app.smsexpense.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Expected(
    val is_transaction: Boolean,
    val reason: String? = null,
    val direction: String? = null,
    val amount: String? = null,
    val currency: String? = null,
    val merchant: String? = null,
    val account: String? = null,
)

@Serializable
data class Fixture(val id: String, val source: String, val sender: String, val text: String, val expected: Expected)

object Fixtures {
    val all: List<Fixture> by lazy {
        val raw = Fixtures::class.java.getResource("/sms-fixtures.json")!!.readText()
        Json.decodeFromString(raw)
    }
}
