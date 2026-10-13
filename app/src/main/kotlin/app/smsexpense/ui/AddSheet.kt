package app.smsexpense.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.smsexpense.R
import app.smsexpense.core.Direction
import app.smsexpense.data.CategoryEntity
import app.smsexpense.ui.components.CategoryChip
import app.smsexpense.ui.components.dayLabel
import app.smsexpense.ui.theme.Haptics
import app.smsexpense.ui.theme.Motion
import app.smsexpense.ui.theme.Spacing
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Currency
import java.util.Locale

/** What the add sheet hands back. */
data class AddResult(
    val direction: Direction,
    val amount: BigDecimal,
    val currency: String,
    val payee: String?,
    val categoryId: Long?,
    val date: LocalDate,
    val note: String,
)

/** An amount above zero with at most two decimals ("1,250.5" is fine), or null. */
fun parseAmount(text: String): BigDecimal? {
    val n = text.replace(",", "").trim().toBigDecimalOrNull() ?: return null
    return n.takeIf { it.signum() > 0 && it.stripTrailingZeros().scale() <= 2 }
}

/** The currency used most in recent payments, or the phone's region currency when there are none. */
fun defaultCurrency(recent: List<String>, locale: Locale = Locale.getDefault()): String =
    recent.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
        ?: runCatching { Currency.getInstance(locale).currencyCode }.getOrDefault("INR")

/** Common currencies offered after the ones already in the ledger. */
private val CommonCurrencies = listOf("INR", "USD", "EUR", "GBP", "AED", "SGD", "AUD", "CAD")

