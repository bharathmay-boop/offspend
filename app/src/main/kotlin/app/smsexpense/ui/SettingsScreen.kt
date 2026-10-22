package app.smsexpense.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import app.smsexpense.R
import app.smsexpense.ui.theme.Spacing
import app.smsexpense.ui.theme.ThemeChoice

/** One row in Settings: an icon, what it is, and its current value. A row with no [onClick] only shows its value. */
data class SettingsRow(val icon: Int, val title: String, val value: String?, val onClick: (() -> Unit)?)

/** A titled group of rows. A section with no rows yet is left out (#77). */
data class SettingsSection(val title: String, val rows: List<SettingsRow>)

/** One scrolling page in sections, in the order from the settings epic (#76). */
@Composable
fun SettingsScreen(sections: List<SettingsSection>, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.xs, vertical = Spacing.s), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = "Back")
            }
            Text("Settings", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        }
        LazyColumn(
            contentPadding = PaddingValues(start = Spacing.l, end = Spacing.l, bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl),
        ) {
            items(sections.filter { it.rows.isNotEmpty() }, key = { it.title }) { section ->
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    Text(
                        section.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = Spacing.l).semantics { heading() },
                    )
                    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
                        Column {
                            section.rows.forEachIndexed { i, row ->
                                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.surface)
                                ListItem(
                                    leadingContent = { Icon(painterResource(row.icon), contentDescription = null) },
                                    headlineContent = { Text(row.title, style = MaterialTheme.typography.titleMedium) },
                                    supportingContent = row.value?.let { { Text(it, style = MaterialTheme.typography.bodyMedium) } },
                                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                    modifier = row.onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Look › Theme: follow the system, light or dark. Picking one applies it at once and closes the dialog. */
@Composable
fun ThemeDialog(current: ThemeChoice, onPick: (ThemeChoice) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Theme") },
        text = {
            Column(Modifier.selectableGroup()) {
                ThemeChoice.entries.forEach { choice ->
                    Row(
                        Modifier.fillMaxWidth().selectable(selected = choice == current, role = Role.RadioButton) { onPick(choice) }
                            .padding(vertical = Spacing.s),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
                    ) {
                        RadioButton(selected = choice == current, onClick = null)
                        Text(choice.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
