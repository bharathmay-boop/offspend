package app.smsexpense.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.smsexpense.R

private fun inter(weight: FontWeight) =
    Font(R.font.inter, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))

private fun bricolage(weight: FontWeight) =
    Font(R.font.bricolage, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))

val Inter = FontFamily(inter(FontWeight.Normal), inter(FontWeight.Medium), inter(FontWeight.SemiBold), inter(FontWeight.Bold))
val Bricolage = FontFamily(bricolage(FontWeight.Medium), bricolage(FontWeight.SemiBold), bricolage(FontWeight.Bold))

/** Tabular figures so amounts line up in columns. */
private const val TABULAR = "tnum"

private fun style(family: FontFamily, size: Int, line: Int, weight: FontWeight, tabular: Boolean = false) = TextStyle(
    fontFamily = family, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight,
    fontFeatureSettings = if (tabular) TABULAR else null,
)

internal val AppTypography = Typography(
    displayLarge = style(Bricolage, 57, 64, FontWeight.SemiBold),
    displayMedium = style(Bricolage, 45, 52, FontWeight.SemiBold),
    displaySmall = style(Bricolage, 34, 40, FontWeight.SemiBold, tabular = true),
    headlineLarge = style(Bricolage, 32, 40, FontWeight.SemiBold),
    headlineMedium = style(Bricolage, 28, 36, FontWeight.SemiBold),
    headlineSmall = style(Bricolage, 24, 32, FontWeight.SemiBold),
    titleLarge = style(Inter, 22, 28, FontWeight.SemiBold),
    titleMedium = style(Inter, 16, 24, FontWeight.SemiBold),
    titleSmall = style(Inter, 14, 20, FontWeight.SemiBold),
    bodyLarge = style(Inter, 16, 24, FontWeight.Normal),
    bodyMedium = style(Inter, 14, 20, FontWeight.Normal),
    bodySmall = style(Inter, 12, 16, FontWeight.Normal),
    labelLarge = style(Inter, 14, 20, FontWeight.SemiBold),
    labelMedium = style(Inter, 12, 16, FontWeight.Medium),
    labelSmall = style(Inter, 11, 16, FontWeight.Medium),
)

/** Styles Material 3 has no slot for. */
@Immutable
data class AppType(val amountHero: TextStyle, val amountRow: TextStyle)

internal val AppTypeExtras = AppType(
    amountHero = style(Bricolage, 44, 52, FontWeight.SemiBold, tabular = true),
    amountRow = style(Inter, 16, 24, FontWeight.SemiBold, tabular = true),
)
