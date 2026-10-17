package app.smsexpense.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import app.smsexpense.core.Direction
import java.math.BigDecimal

@Entity(tableName = "transactions", indices = [Index(value = ["fingerprint"], unique = true)])
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val direction: Direction,
    val amount: BigDecimal,
    val currency: String,
    val merchant: String?,
    val accountLast4: String?,
    val occurredAt: Long,
    val categoryId: Long?,
    val note: String?,
    /** Where it came from: SMS, NOTIFICATION or MANUAL. */
    val source: String,
    /** Stable hash of the source message, so the same message is never stored twice. */
    val fingerprint: String,
    /** True once the person has confirmed or changed the category. */
    val confirmed: Boolean = false,
)

@Entity(tableName = "categories", indices = [Index(value = ["name"], unique = true)])
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Which category color this uses (see DESIGN.md). Assigned in order, never reused. */
    val slot: Int,
)

/** What the person taught the app: this merchant goes in this category. */
@Entity(tableName = "merchant_rules")
data class MerchantRuleEntity(
    @PrimaryKey val merchantKey: String,
    val categoryId: Long,
)
