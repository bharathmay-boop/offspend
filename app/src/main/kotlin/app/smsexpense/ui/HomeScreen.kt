package app.smsexpense.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.smsexpense.core.CurrencyMonth
import app.smsexpense.core.Direction
import app.smsexpense.data.CategoryEntity
import app.smsexpense.data.TransactionEntity
import app.smsexpense.ui.components.AmountText
import app.smsexpense.ui.components.CategoryDot
import app.smsexpense.ui.components.PrivacyNote
import app.smsexpense.ui.components.TransactionRow
import app.smsexpense.ui.components.categoryColor
import app.smsexpense.ui.components.formatMoney
import app.smsexpense.ui.theme.AppTheme
import app.smsexpense.ui.theme.Motion
import app.smsexpense.ui.theme.Spacing
import java.text.DateFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Date

/** The main screen: one month at a time, totals per currency, spending by category, then the payments. */
@Composable
fun HomeScreen(
    month: YearMonth,
    summaries: List<CurrencyMonth>,
    transactions: List<TransactionEntity>,
    categories: Map<Long, CategoryEntity>,
    banner: String?,
    /** Shown until payment app notifications are turned on or the offer is dismissed. */
    appNotificationsOffer: AppNotificationsOffer?,
    onPreviousMonth: () -> Unit,
    onNextMonth: (() -> Unit)?,
    onOpen: (TransactionEntity) -> Unit,
) {
    val slotOf = categories.values.associate { it.name to it.slot }
    LazyColumn(Modifier.safeDrawingPadding()) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.s, vertical = Spacing.s), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onPreviousMonth) { Text("‹ Prev") }
                Text(
                    month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f).padding(horizontal = Spacing.s),
                )
                TextButton(onClick = { onNextMonth?.invoke() }, enabled = onNextMonth != null) { Text("Next ›") }
            }
        }
        if (banner != null) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.l, vertical = Spacing.s),
                ) {
                    Text(banner, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(Spacing.l))
                }
            }
        }
        if (appNotificationsOffer != null) item { AppNotificationsCard(appNotificationsOffer) }
        if (summaries.isEmpty()) {
            item {
                Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                    Text("No payments this month yet", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "When your bank texts you about a payment, it shows up here and you get a notification to file it.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    PrivacyNote()
                }
            }
        }
        items(summaries, key = { it.currency }) { s -> CurrencyBlock(s, slotOf) }
        if (transactions.isNotEmpty()) {
            item {
                Text(
                    "PAYMENTS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = Spacing.l, top = Spacing.xl, bottom = Spacing.s),
                )
            }
        }
        items(transactions, key = { it.id }) { tx ->
            val category = tx.categoryId?.let(categories::get)
            TransactionRow(
                merchant = tx.merchant ?: tx.accountLast4?.let { "Account ··$it" } ?: "Unknown payee",
                category = category?.name,
                categorySlot = category?.slot ?: -1,
                date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(tx.occurredAt)),
                amount = tx.amount,
                currency = tx.currency,
                direction = tx.direction,
                modifier = Modifier.clickable { onOpen(tx) },
            )
        }
    }
}

@Composable
private fun CurrencyBlock(s: CurrencyMonth, slotOf: Map<String, Int>) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.l, vertical = Spacing.s),
    ) {
        Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            Text("Spent", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                formatMoney(s.spent, s.currency),
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.semantics { contentDescription = "Spent ${formatMoney(s.spent, s.currency)} this month" },
            )
            if (s.received.signum() > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.CenterVertically) {
                    Text("Received", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    AmountText(s.received, s.currency, Direction.CREDIT)
                }
            }
            val largest = s.spentByCategory.firstOrNull()?.amount
            s.spentByCategory.forEach { c ->
                val slot = c.category?.let { slotOf[it] } ?: -1
                val share = if (largest == null || largest.signum() == 0) 0f else c.amount.toFloat() / largest.toFloat()
                CategoryBar(c.category ?: "Needs a category", slot, share, formatMoney(c.amount, s.currency))
            }
        }
    }
}

@Composable
private fun CategoryBar(name: String, slot: Int, share: Float, amount: String) {
    val width by animateFloatAsState(share.coerceIn(0.02f, 1f), animationSpec = Motion.default(), label = "bar")
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs), modifier = Modifier.semantics(mergeDescendants = true) {}) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            CategoryDot(slot)
            Text(name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(amount, style = AppTheme.type.amountRow)
        }
        Box(Modifier.fillMaxWidth().height(6.dp).background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)) {
            Box(Modifier.fillMaxWidth(width).fillMaxHeight().background(categoryColor(slot), CircleShape))
        }
    }
}

class AppNotificationsOffer(val onTurnOn: () -> Unit, val onNotNow: () -> Unit)

@Composable
private fun AppNotificationsCard(offer: AppNotificationsOffer) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.l, vertical = Spacing.s),
    ) {
        Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Text("Catch UPI app payments too", style = MaterialTheme.typography.titleMedium)
            Text(
                "Some payments, like small UPI ones, don't come with a bank SMS. With notification access, the app " +
                    "also reads payment notifications from Google Pay, PhonePe, Paytm and bank apps. Notifications " +
                    "from every other app are ignored, and nothing leaves this phone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                androidx.compose.material3.Button(onClick = offer.onTurnOn, modifier = Modifier.heightIn(min = 48.dp)) { Text("Turn on") }
                TextButton(onClick = offer.onNotNow, modifier = Modifier.heightIn(min = 48.dp)) { Text("Not now") }
            }
        }
    }
}
