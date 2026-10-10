package app.smsexpense

import android.app.Application
import app.smsexpense.data.AppDatabase

class ExpenseApp : Application() {
    // ponytail: one database handle on the Application, add DI if the graph grows
    val db: AppDatabase by lazy { AppDatabase.create(this) }
}
