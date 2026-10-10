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
    val merchant: String,
    val accountLast4: String?,
    val occurredAt: Long,
    val categoryId: Long?,
    val note: String?,
    /** Where it came from: SMS, NOTIFICATION or MANUAL. */
    val source: String,
    /** Stable hash of the source message, so the same message is never stored twice. */
    val fingerprint: String,
)

@Entity(tableName = "categories", indices = [Index(value = ["name"], unique = true)])
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)
