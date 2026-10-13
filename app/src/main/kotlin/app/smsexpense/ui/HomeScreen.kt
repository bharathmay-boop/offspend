package app.smsexpense.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import app.smsexpense.core.CategoryTotal
import app.smsexpense.ui.theme.reducedMotion
import java.math.BigDecimal
import java.math.RoundingMode
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.smsexpense.core.CurrencyMonth
import app.smsexpense.core.Direction
import app.smsexpense.data.CategoryEntity
import app.smsexpense.data.TransactionEntity
import app.smsexpense.ui.components.AmountText
import app.smsexpense.ui.components.CategoryIcon
import app.smsexpense.ui.components.CategoryIconSize
import app.smsexpense.ui.components.PrivacyNote
import app.smsexpense.ui.components.TransactionRow
import app.smsexpense.ui.components.categoryColor
import app.smsexpense.ui.components.dayLabel
import app.smsexpense.ui.components.displayMerchant
import app.smsexpense.ui.components.formatMoney
import app.smsexpense.ui.theme.AppTheme
import app.smsexpense.ui.theme.Motion
import app.smsexpense.ui.theme.Spacing
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
    today: LocalDate = LocalDate.now(),
) {
    val zone = ZoneId.systemDefault()
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
        items(summaries, key = { it.currency }) { s -> MonthCard(s, slotOf) }
        // One header per day instead of a date on every row. The list arrives newest first, so groups keep that order.
        transactions.groupBy { Instant.ofEpochMilli(it.occurredAt).atZone(zone).toLocalDate() }.forEach { (day, onDay) ->
            item(key = "day-$day") {
                Text(
                    dayLabel(day, today),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.animateItem().padding(start = Spacing.l, top = Spacing.xl, bottom = Spacing.xs).semantics { heading() },
                )
            }
            items(onDay, key = { it.id }) { tx ->
                val category = tx.categoryId?.let(categories::get)
                TransactionRow(
                    merchant = tx.merchant?.let(::displayMerchant) ?: tx.accountLast4?.let { "Account ··$it" } ?: "Unknown payee",
                    category = category?.name,
                    categorySlot = category?.slot ?: -1,
                    amount = tx.amount,
                    currency = tx.currency,
                    direction = tx.direction,
                    modifier = Modifier.animateItem().clickable { onOpen(tx) },
                )
            }
        }
    }
}

/** The categories shown as chips on the month card, and the rest added up as "Other". */
data class Glance(val shown: List<CategoryTotal>, val other: BigDecimal)

/** The top [top] categories, and the rest as one "Other" amount. Grouping a single category into Other hides it for no gain, so that never happens. */
fun glance(totals: List<CategoryTotal>, top: Int = 4): Glance =
    if (totals.size <= top + 1) Glance(totals, BigDecimal.ZERO)
    else Glance(totals.take(top), totals.drop(top).fold(BigDecimal.ZERO) { sum, t -> sum + t.amount })

/** One currency's month at a glance: the total, one bar split by category, and the top categories. The full list is one tap away. */
@Composable
fun MonthCard(s: CurrencyMonth, slotOf: Map<String, Int>) {
    var expanded by rememberSaveable(s.currency) { mutableStateOf(false) }
    val g = glance(s.spentByCategory)
    val slot = { c: CategoryTotal -> c.category?.let { slotOf[it] } ?: -1 }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.l, vertical = Spacing.s),
    ) {
        Column(Modifier.animateContentSize(Motion.default()).padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            Column {
                Text("Spent", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    formatMoney(countingAmount(s.spent), s.currency),
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.semantics { contentDescription = "Spent ${formatMoney(s.spent, s.currency)} this month" },
                )
                if (s.received.signum() > 0) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.CenterVertically) {
                        Text("Received", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        AmountText(s.received, s.currency, Direction.CREDIT)
                    }
                }
            }
            if (s.spentByCategory.isEmpty()) return@Column
            SpendingBar(s.spentByCategory, slot, expanded, onToggle = { expanded = !expanded })
            if (!expanded) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    g.shown.forEach { c ->
                        CategoryPill(c.category ?: "Needs a category", formatMoney(c.amount, s.currency, whole = true), onClick = { expanded = true }) {
                            CategoryIcon(c.category, slot(c), size = CategoryIconSize.small)
                        }
                    }
                    if (g.other.signum() > 0) {
                        CategoryPill("Other", formatMoney(g.other, s.currency, whole = true), onClick = { expanded = true }, showName = true) {}
                    }
                }
            }
            AnimatedVisibility(expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                    val largest = s.spentByCategory.first().amount
                    s.spentByCategory.forEach { c ->
                        val share = if (largest.signum() == 0) 0f else c.amount.toFloat() / largest.toFloat()
                        CategoryBar(c.category, slot(c), share, formatMoney(c.amount, s.currency))
                    }
                    TextButton(onClick = { expanded = false }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Show less") }
                }
            }
        }
    }
}

