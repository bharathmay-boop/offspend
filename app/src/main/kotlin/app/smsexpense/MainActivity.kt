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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import app.smsexpense.ui.PermissionScreen
import app.smsexpense.ui.components.TransactionRow
import app.smsexpense.ui.theme.ExpenseTheme
import app.smsexpense.ui.theme.Spacing
import java.text.DateFormat
import java.util.Date

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
                        RecentTransactions(app)
                    }
                }
            }
        }
    }

    private fun hasSmsPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
}

// ponytail: plain list until the monthly view (#13) replaces it
@Composable
private fun RecentTransactions(app: ExpenseApp) {
    val transactions by app.db.transactions().observeAll().collectAsState(initial = emptyList())
    val categories by app.db.categories().observeAll().collectAsState(initial = emptyList())
    val byId = categories.associateBy { it.id }
    Column(Modifier.safeDrawingPadding()) {
        Text("Recent", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(Spacing.l))
        if (transactions.isEmpty()) {
            Text(
                "Payments will appear here as your bank texts you.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.l),
            )
        }
        LazyColumn {
            items(transactions, key = { it.id }) { tx ->
                val category = tx.categoryId?.let(byId::get)
                TransactionRow(
                    merchant = tx.merchant ?: "Unknown payee",
                    category = category?.name,
                    categorySlot = category?.slot ?: -1,
                    date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(tx.occurredAt)),
                    amount = tx.amount,
                    currency = tx.currency,
                    direction = tx.direction,
                )
            }
        }
    }
}
