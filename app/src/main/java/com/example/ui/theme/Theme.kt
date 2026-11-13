package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DSquareDarkColorScheme = darkColorScheme(
    primary = ChampagneGold,
    onPrimary = ObsidianBlack,
    primaryContainer = GoldTintSurface,
    onPrimaryContainer = WarmGoldHighlight,
    secondary = WarmGoldHighlight,
    onSecondary = ObsidianBlack,
    secondaryContainer = GraphiteElevated,
    onSecondaryContainer = PureWhite,
    tertiary = EmeraldProfit,
    onTertiary = ObsidianBlack,
    background = ObsidianBlack,
    onBackground = PureWhite,
    surface = OnyxSurface,
    onSurface = PureWhite,
    surfaceVariant = CharcoalCard,
    onSurfaceVariant = SilverText,
    surfaceContainerLowest = ObsidianBlack,
    surfaceContainerLow = OnyxSurface,
    surfaceContainer = CharcoalCard,
    surfaceContainerHigh = GraphiteElevated,
    surfaceContainerHighest = HairlineSlate,
    outline = HairlineSlate,
    outlineVariant = GraphiteElevated,
    error = RubyDanger,
    onError = PureWhite
)

private val DSquareLightColorScheme = lightColorScheme(
    primary = InkPrimary,
    onPrimary = ChampagneGold,
    primaryContainer = SoftWarmGray,
    onPrimaryContainer = InkPrimary,
    secondary = AntiqueGold,
    onSecondary = PureWhite,
    secondaryContainer = ColorTokens.LightGoldWash,
    onSecondaryContainer = InkPrimary,
    tertiary = EmeraldProfit,
    onTertiary = PureWhite,
    background = AlabasterIvory,
    onBackground = InkPrimary,
    surface = PureWhite,
    onSurface = InkPrimary,
    surfaceVariant = SoftWarmGray,
    onSurfaceVariant = InkSecondary,
    outline = LightBorder,
    outlineVariant = SoftWarmGray,
    error = RubyDanger,
    onError = PureWhite
)

private object ColorTokens {
    val LightGoldWash = androidx.compose.ui.graphics.Color(0xFFF6EED4)
}

val DSquareShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun DSquareTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DSquareDarkColorScheme else DSquareLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = DSquareTypography,
        shapes = DSquareShapes,
        content = content
    )
}