/**
 * Adds a payment or money received by hand (#51). The amount comes first, with the keypad already up, because it is
 * the one thing always needed. Like the category sheet, one tap on a category saves it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSheet(
    categories: List<CategoryEntity>,
    /** Currencies already in the ledger, most used first. */
    currencies: List<String>,
    defaultCurrency: String,
    /** Payees used before, most recent first. */
    payees: List<String>,
    /** The category the app would suggest for a payee, as on the notification. */
    guess: (String, Direction) -> Long?,
    today: LocalDate,
    onSave: (AddResult) -> Unit,
    /** Editing an entry (#52): its current values. */
    initial: AddResult? = null,
    /** For an entry read from the bank: the amount, currency and direction can't change. */
    moneyLocked: Boolean = false,
) {
    var direction by rememberSaveable { mutableStateOf(initial?.direction ?: Direction.DEBIT) }
    var amountText by rememberSaveable { mutableStateOf(initial?.amount?.stripTrailingZeros()?.toPlainString().orEmpty()) }
    var currency by rememberSaveable { mutableStateOf(initial?.currency ?: defaultCurrency) }
    var payee by rememberSaveable { mutableStateOf(initial?.payee.orEmpty()) }
    var date by rememberSaveable { mutableStateOf(initial?.date ?: today) }
    var note by rememberSaveable { mutableStateOf(initial?.note.orEmpty()) }
    var noting by rememberSaveable { mutableStateOf(!initial?.note.isNullOrBlank()) }
    var selected by rememberSaveable { mutableStateOf(initial?.categoryId) }
    var filing by rememberSaveable { mutableStateOf<Long?>(null) }
    var amountError by rememberSaveable { mutableStateOf(false) }
    var pickingDate by remember { mutableStateOf(false) }
    var pickingCurrency by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val focus = remember { FocusRequester() }
    val amount = parseAmount(amountText)
    val suggested = payee.trim().takeIf { it.isNotEmpty() }?.let { guess(it, direction) }
    val save by rememberUpdatedState {
        onSave(AddResult(direction, amount!!, currency, payee.trim().ifEmpty { null }, selected, date, note.trim()))
    }

    LaunchedEffect(Unit) { if (initial == null) runCatching { focus.requestFocus() } }
    LaunchedEffect(filing) {
        if (filing == null) return@LaunchedEffect
        delay(Motion.CONFIRM_HOLD_MS)
        save()
    }

    Column(
        Modifier.fillMaxWidth().imePadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.l, vertical = Spacing.s),
        verticalArrangement = Arrangement.spacedBy(Spacing.l),
    ) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf(Direction.DEBIT to "Paid", Direction.CREDIT to "Received").forEachIndexed { i, (d, label) ->
                SegmentedButton(
                    enabled = !moneyLocked,
                    selected = direction == d,
                    onClick = { direction = d; haptics.performHapticFeedback(Haptics.tick) },
                    shape = SegmentedButtonDefaults.itemShape(i, 2),
                ) { Text(label) }
            }
        }

        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Box {
                TextButton(onClick = { pickingCurrency = true }, enabled = !moneyLocked, modifier = Modifier.heightIn(min = 48.dp)) { Text(currency) }
                DropdownMenu(pickingCurrency, onDismissRequest = { pickingCurrency = false }) {
                    (currencies + CommonCurrencies).distinct().forEach { c ->
                        DropdownMenuItem(text = { Text(c) }, onClick = { currency = c; pickingCurrency = false })
                    }
                }
            }
            OutlinedTextField(
                value = amountText,
                onValueChange = { t -> if (t.all { it.isDigit() || it == '.' || it == ',' } && t.length <= 12) { amountText = t; amountError = false } },
                label = { Text(if (direction == Direction.DEBIT) "Amount paid" else "Amount received") },
                singleLine = true,
                isError = amountError,
                enabled = !moneyLocked,
                supportingText = when {
                    amountError -> ({ Text("Enter an amount above zero") })
                    moneyLocked -> ({ Text("From your bank's message, so it can't be changed") })
                    else -> null
                },
                textStyle = MaterialTheme.typography.headlineSmall,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                modifier = Modifier.weight(1f).focusRequester(focus).testTag("amount"),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            OutlinedTextField(
                value = payee,
                onValueChange = { payee = it.take(40) },
                label = { Text(if (direction == Direction.DEBIT) "Paid to (optional)" else "Received from (optional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth().testTag("payee"),
            )
            // Names used before, so a regular payee is two letters and a tap.
            val matches = payees.filter { it.startsWith(payee.trim(), ignoreCase = true) && !it.equals(payee.trim(), ignoreCase = true) }.take(3)
            if (payee.isNotBlank() && matches.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    matches.forEach { m -> SuggestionChip(onClick = { payee = m }, label = { Text(m) }, modifier = Modifier.heightIn(min = 48.dp)) }
                }
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            categories.sortedByDescending { it.id == (suggested ?: initial?.categoryId) }.forEach { c ->
                CategoryChip(
                    name = c.name, slot = c.slot, icon = c.icon,
                    selected = selected == c.id,
                    filed = filing == c.id,
                    suggested = c.id == suggested,
                    onClick = {
                        if (filing != null) return@CategoryChip
                        if (amount == null) {
                            amountError = true
                            haptics.performHapticFeedback(Haptics.reject)
                            return@CategoryChip
                        }
                        selected = c.id
                        if (noting) {
                            haptics.performHapticFeedback(Haptics.tick)
                        } else {
                            haptics.performHapticFeedback(Haptics.confirm)
                            filing = c.id
                        }
                    },
                )
            }
            AssistChip(
                onClick = { pickingDate = true },
                label = { Text(dayLabel(date, today)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_calendar_today), contentDescription = null, Modifier.size(AssistChipDefaults.IconSize)) },
                modifier = Modifier.heightIn(min = 48.dp),
            )
            if (!noting) {
                AssistChip(
                    onClick = { noting = true },
                    label = { Text("Add note") },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_edit_note), contentDescription = null, Modifier.size(AssistChipDefaults.IconSize)) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }

        AnimatedVisibility(noting) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(200) },
                    label = { Text("Note") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth().testTag("note"),
                )
                Button(
                    onClick = {
                        if (amount == null) { amountError = true; haptics.performHapticFeedback(Haptics.reject) } else {
                            haptics.performHapticFeedback(Haptics.confirm)
                            save()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) { Text("Save") }
            }
        }
    }

    if (pickingDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) =
                    !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isAfter(today)
            },
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    pickingDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("Cancel") } },
        ) { DatePicker(state) }
    }
}
