package app.smsexpense.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Values and their meaning are documented in DESIGN.md. Screens never use these directly.

internal val LightScheme = lightColorScheme(
    primary = Color(0xFF0D6B58), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB8EFDC), onPrimaryContainer = Color(0xFF00201A),
    secondary = Color(0xFF4B635B), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCDE8DD), onSecondaryContainer = Color(0xFF072019),
    tertiary = Color(0xFF7A5900), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDF9E), onTertiaryContainer = Color(0xFF261A00),
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF6FAF7), onBackground = Color(0xFF171D1A),
    surface = Color(0xFFF6FAF7), onSurface = Color(0xFF171D1A),
    surfaceVariant = Color(0xFFDBE5DF), onSurfaceVariant = Color(0xFF3F4945),
    outline = Color(0xFF6F7975), outlineVariant = Color(0xFFBFC9C4),
    inverseSurface = Color(0xFF2C322F), inverseOnSurface = Color(0xFFEDF2EE), inversePrimary = Color(0xFF8CD5BD),
    surfaceDim = Color(0xFFD6DBD8), surfaceBright = Color(0xFFF6FAF7),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF0F5F1),
    surfaceContainer = Color(0xFFEAEFEB), surfaceContainerHigh = Color(0xFFE4EAE6),
    surfaceContainerHighest = Color(0xFFDFE4E0),
)

internal val DarkScheme = darkColorScheme(
    primary = Color(0xFF8CD5BD), onPrimary = Color(0xFF00382D),
    primaryContainer = Color(0xFF005142), onPrimaryContainer = Color(0xFFA8F2D9),
    secondary = Color(0xFFB1CCC1), onSecondary = Color(0xFF1D352E),
    secondaryContainer = Color(0xFF344C44), onSecondaryContainer = Color(0xFFCDE8DD),
    tertiary = Color(0xFFF5BF48), onTertiary = Color(0xFF402D00),
    tertiaryContainer = Color(0xFF5C4300), onTertiaryContainer = Color(0xFFFFDF9E),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0F1512), onBackground = Color(0xFFDEE4E0),
    surface = Color(0xFF0F1512), onSurface = Color(0xFFDEE4E0),
    surfaceVariant = Color(0xFF3F4945), onSurfaceVariant = Color(0xFFBFC9C4),
    outline = Color(0xFF89938E), outlineVariant = Color(0xFF3F4945),
    inverseSurface = Color(0xFFDEE4E0), inverseOnSurface = Color(0xFF2C322F), inversePrimary = Color(0xFF0D6B58),
    surfaceDim = Color(0xFF0F1512), surfaceBright = Color(0xFF353B38),
    surfaceContainerLowest = Color(0xFF0A0F0D), surfaceContainerLow = Color(0xFF171D1A),
    surfaceContainer = Color(0xFF1B211E), surfaceContainerHigh = Color(0xFF252B28),
    surfaceContainerHighest = Color(0xFF303633),
)

/** Colors Material 3 has no role for: money direction and category identity. */
@Immutable
data class AppColors(val credit: Color, val categories: List<Color>, val categoryFallback: Color)

// Category slots in fixed order (blue, orange, aqua, yellow, magenta, green, violet, red).
internal val LightAppColors = AppColors(
    credit = Color(0xFF1B7F3B),
    categories = listOf(0xFF2A78D6, 0xFFEB6834, 0xFF1BAF7A, 0xFFEDA100, 0xFFE87BA4, 0xFF008300, 0xFF4A3AA7, 0xFFE34948).map(::Color),
    categoryFallback = Color(0xFF6F7975),
)

internal val DarkAppColors = AppColors(
    credit = Color(0xFF7BDA8F),
    categories = listOf(0xFF3987E5, 0xFFD95926, 0xFF199E70, 0xFFC98500, 0xFFD55181, 0xFF008300, 0xFF9085E9, 0xFFE66767).map(::Color),
    categoryFallback = Color(0xFF89938E),
)
