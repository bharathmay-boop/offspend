package app.smsexpense.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import app.smsexpense.R
import app.smsexpense.ui.theme.Haptics
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
    /** True while the first 3 month import runs: placeholder rows instead of "no payments". */
    importing: Boolean,
    /** A one-time message, such as how many payments the first import filed. Shown as a snackbar. */
    notice: String?,
    onNoticeShown: () -> Unit,
    /** Shown until payment app notifications are turned on or the offer is dismissed. */
    appNotificationsOffer: AppNotificationsOffer?,
    onPreviousMonth: () -> Unit,
    onNextMonth: (() -> Unit)?,
    onOpen: (TransactionEntity) -> Unit,
    /** Files these payments one after another. */
    onReview: (List<Long>) -> Unit,
    today: LocalDate = LocalDate.now(),
) {
    val zone = ZoneId.systemDefault()
    val slotOf = categories.values.associate { it.name to it.slot }
    val iconOf = categories.values.associate { it.name to it.icon }
    val haptics = LocalHapticFeedback.current
    val reduce = reducedMotion()
    val previous = { haptics.performHapticFeedback(Haptics.tick); onPreviousMonth() }
    val next = onNextMonth?.let { go -> { haptics.performHapticFeedback(Haptics.tick); go() } }
    // The swipe handler outlives recompositions, so it reads the latest callbacks rather than the first ones.
    val swipeBack by rememberUpdatedState(previous)
    val swipeForward by rememberUpdatedState(next)
    // The old month keeps its own data while it slides out, so the outgoing screen never shows the new month's numbers.
    val view = MonthView(month, summaries, transactions)

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(notice) {
        if (notice == null) return@LaunchedEffect
        snackbar.showSnackbar(notice)
        // Only after it has been on screen: marking it shown clears the notice, which would cancel this effect.
        onNoticeShown()
    }

    Box(Modifier.safeDrawingPadding()) {
        Column {
            Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.xs, vertical = Spacing.s), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = previous) {
                    Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = "Previous month")
                }
                AnimatedContent(
                    month,
                    transitionSpec = { monthSlide(initialState, targetState, reduce) },
                    modifier = Modifier.weight(1f),
                    label = "month title",
                ) { m ->
                    Text(
                        m.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().semantics { heading() },
                    )
                }
                IconButton(onClick = { next?.invoke() }, enabled = next != null) {
                    Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = "Next month")
                }
            }
            AnimatedContent(
                view,
                transitionSpec = { monthSlide(initialState.month, targetState.month, reduce) },
                contentKey = { it.month },
                label = "month",
                modifier = Modifier.fillMaxSize().testTag("month").pointerInput(Unit) {
                    var dx = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dx = 0f },
                        onHorizontalDrag = { _, amount -> dx += amount },
                        onDragEnd = {
                            when (swipeStep(dx, threshold = Spacing.xxxl.toPx())) {
                                -1 -> swipeBack()
                                1 -> swipeForward?.invoke()
                            }
                        },
                    )
                },
            ) { v -> MonthList(v, categories, slotOf, iconOf, importing, appNotificationsOffer, onOpen, onReview, today, zone) }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

/** What one month's page shows. Passed whole to the slide, so the outgoing page keeps its own month's data. */
private data class MonthView(val month: YearMonth, val summaries: List<CurrencyMonth>, val transactions: List<TransactionEntity>)

/** A horizontal swipe longer than [threshold]: right goes back a month (-1), left goes forward (+1), shorter does nothing. */
fun swipeStep(dragX: Float, threshold: Float): Int = when {
    dragX > threshold -> -1
    dragX < -threshold -> 1
    else -> 0
}

/** A later month comes in from the right, an earlier one from the left, like turning pages. Nothing moves when animations are off. */
private fun AnimatedContentTransitionScope<*>.monthSlide(from: YearMonth, to: YearMonth, reduce: Boolean): ContentTransform {
    if (reduce) return EnterTransition.None togetherWith ExitTransition.None
    val dir = if (to > from) 1 else -1
    return (slideInHorizontally(Motion.default()) { it / 4 * dir } + fadeIn(Motion.effects())) togetherWith
        (slideOutHorizontally(Motion.default()) { -it / 4 * dir } + fadeOut(Motion.effects()))
}

