package app.smsexpense.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
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
    /** Deleted by the person. Kept, not removed, so the same message is never filed again by a catch-up. */
    @ColumnInfo(defaultValue = "0") val deleted: Boolean = false,
)

@Entity(tableName = "categories", indices = [Index(value = ["name"], unique = true)])
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Which category color this uses (see DESIGN.md). Assigned in order, never reused. */
    val slot: Int,
    /** An icon the person picked for a category they made (a key from `PickableIcons`), or null. */
    val icon: String? = null,
)

/** What the person taught the app: this merchant goes in this category. */
@Entity(tableName = "merchant_rules")
data class MerchantRuleEntity(
    @PrimaryKey val merchantKey: String,
    val categoryId: Long,
)

/**
 * One source reporting a payment (#169): the bank's text, a payment app's alert, or the person adding it by hand. A
 * payment seen by two sources has two. Never holds message text.
 */
@Entity(
    tableName = "sightings",
    foreignKeys = [ForeignKey(entity = TransactionEntity::class, parentColumns = ["id"], childColumns = ["transactionId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("transactionId"), Index(value = ["fingerprint"], unique = true)],
)
data class SightingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: Long,
    /** SMS, NOTIFICATION or MANUAL. */
    val source: String,
    /** The bank's sender ID (VM-HDFCBK) or the payment app's package name. Null when added by hand or from before v4. */
    val sender: String?,
    /** The payee as this source wrote it. */
    val merchant: String?,
    val accountLast4: String?,
    val seenAt: Long,
    /** The message's fingerprint, so the same alert seen twice is one sighting. Null when added by hand or from before v4. */
    val fingerprint: String?,
)
