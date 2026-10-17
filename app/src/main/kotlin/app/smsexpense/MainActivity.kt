package app.smsexpense

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import app.smsexpense.core.Entry
import app.smsexpense.core.MonthlySummary
import app.smsexpense.ui.HomeScreen
import app.smsexpense.ui.PermissionScreen
import app.smsexpense.ui.theme.ExpenseTheme
import java.time.YearMonth
import java.time.ZoneId

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as ExpenseApp
        setContent {
            ExpenseTheme {
                Surface(Modifier.fillMaxSize()) {
                    var granted by remember { mutableStateOf(hasSmsPermission()) }
                    var denied by remember { mutableStateOf(false) }
                    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
                        granted = result[Manifest.permission.RECEIVE_SMS] == true
                        denied = !granted
                    }
                    if (!granted) {
                        PermissionScreen(
                            deniedBefore = denied,
                            onAllow = { ask.launch(arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.POST_NOTIFICATIONS)) },
                            onOpenSettings = {
                                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
                            },
                        )
                    } else {
                        Home(app)
                    }
                }
            }
        }
    }

    private fun hasSmsPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
}

@Composable
private fun Home(app: ExpenseApp) {
    val zone = remember { ZoneId.systemDefault() }
    val thisMonth = remember { YearMonth.now(zone) }
    var month by rememberSaveable { mutableStateOf(thisMonth) }
    val transactions by app.db.transactions().observeAll().collectAsState(initial = emptyList())
    val categories by app.db.categories().observeAll().collectAsState(initial = emptyList())
    val byId = categories.associateBy { it.id }
    val entries = transactions.map { Entry(it.direction, it.amount, it.currency, it.categoryId?.let(byId::get)?.name, it.occurredAt) }

    HomeScreen(
        month = month,
        summaries = MonthlySummary.of(entries, month, zone),
        transactions = transactions.filter { MonthlySummary.monthOf(it.occurredAt, zone) == month },
        categories = byId,
        onPreviousMonth = { month = month.minusMonths(1) },
        onNextMonth = if (month < thisMonth) ({ month = month.plusMonths(1) }) else null,
        onOpen = { app.startActivity(CategorySheetActivity.intent(app, it.id)) },
    )
}
