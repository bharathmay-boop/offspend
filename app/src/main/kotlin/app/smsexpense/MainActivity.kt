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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.smsexpense.capture.PaymentNotificationListener
import app.smsexpense.core.Entry
import app.smsexpense.core.MonthlySummary
import app.smsexpense.ui.AppNotificationsOffer
import app.smsexpense.ui.HomeScreen
import app.smsexpense.ui.PermissionScreen
import app.smsexpense.ui.theme.ExpenseTheme
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId

class MainActivity : ComponentActivity() {
    override fun onStart() {
        super.onStart()
        val app = application as ExpenseApp
        if (canReadInbox(app)) app.scope.launch { app.catchUp() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as ExpenseApp
        if (savedInstanceState == null) openAddIfAsked(intent)
        setContent {
            ExpenseTheme {
                Surface(Modifier.fillMaxSize()) {
                    var granted by remember { mutableStateOf(hasSmsPermission()) }
                    var denied by remember { mutableStateOf(false) }
                    // SMS may have been turned on in Settings while the app was away (#151). Home starts the import.
                    LifecycleResumeEffect(Unit) {
                        granted = hasSmsPermission()
                        onPauseOrDispose {}
                    }
                    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
                        granted = result[Manifest.permission.RECEIVE_SMS] == true
                        if (result[Manifest.permission.READ_SMS] == true) app.importHistoryOnce()
                        denied = !granted
                    }
                    if (!granted) {
                        PermissionScreen(
                            deniedBefore = denied,
                            onAllow = { ask.launch(arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS, Manifest.permission.POST_NOTIFICATIONS)) },
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openAddIfAsked(intent)
    }

    /** The "Add payment" shortcut on the app icon lands here (#54). */
    private fun openAddIfAsked(intent: Intent?) {
        if (intent?.action == ACTION_ADD) startActivity(AddPaymentActivity.intent(this))
    }

    companion object {
        const val ACTION_ADD = "app.smsexpense.ADD"
    }

    private fun hasSmsPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
}

@Composable
private fun Home(app: ExpenseApp) {
    val zone = remember { ZoneId.systemDefault() }
    var thisMonth by remember { mutableStateOf(YearMonth.now(zone)) }
    var month by rememberSaveable { mutableStateOf(thisMonth) }
    val transactions by app.db.transactions().observeAll().collectAsState(initial = emptyList())
    val categories by app.db.categories().observeAll().collectAsState(initial = emptyList())
    val history by app.history.collectAsState()
    val justDeleted by app.justDeleted.collectAsState()
    LaunchedEffect(Unit) { if (canReadInbox(app)) app.importHistoryOnce() }
    val prefs = remember { app.getSharedPreferences("ui", android.content.Context.MODE_PRIVATE) }
    var listenerOn by remember { mutableStateOf(PaymentNotificationListener.isEnabled(app)) }
    var importNoticeShown by remember { mutableStateOf(prefs.getBoolean("import_notice_shown", false)) }
    var offerDismissed by remember { mutableStateOf(prefs.getBoolean("app_notifications_offer_dismissed", false)) }
    // Coming back from Settings is a resume, so check again then.
    LifecycleResumeEffect(Unit) {
        listenerOn = PaymentNotificationListener.isEnabled(app)
        // The app may have stayed open into a new month.
        val now = YearMonth.now(zone)
        month = followCurrentMonth(selected = month, oldCurrent = thisMonth, newCurrent = now)
        thisMonth = now
        onPauseOrDispose {}
    }
    val byId = categories.associateBy { it.id }
    val entries = transactions.map { Entry(it.direction, it.amount, it.currency, it.categoryId?.let(byId::get)?.name, it.occurredAt) }

    HomeScreen(
        month = month,
        summaries = MonthlySummary.of(entries, month, zone),
        transactions = transactions.filter { MonthlySummary.monthOf(it.occurredAt, zone) == month },
        categories = byId,
        importing = history == ExpenseApp.History.Reading,
        notice = when (val h = history) {
            // Said once, then gone: it is news the first time and clutter after that.
            is ExpenseApp.History.Done -> if (h.found > 0 && !importNoticeShown) "Filed ${h.found} payments from the last 3 months." else null
            ExpenseApp.History.Failed -> "Couldn't read past messages this time. The app will try again next time it opens."
            else -> null
        },
        onNoticeShown = {
            if (history is ExpenseApp.History.Done) {
                prefs.edit().putBoolean("import_notice_shown", true).apply()
                importNoticeShown = true
            }
        },
        appNotificationsOffer = if (listenerOn || offerDismissed) null else AppNotificationsOffer(
            onTurnOn = {
                app.startActivity(PaymentNotificationListener.settingsIntent(app).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            },
            onNotNow = {
                prefs.edit().putBoolean("app_notifications_offer_dismissed", true).apply()
                offerDismissed = true
            },
        ),
        onPreviousMonth = { month = month.minusMonths(1) },
        onNextMonth = if (month < thisMonth) ({ month = month.plusMonths(1) }) else null,
        onOpen = { app.startActivity(CategorySheetActivity.intent(app, it.id)) },
        onReview = { app.startActivity(CategorySheetActivity.reviewIntent(app, it.toLongArray())) },
        onAdd = { app.startActivity(AddPaymentActivity.intent(app)) },
        onSettings = { app.startActivity(SettingsActivity.intent(app).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) },
        justDeleted = justDeleted,
        onUndoDelete = { id -> app.scope.launch { app.ledger.restore(id) } },
        onDeletedShown = { app.justDeleted.value = null },
    )
}

private fun canReadInbox(app: ExpenseApp) =
    ContextCompat.checkSelfPermission(app, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED

/** If the person was looking at the current month when the month changed, move them to the new one. */
internal fun followCurrentMonth(selected: YearMonth, oldCurrent: YearMonth, newCurrent: YearMonth): YearMonth =
    if (selected == oldCurrent) newCurrent else selected
