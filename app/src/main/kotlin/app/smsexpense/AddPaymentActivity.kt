package app.smsexpense

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import app.smsexpense.core.Direction
import app.smsexpense.data.Ledger
import app.smsexpense.ui.AddResult
import app.smsexpense.ui.AddSheet
import app.smsexpense.ui.defaultCurrency
import app.smsexpense.ui.theme.ExpenseTheme
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Opens the add sheet over the home screen (#51). */
class AddPaymentActivity : ComponentActivity() {

    /** What the sheet needs from the ledger before it can show. */
    private class Ready(
        val payees: List<String>,
        val recentCurrencies: List<String>,
        val learned: Map<String, Long>,
        /** When editing (#52): the entry's current values, and whether they came from the bank. */
        val initial: AddResult?,
        val fromBank: Boolean,
    )

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as ExpenseApp
        val today = LocalDate.now()
        val editing = intent.getLongExtra(EXTRA_EDIT, -1).takeIf { it > 0 }

        setContent {
            ExpenseTheme {
                val categories by app.db.categories().observeAll().collectAsState(initial = emptyList())
                val ready by produceState<Ready?>(null) {
                    val since = System.currentTimeMillis() - THREE_MONTHS_MS
                    val payees = app.db.transactions().recentPayees()
                    // What the app would guess for each known payee, worked out once so typing stays instant.
                    // ponytail: guessed as payments only; add a second map for money received if salary-style payees matter
                    val learned = payees.mapNotNull { p -> app.ledger.guessCategory(p, Direction.DEBIT)?.let { p.lowercase() to it.id } }.toMap()
                    val tx = editing?.let { app.db.transactions().byId(it) }
                    val initial = tx?.let {
                        AddResult(
                            it.direction, it.amount, it.currency, it.merchant, it.categoryId,
                            Instant.ofEpochMilli(it.occurredAt).atZone(ZoneId.systemDefault()).toLocalDate(), it.note.orEmpty(),
                        )
                    }
                    value = Ready(payees, app.db.transactions().currenciesSince(since), learned, initial, tx != null && tx.source != Ledger.MANUAL)
                }
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                val scope = rememberCoroutineScope()
                val r = ready ?: return@ExpenseTheme

                ModalBottomSheet(onDismissRequest = { finish() }, sheetState = sheetState) {
                    AddSheet(
                        categories = categories,
                        currencies = r.recentCurrencies.distinct(),
                        defaultCurrency = defaultCurrency(r.recentCurrencies),
                        payees = r.payees,
                        guess = { name, _ -> r.learned[name.trim().lowercase()] },
                        today = today,
                        initial = r.initial,
                        moneyLocked = r.fromBank,
                        onSave = { result ->
                            scope.launch {
                                if (editing != null) app.ledger.edit(editing, result) else app.ledger.addByHand(result)
                                sheetState.hide()
                                finish()
                            }
                        },
                    )
                }
            }
        }
    }

    companion object {
        private const val THREE_MONTHS_MS = 92L * 24 * 60 * 60 * 1000

        private const val EXTRA_EDIT = "app.smsexpense.EDIT"

        /** Opens the same sheet filled in with an entry, to change it (#52). */
        fun editIntent(context: Context, txId: Long): Intent = intent(context).putExtra(EXTRA_EDIT, txId)

        fun intent(context: Context): Intent =
            Intent(context, AddPaymentActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
