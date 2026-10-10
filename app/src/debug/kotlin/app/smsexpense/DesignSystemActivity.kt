package app.smsexpense

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.smsexpense.core.Direction
import app.smsexpense.ui.components.AmountText
import app.smsexpense.ui.components.CategoryChip
import app.smsexpense.ui.components.PrivacyNote
import app.smsexpense.ui.components.TransactionRow
import app.smsexpense.ui.components.categoryColor
import app.smsexpense.ui.theme.AppTheme
import app.smsexpense.ui.theme.ExpenseTheme
import app.smsexpense.ui.theme.Spacing
import java.math.BigDecimal

/** Debug-only screen that shows every token and shared component, in light or dark. */
class DesignSystemActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var dark by remember { mutableStateOf(false) }
            ExpenseTheme(dark = dark) {
                Surface(Modifier.fillMaxSize()) { DesignSystemScreen(dark) { dark = it } }
            }
        }
    }
}

@Composable
private fun DesignSystemScreen(dark: Boolean, onDark: (Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.xl),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Design system", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            Text("Dark", style = MaterialTheme.typography.labelLarge)
            Switch(checked = dark, onCheckedChange = onDark, modifier = Modifier.padding(start = Spacing.s))
        }

        Section("Color") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                listOf(
                    "primary" to scheme.primary, "primaryContainer" to scheme.primaryContainer,
                    "tertiary" to scheme.tertiary, "surfaceContainer" to scheme.surfaceContainer,
                    "onSurface" to scheme.onSurface, "credit" to AppTheme.colors.credit, "error" to scheme.error,
                ).forEach { (name, color) -> Swatch(name, color) }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                repeat(8) { Box(Modifier.size(28.dp).background(categoryColor(it), MaterialTheme.shapes.extraSmall)) }
            }
        }

        Section("Type") {
            Text("₹1,25,000.00", style = AppTheme.type.amountHero)
            Text("This month ₹42,180", style = MaterialTheme.typography.displaySmall)
            Text("Screen title", style = MaterialTheme.typography.headlineSmall)
            Text("California Burrito", style = MaterialTheme.typography.titleMedium)
            Text("Body text reads like this, at the size most of the app uses.", style = MaterialTheme.typography.bodyLarge)
            Text("8 Oct · HDFC Bank ··1234", style = MaterialTheme.typography.labelMedium)
        }

        Section("Shape") {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                listOf(MaterialTheme.shapes.extraSmall, MaterialTheme.shapes.small, MaterialTheme.shapes.medium,
                    MaterialTheme.shapes.large, MaterialTheme.shapes.extraLarge).forEach {
                    Box(Modifier.size(48.dp).background(scheme.primaryContainer, it))
                }
            }
        }

        Section("Components") {
            var selected by remember { mutableIntStateOf(0) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                listOf("Food", "Transport", "Shopping", "Bills").forEachIndexed { i, name ->
                    CategoryChip(name, i, selected == i, onClick = { selected = i })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Button(onClick = {}) { Text("Looks right") }
                OutlinedButton(onClick = {}) { Text("Change") }
            }
            AmountText(BigDecimal("320.00"), "INR", Direction.DEBIT, style = AppTheme.type.amountHero)
            AmountText(BigDecimal("45000.00"), "INR", Direction.CREDIT)
            AmountText(BigDecimal("45.50"), "AED", Direction.DEBIT)
            Surface(color = scheme.surfaceContainerLow, shape = MaterialTheme.shapes.medium) {
                Column {
                    TransactionRow("California Burrito", "Food", 0, "8 Oct", BigDecimal("320"), "INR", Direction.DEBIT)
                    TransactionRow("ACME TECHNOLOGIES PVT LTD", "Salary", 5, "1 Oct", BigDecimal("125000"), "INR", Direction.CREDIT)
                    TransactionRow("PNB ··3210", null, 99, "6 Oct", BigDecimal("400"), "INR", Direction.DEBIT)
                }
            }
            PrivacyNote()
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

@Composable
private fun Swatch(name: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(56.dp).background(color, MaterialTheme.shapes.small))
        Text(name, style = MaterialTheme.typography.labelSmall)
    }
}