@Composable
private fun MonthList(
    v: MonthView,
    categories: Map<Long, CategoryEntity>,
    slotOf: Map<String, Int>,
    iconOf: Map<String, String?>,
    importing: Boolean,
    appNotificationsOffer: AppNotificationsOffer?,
    onOpen: (TransactionEntity) -> Unit,
    onReview: (List<Long>) -> Unit,
    today: LocalDate,
    zone: ZoneId,
) {
    val unfiled = v.transactions.filter { it.categoryId == null }.map { it.id }
    LazyColumn(Modifier.fillMaxSize()) {
        if (appNotificationsOffer != null) item { AppNotificationsCard(appNotificationsOffer) }
        if (v.summaries.isEmpty() && importing) {
            item { ImportPlaceholder() }
        } else if (v.summaries.isEmpty()) {
            item {
                Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                    Text(
                        if (v.month == YearMonth.from(today)) "No payments this month yet"
                        else "No payments in ${v.month.format(DateTimeFormatter.ofPattern("MMMM"))}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        "When your bank texts you about a payment, it shows up here and you get a notification to file it.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    PrivacyNote()
                }
            }
        }
        items(v.summaries, key = { it.currency }) { s -> MonthCard(s, slotOf, iconOf) }
        if (unfiled.isNotEmpty()) item(key = "review") { ReviewPrompt(unfiled.size, onReview = { onReview(unfiled) }, Modifier.animateItem()) }
        // One header per day instead of a date on every row. The list arrives newest first, so groups keep that order.
        v.transactions.groupBy { Instant.ofEpochMilli(it.occurredAt).atZone(zone).toLocalDate() }.forEach { (day, onDay) ->
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
                    categoryIcon = category?.icon,
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
fun MonthCard(s: CurrencyMonth, slotOf: Map<String, Int>, iconOf: Map<String, String?> = emptyMap()) {
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
                            CategoryIcon(c.category, slot(c), size = CategoryIconSize.small, icon = c.category?.let(iconOf::get))
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
                        CategoryBar(c.category, slot(c), share, formatMoney(c.amount, s.currency), c.category?.let(iconOf::get))
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
private fun CategoryBar(category: String?, slot: Int, share: Float, amount: String, icon: String? = null) {
    val width by animateFloatAsState(share.coerceIn(0.02f, 1f), animationSpec = Motion.default(), label = "bar")
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs), modifier = Modifier.semantics(mergeDescendants = true) {}) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            CategoryIcon(category, slot, size = CategoryIconSize.small, icon = icon)
            Text(category ?: "Needs a category", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(amount, style = AppTheme.type.amountRow)
        }
        Box(Modifier.fillMaxWidth().height(6.dp).background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)) {
            Box(Modifier.fillMaxWidth(width).fillMaxHeight().background(if (category == null) MaterialTheme.colorScheme.tertiary else categoryColor(slot), CircleShape))
        }
    }
}

/** Rows in the shape of the list while the first import runs, so the screen never claims there are no payments. Static: no shimmer. */
@Composable
private fun ImportPlaceholder() {
    val block = MaterialTheme.colorScheme.surfaceContainerHigh
    Column(Modifier.padding(Spacing.l).testTag("placeholder"), verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
        Text("Reading your last 3 months of bank messages…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        repeat(4) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(CategoryIconSize.row).background(block, CircleShape))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    Box(Modifier.fillMaxWidth(0.5f).height(Spacing.l).background(block, MaterialTheme.shapes.extraSmall))
                    Box(Modifier.fillMaxWidth(0.3f).height(Spacing.m).background(block, MaterialTheme.shapes.extraSmall))
                }
            }
        }
    }
}

/** "3 payments need a category", with one button that files them in a row. */
@Composable
private fun ReviewPrompt(count: Int, onReview: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(start = Spacing.l, end = Spacing.s, top = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        CategoryIcon(null, -1, size = CategoryIconSize.small)
        Text(
            if (count == 1) "1 payment needs a category" else "$count payments need a category",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        FilledTonalButton(onClick = onReview, modifier = Modifier.heightIn(min = 48.dp)) { Text("Review") }
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
                "Small UPI payments often skip the SMS.",
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
