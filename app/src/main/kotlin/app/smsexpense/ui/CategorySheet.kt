package app.smsexpense.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.smsexpense.R
import app.smsexpense.core.Direction
import app.smsexpense.data.CategoryEntity
import app.smsexpense.ui.components.AmountText
import app.smsexpense.ui.components.CategoryChip
import app.smsexpense.ui.components.CategoryIcon
import app.smsexpense.ui.components.CategoryIconSize
import app.smsexpense.ui.components.PickableIcons
import app.smsexpense.ui.components.displayMerchant
import app.smsexpense.ui.theme.AppTheme
import app.smsexpense.ui.theme.Haptics
import app.smsexpense.ui.theme.Motion
import app.smsexpense.ui.theme.Spacing
import kotlinx.coroutines.delay
import java.math.BigDecimal

/** What the sheet hands back when the payment is filed. A new category name wins over a picked one. */
data class SheetResult(val categoryId: Long?, val newCategoryName: String?, val note: String, val newCategoryIcon: String? = null)

/**
 * The content of the bottom sheet that files a transaction. In the usual case one tap on a category files it.
 * Only when a note or a new category is being typed does a Save button appear, because then the tap is not the last step.
 */
@Composable
fun CategorySheet(
    amount: BigDecimal,
    currency: String,
    direction: Direction,
    merchant: String?,
    detail: String,
    categories: List<CategoryEntity>,
    initialCategoryId: Long?,
    initialNote: String?,
    onSave: (SheetResult) -> Unit,
    /** "2 of 3" while reviewing several payments in a row. */
    progress: String? = null,
    /** In a review run: move on and leave this payment without a category. Null hides Skip. */
    onSkip: (() -> Unit)? = null,
) {
    var selected by rememberSaveable { mutableStateOf(initialCategoryId) }
    var note by rememberSaveable { mutableStateOf(initialNote.orEmpty()) }
    var noting by rememberSaveable { mutableStateOf(!initialNote.isNullOrBlank()) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var newName by rememberSaveable { mutableStateOf("") }
    var newIcon by rememberSaveable { mutableStateOf<String?>(null) }
    // Set by a one-tap pick: the chip shows its check for a moment, then the sheet files and closes.
    var filing by rememberSaveable { mutableStateOf<Long?>(null) }
    val haptics = LocalHapticFeedback.current
    val save by rememberUpdatedState {
        val typed = newName.trim().ifEmpty { null }
        onSave(SheetResult(selected, typed, note, newIcon.takeIf { typed != null }))
    }

    LaunchedEffect(filing) {
        if (filing == null) return@LaunchedEffect
        delay(Motion.CONFIRM_HOLD_MS)
        save()
    }

    // The app's guess first, so the most likely tap is the first chip.
    val ordered = categories.sortedByDescending { it.id == initialCategoryId }

    Column(
        // Scrolls and sits above the keyboard, so the field being typed in and Save stay visible.
        Modifier.fillMaxWidth().imePadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.l, vertical = Spacing.s),
        verticalArrangement = Arrangement.spacedBy(Spacing.l),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            if (progress != null) Text(progress, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AmountText(amount, currency, direction, style = AppTheme.type.amountHero)
            Text(merchant?.let(::displayMerchant) ?: "Payee not named in the message", style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            ordered.forEach { c ->
                CategoryChip(
                    name = c.name,
                    slot = c.slot,
                    selected = selected == c.id && newName.isBlank(),
                    filed = filing == c.id,
                    suggested = c.id == initialCategoryId,
                    icon = c.icon,
                    onClick = {
                        if (filing != null) return@CategoryChip
                        selected = c.id
                        newName = ""
                        creating = false
                        if (!noting) {
                            haptics.performHapticFeedback(Haptics.confirm)
                            filing = c.id
                        } else {
                            haptics.performHapticFeedback(Haptics.tick)
                        }
                    },
                )
            }
            AssistChip(
                onClick = { creating = true },
                label = { Text("+ New category") },
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
            if (onSkip != null) {
                AssistChip(onClick = { if (filing == null) onSkip() }, label = { Text("Skip") }, modifier = Modifier.heightIn(min = 48.dp))
            }
        }

        AnimatedVisibility(creating) {
          Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it.take(30) },
                label = { Text("New category name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (newName.isNotBlank() && !noting) {
                        haptics.performHapticFeedback(Haptics.confirm)
                        save()
                    }
                }),
                // A neutral preview until the name is typed; null would mean "needs a category" and take its colour.
                leadingIcon = { CategoryIcon(newName.trim().ifEmpty { "?" }, slot = -2, size = CategoryIconSize.small, icon = newIcon) },
                modifier = Modifier.fillMaxWidth().testTag("newCategory"),
            )
            // Optional: with no icon picked the category shows its first letter, as before.
            IconPicker(newIcon, onPick = { newIcon = if (newIcon == it) null else it; haptics.performHapticFeedback(Haptics.tick) })
          }
        }

        AnimatedVisibility(noting) {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(200) },
                label = { Text("Note") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth().testTag("note"),
            )
        }

        AnimatedVisibility(noting || creating) {
            Button(
                onClick = {
                    haptics.performHapticFeedback(Haptics.confirm)
                    save()
                },
                enabled = selected != null || newName.isNotBlank(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) { Text("Save") }
        }
    }
}

/** A row of icons for a new category. Tap one to pick it, tap it again to go back to the letter. */
@Composable
private fun IconPicker(picked: String?, onPick: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        PickableIcons.forEach { p ->
            val on = p.key == picked
            Box(
                Modifier.size(48.dp)
                    .background(if (on) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                    .border(if (on) 2.dp else 0.dp, if (on) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                    .clip(CircleShape)
                    .selectable(selected = on, onClick = { onPick(p.key) })
                    .semantics { contentDescription = "${p.label} icon" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(p.res), contentDescription = null,
                    tint = if (on) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
