package app.smsexpense.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.smsexpense.R
import app.smsexpense.ui.theme.Spacing

/**
 * "Report a problem" (#100): the whole report on screen, exactly as it will be sent, and one button to hand it to the
 * share sheet. [report] is null while it is being built.
 */
@Composable
fun ReportScreen(report: String?, onShare: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.xs, vertical = Spacing.s), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = "Back")
            }
            Text("Report a problem", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            Text(
                "This is everything the report says. It has no messages, amounts, payees or notes. " +
                    "You choose where it goes, and the app sends nothing itself.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                Text(report ?: "Getting the details...", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(Spacing.m))
            }
            Text("After you share it, add a line about what happened.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button(
            onClick = onShare,
            enabled = report != null,
            modifier = Modifier.fillMaxWidth().padding(Spacing.l).heightIn(min = 56.dp),
        ) { Text("Share report") }
    }
}
