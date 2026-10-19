package app.smsexpense

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import app.smsexpense.capture.TransactionNotifier
import app.smsexpense.data.Ledger
import app.smsexpense.data.TransactionEntity
import app.smsexpense.ui.CategorySheet
import app.smsexpense.ui.Provenance
import app.smsexpense.ui.SourceLine
import app.smsexpense.ui.theme.ExpenseTheme
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/** Opens over whatever the person is doing, from the notification or the transaction list. */
class CategorySheetActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        // The pre-Material translucent theme doesn't set this, so Android painted its own black status bar (#73).
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as ExpenseApp
        // One payment, or a run of them to review one after another (#62).
        val reviewing = intent.hasExtra(EXTRA_QUEUE)
        val queue = intent.getLongArrayExtra(EXTRA_QUEUE)?.toList() ?: listOf(intent.getLongExtra(TransactionNotifier.EXTRA_TX, -1))
        queue.forEach { TransactionNotifier.dismiss(this, it) }

        setContent {
            ExpenseTheme {
                var index by rememberSaveable { mutableIntStateOf(0) }
                var tx by remember { mutableStateOf<TransactionEntity?>(null) }
                var knownPayee by remember { mutableStateOf(false) }
                var sources by remember { mutableStateOf(emptyList<SourceLine>()) }
                var mostUsed by remember { mutableStateOf(emptyList<Long>()) }
                LaunchedEffect(Unit) { mostUsed = app.db.transactions().mostUsedCategories() }
                LaunchedEffect(index) {
                    val loaded = app.db.transactions().byId(queue[index]) ?: run { finish(); return@LaunchedEffect }
                    knownPayee = app.ledger.knowsPayee(loaded.merchant)
                    sources = Provenance.lines(loaded.source, app.db.sightings().forTransaction(loaded.id), appName = ::appName)
                    tx = loaded
                }
                val categories by app.db.categories().observeAll().collectAsState(initial = emptyList())
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                val scope = rememberCoroutineScope()
                val current = tx ?: return@ExpenseTheme

                ModalBottomSheet(onDismissRequest = { finish() }, sheetState = sheetState) {
                    // A fresh sheet per payment, so nothing picked or typed carries over to the next one.
                    key(current.id) {
                        CategorySheet(
                            amount = current.amount,
                            currency = current.currency,
                            direction = current.direction,
                            merchant = current.merchant,
                            detail = listOfNotNull(
                                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(current.occurredAt)),
                                current.accountLast4?.let { "Account ··$it" },
                            ).joinToString(" · "),
                            categories = categories,
                            initialCategoryId = current.categoryId,
                            initialNote = current.note,
                            knownPayee = knownPayee,
                            mostUsed = mostUsed,
                            sources = sources,
                            handAdded = current.source == Ledger.MANUAL,
                            progress = if (queue.size > 1) "${index + 1} of ${queue.size}" else null,
                            // Skip leaves the payment as it is: no category, no note, nothing learned.
                            onSkip = if (reviewing) ({
                                if (index < queue.lastIndex) {
                                    index++
                                } else {
                                    scope.launch {
                                        sheetState.hide()
                                        finish()
                                    }
                                }
                            }) else null,
                            onEdit = {
                                startActivity(AddPaymentActivity.editIntent(this@CategorySheetActivity, current.id))
                                finish()
                            },
                            onDelete = {
                                scope.launch {
                                    app.ledger.delete(current.id)
                                    app.justDeleted.value = current.id
                                    if (index < queue.lastIndex) {
                                        index++
                                    } else {
                                        sheetState.hide()
                                        finish()
                                    }
                                }
                            },
                            onSave = { result ->
                                scope.launch {
                                    app.ledger.file(current.id, result.categoryId, result.newCategoryName, result.note, result.newCategoryIcon, result.alwaysUse)
                                    if (index < queue.lastIndex) {
                                        index++
                                    } else {
                                        sheetState.hide()
                                        finish()
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    // Android 11+ hides other apps' names unless declared, so a known list in Provenance is the fallback.
    private fun appName(pkg: String): String? =
        runCatching { packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString() }.getOrNull()

    companion object {
        private const val EXTRA_QUEUE = "app.smsexpense.QUEUE"

        /** Files these payments one after another, with a "2 of 3" counter. */
        fun reviewIntent(context: Context, txIds: LongArray): Intent =
            Intent(context, CategorySheetActivity::class.java)
                .putExtra(EXTRA_QUEUE, txIds)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)

        fun intent(context: Context, txId: Long): Intent =
            Intent(context, CategorySheetActivity::class.java)
                .putExtra(TransactionNotifier.EXTRA_TX, txId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
    }
}
