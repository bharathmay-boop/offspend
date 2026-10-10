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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import app.smsexpense.capture.TransactionNotifier
import app.smsexpense.data.TransactionEntity
import app.smsexpense.ui.CategorySheet
import app.smsexpense.ui.theme.ExpenseTheme
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/** Opens over whatever the person is doing, from the notification or the transaction list. */
class CategorySheetActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as ExpenseApp
        val txId = intent.getLongExtra(TransactionNotifier.EXTRA_TX, -1)
        TransactionNotifier.dismiss(this, txId)

        setContent {
            ExpenseTheme {
                var tx by remember { mutableStateOf<TransactionEntity?>(null) }
                LaunchedEffect(txId) { tx = app.db.transactions().byId(txId) ?: run { finish(); null } }
                val categories by app.db.categories().observeAll().collectAsState(initial = emptyList())
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                val scope = rememberCoroutineScope()
                val current = tx ?: return@ExpenseTheme

                ModalBottomSheet(onDismissRequest = { finish() }, sheetState = sheetState) {
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
                        onCancel = { finish() },
                        onSave = { result ->
                            scope.launch {
                                app.ledger.file(current.id, result.categoryId, result.newCategoryName, result.note)
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
        fun intent(context: Context, txId: Long): Intent =
            Intent(context, CategorySheetActivity::class.java)
                .putExtra(TransactionNotifier.EXTRA_TX, txId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
    }
}
