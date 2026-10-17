package app.smsexpense.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Every schema change gets a migration, so nobody's history is lost on an update.
@Database(
    entities = [TransactionEntity::class, CategoryEntity::class, MerchantRuleEntity::class, SightingEntity::class],
    version = 4,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactions(): TransactionDao
    abstract fun categories(): CategoryDao
    abstract fun merchantRules(): MerchantRuleDao
    abstract fun sightings(): SightingDao

    companion object {
        fun create(context: Context, name: String = "expenses.db"): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()

        /** #75: categories the person makes can have an icon. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) = db.execSQL("ALTER TABLE categories ADD COLUMN icon TEXT")
        }

        /** #52: deleted entries are kept and hidden, so a catch-up never files them again. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) = db.execSQL("ALTER TABLE transactions ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0")
        }

        /** #169: every source that reported a payment. Older payments get one, made from their own row. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(SIGHTINGS_TABLE)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sightings_transactionId` ON `sightings` (`transactionId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_sightings_fingerprint` ON `sightings` (`fingerprint`)")
                db.execSQL(
                    "INSERT INTO sightings (transactionId, source, sender, merchant, accountLast4, seenAt, fingerprint) " +
                        "SELECT id, source, NULL, merchant, accountLast4, occurredAt, NULL FROM transactions",
                )
            }
        }

        // Exactly as Room creates it, so the schema check after the migration passes.
        private const val SIGHTINGS_TABLE =
            "CREATE TABLE IF NOT EXISTS `sightings` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `transactionId` INTEGER NOT NULL, " +
                "`source` TEXT NOT NULL, `sender` TEXT, `merchant` TEXT, `accountLast4` TEXT, `seenAt` INTEGER NOT NULL, `fingerprint` TEXT, " +
                "FOREIGN KEY(`transactionId`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
    }
}
