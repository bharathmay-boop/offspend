package app.smsexpense.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.smsexpense.ui.components.PrivacyNote
import app.smsexpense.ui.theme.Spacing

/**
 * Shown before Android's permission prompt, so people know what the app will read and why.
 * Google Play requires this explanation for SMS access.
 */
@Composable
fun PermissionScreen(deniedBefore: Boolean, onAllow: () -> Unit, onOpenSettings: () -> Unit) {
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = Spacing.l, vertical = Spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.l),
    ) {
        Text("Track spending without typing", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Your bank already texts you after every payment. With SMS access, this app reads those messages " +
                "and files each payment for you. You just confirm the category with one tap.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Bullet("It only keeps messages about money: payments, refunds and salary.")
        Bullet("On setup it also files your last 3 months, so you start with real history.")
        Bullet("OTPs, offers and personal messages are skipped and never stored.")
        Bullet("Everything stays on this phone. The app has no account and uploads nothing.")
        PrivacyNote()
        Spacer(Modifier.weight(1f))
        if (deniedBefore) {
            Text(
                "SMS access is off, so payments can't be filed automatically. You can turn it on in Settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Open Settings") }
            TextButton(onClick = onAllow, modifier = Modifier.fillMaxWidth()) { Text("Ask again") }
        } else {
            Button(onClick = onAllow, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Allow SMS access") }
        }
    }
}

/** A point with a hanging indent: wrapped lines start under the text, not under the bullet. */
@Composable
internal fun Bullet(text: String) {
    Row(Modifier.semantics(mergeDescendants = true) {}, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Text("•", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
    }
}
