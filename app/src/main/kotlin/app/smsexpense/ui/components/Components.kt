package app.smsexpense.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.semantics.semantics
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

/** Formats an amount in its own currency, e.g. ₹1,25,000.00 or AED 45.50. [whole] drops ".00" from a round amount, for glanceable totals. */
fun formatMoney(amount: BigDecimal, currency: String, locale: Locale = Locale.getDefault(), whole: Boolean = false): String {
    val f = NumberFormat.getCurrencyInstance(if (currency == "INR") Locale.forLanguageTag("en-IN") else locale)
    f.currency = Currency.getInstance(currency)
    if (whole && amount.stripTrailingZeros().scale() <= 0) f.maximumFractionDigits = 0
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

/** An icon the person can give a category they make. Same family (Material Symbols Rounded) as the built-in ones. */
data class PickableIcon(val key: String, @DrawableRes val res: Int, val label: String)

val PickableIcons = listOf(
    PickableIcon("fitness_center", R.drawable.ic_fitness_center, "Fitness"),
    PickableIcon("pets", R.drawable.ic_pets, "Pets"),
    PickableIcon("home", R.drawable.ic_home, "Home"),
    PickableIcon("child_care", R.drawable.ic_child_care, "Kids"),
    PickableIcon("redeem", R.drawable.ic_redeem, "Gifts"),
    PickableIcon("local_cafe", R.drawable.ic_local_cafe, "Coffee"),
    PickableIcon("local_gas_station", R.drawable.ic_local_gas_station, "Fuel"),
    PickableIcon("smartphone", R.drawable.ic_smartphone, "Phone"),
    PickableIcon("school", R.drawable.ic_school, "Education"),
    PickableIcon("spa", R.drawable.ic_spa, "Beauty"),
    PickableIcon("checkroom", R.drawable.ic_checkroom, "Clothes"),
    PickableIcon("sports_esports", R.drawable.ic_sports_esports, "Games"),
    PickableIcon("local_bar", R.drawable.ic_local_bar, "Drinks"),
    PickableIcon("savings", R.drawable.ic_savings, "Savings"),
    PickableIcon("volunteer_activism", R.drawable.ic_volunteer_activism, "Charity"),
    PickableIcon("celebration", R.drawable.ic_celebration, "Celebrations"),
    PickableIcon("medication", R.drawable.ic_medication, "Medicine"),
    PickableIcon("directions_car", R.drawable.ic_directions_car, "Car"),
    PickableIcon("two_wheeler", R.drawable.ic_two_wheeler, "Bike"),
    PickableIcon("bolt", R.drawable.ic_bolt, "Electricity"),
    PickableIcon("water_drop", R.drawable.ic_water_drop, "Water"),
    PickableIcon("menu_book", R.drawable.ic_menu_book, "Books"),
    PickableIcon("build", R.drawable.ic_build, "Repairs"),
    PickableIcon("content_cut", R.drawable.ic_content_cut, "Salon"),
    PickableIcon("work", R.drawable.ic_work, "Work"),
)

@DrawableRes
fun pickableIconRes(key: String): Int? = PickableIcons.firstOrNull { it.key == key }?.res

/**
 * A category at a glance: its icon on a circle of its colour. A category the person made shows its first letter,
 * and a payment with no category shows a question mark. Decorative: the category name next to it is what TalkBack reads.
 */
@Composable
fun CategoryIcon(name: String?, slot: Int, modifier: Modifier = Modifier, size: Dp = CategoryIconSize.row, icon: String? = null) {
    val bg = if (name == null) MaterialTheme.colorScheme.tertiaryContainer else categoryColor(slot)
    val fg = if (name == null) MaterialTheme.colorScheme.onTertiaryContainer else readableOn(bg)
    Box(modifier.size(size).background(bg, CircleShape).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        val res = icon?.let(::pickableIconRes) ?: name?.let(::categoryIconRes)
        if (res != null) {
            Icon(painterResource(res), contentDescription = null, tint = fg, modifier = Modifier.size(size * 0.55f))
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

/**
 * A category to pick. [filed] swaps the icon for a check mark while the sheet closes, so the tap visibly landed.
 * [suggested] marks the app's guess.
 */
@Composable
fun CategoryChip(
    name: String,
    slot: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filed: Boolean = false,
    suggested: Boolean = false,
    icon: String? = null,
) {
    val container by animateColorAsState(
        if (selected || filed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        animationSpec = Motion.effects(), label = "chip",
    )
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(name) },
        leadingIcon = {
            AnimatedContent(filed, transitionSpec = { scaleIn(Motion.fast()) + fadeIn(Motion.effects()) togetherWith fadeOut(Motion.effects()) }, label = "check") { done ->
                if (done) {
                    Box(Modifier.size(CategoryIconSize.small).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(CategoryIconSize.small * 0.7f))
                    }
                } else {
                    CategoryIcon(name, slot, size = CategoryIconSize.small, icon = icon)
                }
            }
        },
        trailingIcon = if (suggested) {
            { Text("Suggested", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else null,
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(containerColor = container, selectedContainerColor = container),
        modifier = modifier.heightIn(min = 48.dp),
    )
}

/** A payment in the list: what it is at a glance (icon), who, which category, how much. The day is in the header above. */
@Composable
fun TransactionRow(
    merchant: String,
    category: String?,
    categorySlot: Int,
    amount: BigDecimal,
    currency: String,
    direction: Direction,
    modifier: Modifier = Modifier,
    categoryIcon: String? = null,
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 72.dp).padding(horizontal = Spacing.l, vertical = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        CategoryIcon(category, categorySlot, icon = categoryIcon)
        Column(Modifier.weight(1f)) {
            Text(merchant, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                category ?: "Needs a category",
                style = MaterialTheme.typography.bodyMedium,
                color = if (category == null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AmountText(amount, currency, direction)
    }
}

/** "Today", "Yesterday", then "Tue, 6 Oct". */
fun dayLabel(day: LocalDate, today: LocalDate, locale: Locale = Locale.getDefault()): String = when (day) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    else -> day.format(DateTimeFormatter.ofPattern("EEE, d MMM", locale))
}

/**
 * Banks write payees in capitals (SWIGGY, BLUE TOKAI). Shown as people write them: Swiggy, Blue Tokai.
 * Short forms stay in capitals (KFC, IRCTC). A name that is already mixed case, or has no spaces and odd characters
 * like a UPI id, is left as it is. Only for display; the stored name never changes.
 */
fun displayMerchant(raw: String): String {
    if (raw.any { it.isLowerCase() }) return raw
    return raw.split(' ').joinToString(" ") { word ->
        when {
            word.isEmpty() -> word
            word in SHORT_FORMS -> word
            word.length <= 3 && word !in SMALL_WORDS && word.all { it.isLetter() } -> word
            !word.all { it.isLetter() || it == '\'' || it == '&' || it == '-' } -> word
            else -> word.lowercase().replaceFirstChar { it.titlecase(Locale.ROOT) }
        }
    }
}

private val SHORT_FORMS = setOf("IRCTC", "HDFC", "ICICI", "BSNL", "IOCL", "BPCL", "HPCL", "NEFT", "IMPS", "RTGS", "UPI", "EMI", "ATM")
private val SMALL_WORDS = setOf("THE", "AND", "OF", "FOR", "TO", "BY", "AT", "IN", "ON", "PAY", "BAR", "CAB", "BUS", "CAR", "TEA", "ONE", "NEW", "BIG", "MY")

/** The one-line privacy promise, with a lock. */
@Composable
fun PrivacyNote(modifier: Modifier = Modifier) {
    Row(modifier.semantics(mergeDescendants = true) {}, horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(R.drawable.ic_lock), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Text("Read on this phone. Nothing is uploaded.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
