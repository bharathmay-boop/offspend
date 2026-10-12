package app.smsexpense.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.smsexpense.core.Direction
import app.smsexpense.data.CategoryEntity
import app.smsexpense.ui.components.AmountText
import app.smsexpense.ui.components.CategoryChip
import app.smsexpense.ui.theme.AppTheme
import app.smsexpense.ui.theme.Spacing
import java.math.BigDecimal

/** What the sheet hands back when the person taps Save. A new category name wins over a picked one. */
data class SheetResult(val categoryId: Long?, val newCategoryName: String?, val note: String)

/** The content of the bottom sheet that files a transaction: amount, categories, new category, note. */
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
    onCancel: () -> Unit,
) {
    var selected by rememberSaveable { mutableStateOf(initialCategoryId) }
    var note by rememberSaveable { mutableStateOf(initialNote.orEmpty()) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var newName by rememberSaveable { mutableStateOf("") }
    val pickedOrTyped = selected != null || newName.isNotBlank()

    Column(
        // Scrolls and sits above the keyboard, so the field being typed in and Save stay visible.
        Modifier.fillMaxWidth().imePadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.l, vertical = Spacing.s),
        verticalArrangement = Arrangement.spacedBy(Spacing.l),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            AmountText(amount, currency, direction, style = AppTheme.type.amountHero)
            Text(merchant ?: "Payee not named in the message", style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Text("Category", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            categories.forEach { c ->
                CategoryChip(c.name, c.slot, selected == c.id && newName.isBlank(), onClick = {
                    selected = c.id
                    newName = ""
                    creating = false
                })
            }
            AssistChip(onClick = { creating = true }, label = { Text("+ New category") }, modifier = Modifier.heightIn(min = 48.dp))
        }
        if (creating) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it.take(30) },
                label = { Text("New category name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth().testTag("newCategory"),
            )
        }

        OutlinedTextField(
            value = note,
            onValueChange = { note = it.take(200) },
            label = { Text("Note (optional)") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(),
            modifier = Modifier.fillMaxWidth().testTag("note"),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = 56.dp)) { Text("Cancel") }
            Button(
                onClick = { onSave(SheetResult(selected, newName.trim().ifEmpty { null }, note)) },
                enabled = pickedOrTyped,
                modifier = Modifier.weight(1f).heightIn(min = 56.dp),
            ) { Text("Save") }
        }
    }
}
