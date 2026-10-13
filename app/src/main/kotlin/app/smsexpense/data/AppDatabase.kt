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
    entities = [TransactionEntity::class, CategoryEntity::class, MerchantRuleEntity::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactions(): TransactionDao
    abstract fun categories(): CategoryDao
    abstract fun merchantRules(): MerchantRuleDao

    companion object {
        fun create(context: Context, name: String = "expenses.db"): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(MIGRATION_1_2).build()

        /** #75: categories the person makes can have an icon. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) = db.execSQL("ALTER TABLE categories ADD COLUMN icon TEXT")
        }
    }
}
