package app.smsexpense.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import app.smsexpense.core.Direction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    /** Returns -1 when a transaction with the same fingerprint already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tx: TransactionEntity): Long

    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun byId(id: Long): TransactionEntity?

    @Query("UPDATE transactions SET categoryId = :categoryId, confirmed = 1 WHERE id = :id")
    suspend fun setCategory(id: Long, categoryId: Long)

    @Query(
        "SELECT * FROM transactions WHERE direction = :direction AND currency = :currency " +
            "AND source != :source AND occurredAt BETWEEN :from AND :to",
    )
    suspend fun fromOtherSourcesNear(direction: Direction, currency: String, source: String, from: Long, to: Long): List<TransactionEntity>

    @Query("UPDATE transactions SET merchant = :merchant WHERE id = :id AND merchant IS NULL")
    suspend fun fillMerchant(id: Long, merchant: String)

    /** Payees seen before, most recent first, for suggestions when adding by hand. */
    @Query("SELECT merchant FROM transactions WHERE merchant IS NOT NULL GROUP BY merchant ORDER BY MAX(occurredAt) DESC LIMIT 200")
    suspend fun recentPayees(): List<String>

    /** Currencies of payments since a time, newest first, to pick the default for adding by hand. */
    @Query("SELECT currency FROM transactions WHERE occurredAt >= :since ORDER BY occurredAt DESC")
    suspend fun currenciesSince(since: Long): List<String>

    @Query("UPDATE transactions SET note = :note WHERE id = :id")
    suspend fun setNote(id: Long, note: String?)
}

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: CategoryEntity): Long

    @Query("SELECT * FROM categories ORDER BY slot")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY slot")
    suspend fun all(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE name = :name COLLATE NOCASE")
    suspend fun byName(name: String): CategoryEntity?

    @Query("SELECT COALESCE(MAX(slot), -1) + 1 FROM categories")
    suspend fun nextSlot(): Int
}

@Dao
interface MerchantRuleDao {
    @Upsert
    suspend fun upsert(rule: MerchantRuleEntity)

    @Query("SELECT * FROM merchant_rules")
    suspend fun all(): List<MerchantRuleEntity>
}
