package app.smsexpense.ui.theme

import android.graphics.Color
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.smsexpense.ExpenseApp

internal val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** The 4dp grid from DESIGN.md. */
object Spacing {
    val xxs = 2.dp
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
}

/** Springs from the Material 3 Expressive standard motion scheme. Screens never use tween durations. */
object Motion {
    fun <T> fast(): SpringSpec<T> = spring(dampingRatio = 0.9f, stiffness = 1400f)
    fun <T> default(): SpringSpec<T> = spring(dampingRatio = 0.9f, stiffness = 700f)
    fun <T> effects(): SpringSpec<T> = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 1600f)

    /** A number counting to a new value (the month total): settles in about half a second, no overshoot. */
    fun <T> count(): SpringSpec<T> = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 120f)

    /** How long a confirmation (the check on a picked category) stays on screen before the sheet closes. */
    const val CONFIRM_HOLD_MS = 350L
}

/** True when the person turned animations off (Settings, Accessibility, "Remove animations"). Animations then jump to their end. */
@Composable
fun reducedMotion(): Boolean {
    val context = LocalContext.current
    return remember { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
}

/** Haptic feedback by meaning, not by effect. Through LocalHapticFeedback, so the system haptics setting is respected. */
object Haptics {
    /** Something was filed or saved. */
    val confirm = HapticFeedbackType.Confirm
    /** A small choice changed: a chip picked, a month changed. */
    val tick = HapticFeedbackType.SegmentTick
    /** An action could not complete. */
    val reject = HapticFeedbackType.Reject
}

private val LocalAppColors = staticCompositionLocalOf { LightAppColors }
private val LocalAppType = staticCompositionLocalOf { AppTypeExtras }

/** Access to the tokens Material 3 has no slot for. */
object AppTheme {
    val colors: AppColors @Composable @ReadOnlyComposable get() = LocalAppColors.current
    val type: AppType @Composable @ReadOnlyComposable get() = LocalAppType.current
}

/** Look › Theme in Settings (#84). */
enum class ThemeChoice(val label: String) { SYSTEM("Follow the system"), LIGHT("Light"), DARK("Dark") }

/** Dark or light from the person's choice, which follows the system until they pick one. */
@Composable
fun appDark(): Boolean {
    val app = LocalContext.current.applicationContext as? ExpenseApp
    return when (app?.theme?.collectAsState()?.value ?: ThemeChoice.SYSTEM) {
        ThemeChoice.SYSTEM -> isSystemInDarkTheme()
        ThemeChoice.LIGHT -> false
        ThemeChoice.DARK -> true
    }
}

@Composable
fun ExpenseTheme(dark: Boolean = appDark(), content: @Composable () -> Unit) {
    // The status and navigation bar icons follow the app's theme too, not only the system's.
    val activity = LocalActivity.current as? ComponentActivity
    LaunchedEffect(activity, dark) {
        activity?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
            // The same scrims enableEdgeToEdge() uses by default.
            navigationBarStyle = SystemBarStyle.auto(Color.argb(0xe6, 0xFF, 0xFF, 0xFF), Color.argb(0x80, 0x1b, 0x1b, 0x1b)) { dark },
        )
    }
    CompositionLocalProvider(
        LocalAppColors provides if (dark) DarkAppColors else LightAppColors,
        LocalAppType provides AppTypeExtras,
    ) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
