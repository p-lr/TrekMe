package com.peterlaurence.trekme.features.common.presentation.ui.theme.m3

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val md_theme_light_primary = Color(0xFF805600)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFFFFDDB0)
val md_theme_light_onPrimaryContainer = Color(0xFF281800)
val md_theme_light_secondary = Color(0xFF6F5B40)
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFF9DEBB)
val md_theme_light_onSecondaryContainer = Color(0xFF261904)
val md_theme_light_tertiary = Color(0xFF625B71)
val md_theme_light_onTertiary = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer = Color(0xFFE8DEF8)
val md_theme_light_onTertiaryContainer = Color(0xFF1D192B)
val md_theme_light_error = Color(0xFFBA1A1A)
val md_theme_light_errorContainer = Color(0xFFFFDAD6)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_onErrorContainer = Color(0xFF410002)
val md_theme_light_background = Color(0xFFFFFBFF)
val md_theme_light_onBackground = Color(0xFF1F1B16)
val md_theme_light_surface = Color(0xFFFFFBFF)
val md_theme_light_onSurface = Color(0xFF1F1B16)
val md_theme_light_surfaceVariant = Color(0xFFEFE0CF)
val md_theme_light_onSurfaceVariant = Color(0xFF4F4539)
val md_theme_light_outline = Color(0xFF817567)
val md_theme_light_inverseOnSurface = Color(0xFFF9EFE7)
val md_theme_light_inverseSurface = Color(0xFF34302A)
val md_theme_light_inversePrimary = Color(0xFFFDBA4B)
val md_theme_light_shadow = Color(0xFF000000)
val md_theme_light_surfaceTint = Color(0xFF805600)
val md_theme_light_outlineVariant = Color(0xFFD2C4B4)
val md_theme_light_scrim = Color(0xFF000000)

val md_theme_dark_primary = Color(0xFFFDBA4B)
val md_theme_dark_onPrimary = Color(0xFF442C00)
val md_theme_dark_primaryContainer = Color(0xFF614000)
val md_theme_dark_onPrimaryContainer = Color(0xFFFFDDB0)
val md_theme_dark_secondary = Color(0xFFDCC3A1)
val md_theme_dark_onSecondary = Color(0xFF3D2E16)
val md_theme_dark_secondaryContainer = Color(0xFF55442A)
val md_theme_dark_onSecondaryContainer = Color(0xFFF9DEBB)
val md_theme_dark_tertiary = Color(0xFFCCC2DC)
val md_theme_dark_onTertiary = Color(0xFF332D41)
val md_theme_dark_tertiaryContainer = Color(0xFF4A4458)
val md_theme_dark_onTertiaryContainer = Color(0xFFE8DEF8)
val md_theme_dark_error = Color(0xFFFFB4AB)
val md_theme_dark_errorContainer = Color(0xFF93000A)
val md_theme_dark_onError = Color(0xFF690005)
val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)
val md_theme_dark_background = Color(0xFF1F1B16)
val md_theme_dark_onBackground = Color(0xFFEAE1D9)
val md_theme_dark_surface = Color(0xFF313035)
val md_theme_dark_onSurface = Color(0xFFEAE1D9)
val md_theme_dark_surfaceVariant = Color(0xFF4F4539)
val md_theme_dark_onSurfaceVariant = Color(0xFFD2C4B4)
val md_theme_dark_outline = Color(0xFF9B8F80)
val md_theme_dark_inverseOnSurface = Color(0xFF1F1B16)
val md_theme_dark_inverseSurface = Color(0xFFEAE1D9)
val md_theme_dark_inversePrimary = Color(0xFF805600)
val md_theme_dark_shadow = Color(0xFF000000)
val md_theme_dark_surfaceTint = Color(0xFFFDBA4B)
val md_theme_dark_outlineVariant = Color(0xFF4F4539)
val md_theme_dark_scrim = Color(0xFF000000)

/**
 * A grey-ish background, different from the default brown one.
 */
@Composable
fun backgroundVariant(): Color {
    return if (isSystemInDarkTheme()) Color(0xFF1b1a1f) else Color(0xFFf4eff5)
}

@Composable
fun activeColor(): Color {
    return if (isSystemInDarkTheme()) Color(0xffb79bff) else Color(0xff8f5eff)
}

val seed = Color(0xFF805600)
val accentGreen = Color(0xFF4CAF50)
val light_accentGreen = Color(0xFF006E1C)
val light_onAccentGreen = Color(0xFFFFFFFF)
val light_accentGreenContainer = Color(0xFF94F990)
val light_onAccentGreenContainer = Color(0xFF002204)
val dark_accentGreen = Color(0xFF78DC77)
val dark_onAccentGreen = Color(0xFF00390A)
val dark_accentGreenContainer = Color(0xFF005313)
val dark_onAccentGreenContainer = Color(0xFF94F990)