/** The whole month's spending in one bar, split by category. Grows in from the left the first time it shows. */
@Composable
private fun SpendingBar(totals: List<CategoryTotal>, slot: (CategoryTotal) -> Int, expanded: Boolean, onToggle: () -> Unit) {
    val reduce = reducedMotion()
    val grow = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, Motion.default()) }
    val sum = totals.fold(BigDecimal.ZERO) { a, t -> a + t.amount }.toFloat().coerceAtLeast(1f)
    Box(
        // ponytail: the bar is a small tap target; the pills below do the same thing at full size for touch and TalkBack.
        Modifier.fillMaxWidth().padding(vertical = Spacing.xs)
            .clickable(onClickLabel = if (expanded) "Show fewer categories" else "Show all categories", onClick = onToggle),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            Modifier.fillMaxWidth(grow.value.coerceIn(0.001f, 1f)).height(Spacing.m).clip(CircleShape),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
        ) {
            totals.forEach { t ->
                val w = t.amount.toFloat() / sum
                if (w > 0f) Box(Modifier.weight(w).fillMaxHeight().background(if (t.category == null) MaterialTheme.colorScheme.tertiary else categoryColor(slot(t))))
            }
        }
    }
}

/** A category's share as a small pill: its icon and amount. The name is read by TalkBack, and shown when there is no icon. */
@Composable
private fun CategoryPill(name: String, amount: String, onClick: () -> Unit, showName: Boolean = false, icon: @Composable () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.minimumInteractiveComponentSize().semantics(mergeDescendants = true) { contentDescription = "$name $amount" },
    ) {
        Row(
            Modifier.padding(start = Spacing.xs, end = Spacing.m, top = Spacing.xs, bottom = Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Text(if (showName) "$name $amount" else amount, style = AppTheme.type.amountRow)
        }
    }
}

/** Counts from the last shown value to [target] instead of jumping, so a change gets noticed. Instant when animations are off. */
@Composable
private fun countingAmount(target: BigDecimal): BigDecimal {
    val reduce = reducedMotion()
    val progress = remember { Animatable(1f) }
    var from by remember { mutableStateOf(BigDecimal.ZERO) }
    var to by remember { mutableStateOf(if (reduce) target else BigDecimal.ZERO) }
    val current = from + (to - from).multiply(BigDecimal(progress.value.toDouble()))
    LaunchedEffect(target) {
        if (reduce) {
            from = target
            to = target
            return@LaunchedEffect
        }
        from = current
        to = target
        progress.snapTo(0f)
        progress.animateTo(1f, Motion.count())
    }
    return current.setScale(2, RoundingMode.HALF_UP)
}

@Composable
private fun CategoryBar(category: String?, slot: Int, share: Float, amount: String) {
    val width by animateFloatAsState(share.coerceIn(0.02f, 1f), animationSpec = Motion.default(), label = "bar")
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs), modifier = Modifier.semantics(mergeDescendants = true) {}) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            CategoryIcon(category, slot, size = CategoryIconSize.small)
            Text(category ?: "Needs a category", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(amount, style = AppTheme.type.amountRow)
        }
        Box(Modifier.fillMaxWidth().height(6.dp).background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)) {
            Box(Modifier.fillMaxWidth(width).fillMaxHeight().background(if (category == null) MaterialTheme.colorScheme.tertiary else categoryColor(slot), CircleShape))
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
