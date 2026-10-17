package app.smsexpense.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.smsexpense.R
import app.smsexpense.core.Direction
import app.smsexpense.ui.theme.AppTheme
import app.smsexpense.ui.theme.Motion
import app.smsexpense.ui.theme.Spacing
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** Formats an amount in its own currency, e.g. ₹1,25,000.00 or AED 45.50. */
fun formatMoney(amount: BigDecimal, currency: String, locale: Locale = Locale.getDefault()): String {
    val f = NumberFormat.getCurrencyInstance(if (currency == "INR") Locale.forLanguageTag("en-IN") else locale)
    f.currency = Currency.getInstance(currency)
    return f.format(amount)
}

/** Every amount on screen goes through this: sign, currency and money color, plus a full TalkBack label. */
@Composable
fun AmountText(
    amount: BigDecimal,
    currency: String,
    direction: Direction,
    modifier: Modifier = Modifier,
    style: TextStyle = AppTheme.type.amountRow,
) {
    val formatted = formatMoney(amount, currency)
    val (sign, color, spoken) = when (direction) {
        Direction.DEBIT -> Triple("−", MaterialTheme.colorScheme.onSurface, "Paid")
        Direction.CREDIT -> Triple("+", AppTheme.colors.credit, "Received")
    }
    Text(
        text = "$sign$formatted",
        style = style,
        color = color,
        maxLines = 1,
        modifier = modifier.clearAndSetSemantics { contentDescription = "$spoken $formatted" },
    )
}

/** The color for a category slot. Slots are assigned in order and never cycled. */
@Composable
fun categoryColor(slot: Int): Color =
    AppTheme.colors.categories.getOrNull(slot) ?: AppTheme.colors.categoryFallback

/** The Material Symbol for a built-in category, or null for one the person made. */
@DrawableRes
fun categoryIconRes(name: String): Int? = when (name) {
    "Food" -> R.drawable.ic_restaurant
    "Groceries" -> R.drawable.ic_shopping_cart
    "Transport" -> R.drawable.ic_directions_bus
    "Shopping" -> R.drawable.ic_shopping_bag
    "Bills" -> R.drawable.ic_receipt_long
    "Entertainment" -> R.drawable.ic_movie
    "Health" -> R.drawable.ic_medical_services
    "Travel" -> R.drawable.ic_flight
    "EMI & loans" -> R.drawable.ic_account_balance
    "Cash" -> R.drawable.ic_payments
    "Salary" -> R.drawable.ic_work
    "Transfers" -> R.drawable.ic_sync_alt
    else -> null
}

/**
 * A category at a glance: its icon on a circle of its colour. A category the person made shows its first letter,
 * and a payment with no category shows a question mark. Decorative: the category name next to it is what TalkBack reads.
 */
@Composable
fun CategoryIcon(name: String?, slot: Int, modifier: Modifier = Modifier, size: Dp = CategoryIconSize.row) {
    val bg = if (name == null) MaterialTheme.colorScheme.tertiaryContainer else categoryColor(slot)
    val fg = if (name == null) MaterialTheme.colorScheme.onTertiaryContainer else readableOn(bg)
    Box(modifier.size(size).background(bg, CircleShape).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        val icon = name?.let(::categoryIconRes)
        if (icon != null) {
            Icon(painterResource(icon), contentDescription = null, tint = fg, modifier = Modifier.size(size * 0.55f))
        } else {
            Text(
                name?.trim()?.firstOrNull()?.uppercase() ?: "?",
                color = fg,
                style = if (size >= CategoryIconSize.row) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelMedium,
            )
        }
    }
}

object CategoryIconSize {
    /** In chips and the spending breakdown. */
    val small = 24.dp
    /** At the start of a payment row. */
    val row = 40.dp
}

/** Ink or white, whichever reads better on this colour (WCAG contrast, the two meet at about 0.18 luminance). */
private fun readableOn(bg: Color): Color = if (bg.luminance() > 0.18f) Color(0xFF171D1A) else Color.White

@Composable
fun CategoryChip(name: String, slot: Int, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val container by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        animationSpec = Motion.effects(), label = "chip",
    )
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(name) },
        leadingIcon = { CategoryIcon(name, slot, size = CategoryIconSize.small) },
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(containerColor = container, selectedContainerColor = container),
        modifier = modifier.heightIn(min = 48.dp),
    )
}

@Composable
fun TransactionRow(
    merchant: String,
    category: String?,
    categorySlot: Int,
    date: String,
    amount: BigDecimal,
    currency: String,
    direction: Direction,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = Spacing.l, vertical = Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        CategoryIcon(category, categorySlot)
        Column(Modifier.weight(1f)) {
            Text(merchant, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(category ?: "Needs a category", date).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = if (category == null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AmountText(amount, currency, direction)
    }
}

@Composable
fun PrivacyNote(modifier: Modifier = Modifier) {
    Text(
        "🔒  Your messages are read on this phone. Nothing is uploaded.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
