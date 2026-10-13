package app.smsexpense.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.smsexpense.R
import app.smsexpense.ui.components.PrivacyNote
import app.smsexpense.ui.theme.Motion
import app.smsexpense.ui.theme.Spacing
import app.smsexpense.ui.theme.reducedMotion
import kotlinx.coroutines.delay

/**
 * Shown before Android's permission prompt, so people know what the app will read and why.
 * Three short steps carry the idea, one line says what is read (Google Play needs that visible), and the rest is one tap away.
 */
@Composable
fun PermissionScreen(deniedBefore: Boolean, onAllow: () -> Unit, onOpenSettings: () -> Unit) {
    var details by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = Spacing.l, vertical = Spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.l),
    ) {
        // Scrolls only at large font sizes; the button stays at the bottom where the thumb is.
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Spacing.xl)) {
            Text("Track spending without typing", style = MaterialTheme.typography.headlineMedium)
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
                Steps.forEachIndexed { i, (icon, text) -> Step(i, icon, text) }
            }
            // Google Play's prominent disclosure: what is read must be on screen without a tap, so this line stays visible.
            Text(
                "The app reads your SMS to find bank payments. OTPs and personal messages are skipped.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PrivacyNote()
            Column {
                TextButton(onClick = { details = !details }, modifier = Modifier.heightIn(min = 48.dp)) { Text("What does it read?") }
                AnimatedVisibility(details) {
                    Column(Modifier.padding(start = Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Bullet("Only messages about money are kept: payments, refunds and salary.")
                        Bullet("OTPs, offers and personal messages are never stored.")
                        Bullet("On setup it also files your last 3 months, so you start with real history.")
                        Bullet("There is no account, and the app cannot use the internet.")
                    }
                }
            }
        }
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

private val Steps = listOf(
    R.drawable.ic_sms to "Your bank texts you",
    R.drawable.ic_receipt_long to "The app files the payment",
    R.drawable.ic_check to "You confirm with one tap",
)

/** One step of how the app works. The steps rise in one after another the first time the screen shows. */
@Composable
private fun Step(index: Int, @DrawableRes icon: Int, text: String) {
    val reduce = reducedMotion()
    val shown = remember { MutableTransitionState(reduce) }
    LaunchedEffect(Unit) {
        delay(STEP_STAGGER_MS * index)
        shown.targetState = true
    }
    AnimatedVisibility(shown, enter = fadeIn(Motion.effects()) + slideInVertically(Motion.default()) { it / 2 }) {
        Row(
            Modifier.semantics(mergeDescendants = true) {},
            horizontalArrangement = Arrangement.spacedBy(Spacing.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(48.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Text(text, style = MaterialTheme.typography.titleMedium)
        }
    }
}

private const val STEP_STAGGER_MS = 120L

/** A point with a hanging indent: wrapped lines start under the text, not under the bullet. */
@Composable
internal fun Bullet(text: String) {
    Row(Modifier.semantics(mergeDescendants = true) {}, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Text("•", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
    }
}
