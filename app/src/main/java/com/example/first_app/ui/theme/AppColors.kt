package com.example.first_app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/*
 * Central place for every color used by the app. Change the brand tokens below and both the
 * light and dark themes follow. UI code should read colors from MaterialTheme.colorScheme or
 * MaterialTheme.appColors, never from these constants directly.
 */

// Brand: indigo
private val Indigo10 = Color(0xFF0B0A5E)
private val Indigo20 = Color(0xFF1F2094)
private val Indigo30 = Color(0xFF3739AF)
private val Indigo40 = Color(0xFF4F53C9)
private val Indigo80 = Color(0xFFC0C1FF)
private val Indigo90 = Color(0xFFE1E0FF)

// Secondary: slate violet
private val Slate10 = Color(0xFF191A2C)
private val Slate20 = Color(0xFF2E2F42)
private val Slate30 = Color(0xFF444559)
private val Slate40 = Color(0xFF5C5D72)
private val Slate80 = Color(0xFFC5C4DD)
private val Slate90 = Color(0xFFE1E0F9)

// Tertiary: amber, used for snoozes and attention banners
private val Amber10 = Color(0xFF2C1600)
private val Amber20 = Color(0xFF4A2800)
private val Amber30 = Color(0xFF693C00)
private val Amber40 = Color(0xFF8B5000)
private val Amber80 = Color(0xFFFFB870)
private val Amber90 = Color(0xFFFFDCBE)

// Success: green, used for completed reminders
private val Green10 = Color(0xFF00210E)
private val Green20 = Color(0xFF003919)
private val Green30 = Color(0xFF005229)
private val Green40 = Color(0xFF006D3B)
private val Green80 = Color(0xFF7BDA9B)
private val Green90 = Color(0xFF97F7B5)

// Error: red
private val Red10 = Color(0xFF410002)
private val Red20 = Color(0xFF690005)
private val Red30 = Color(0xFF93000A)
private val Red40 = Color(0xFFBA1A1A)
private val Red80 = Color(0xFFFFB4AB)
private val Red90 = Color(0xFFFFDAD6)

// Neutrals with a slight indigo tint
private val Neutral4 = Color(0xFF0E0E13)
private val Neutral6 = Color(0xFF131318)
private val Neutral10 = Color(0xFF1B1B21)
private val Neutral12 = Color(0xFF1F1F25)
private val Neutral17 = Color(0xFF2A292F)
private val Neutral20 = Color(0xFF303036)
private val Neutral22 = Color(0xFF35343A)
private val Neutral24 = Color(0xFF39383F)
private val Neutral87 = Color(0xFFDBD9E0)
private val Neutral90 = Color(0xFFE4E1E9)
private val Neutral92 = Color(0xFFE9E7EF)
private val Neutral94 = Color(0xFFEFEDF4)
private val Neutral95 = Color(0xFFF2EFF7)
private val Neutral96 = Color(0xFFF5F2FA)
private val Neutral98 = Color(0xFFFBF8FF)
private val Neutral100 = Color(0xFFFFFFFF)

private val NeutralVariant30 = Color(0xFF46464F)
private val NeutralVariant50 = Color(0xFF777680)
private val NeutralVariant60 = Color(0xFF918F9A)
private val NeutralVariant80 = Color(0xFFC7C5D0)
private val NeutralVariant90 = Color(0xFFE4E1EC)

internal val LightColorScheme: ColorScheme = lightColorScheme(
    primary = Indigo40,
    onPrimary = Neutral100,
    primaryContainer = Indigo90,
    onPrimaryContainer = Indigo10,
    inversePrimary = Indigo80,
    secondary = Slate40,
    onSecondary = Neutral100,
    secondaryContainer = Slate90,
    onSecondaryContainer = Slate10,
    tertiary = Amber40,
    onTertiary = Neutral100,
    tertiaryContainer = Amber90,
    onTertiaryContainer = Amber10,
    error = Red40,
    onError = Neutral100,
    errorContainer = Red90,
    onErrorContainer = Red10,
    background = Neutral98,
    onBackground = Neutral10,
    surface = Neutral98,
    onSurface = Neutral10,
    surfaceVariant = NeutralVariant90,
    onSurfaceVariant = NeutralVariant30,
    surfaceTint = Indigo40,
    inverseSurface = Neutral20,
    inverseOnSurface = Neutral95,
    outline = NeutralVariant50,
    outlineVariant = NeutralVariant80,
    scrim = Color.Black,
    surfaceBright = Neutral98,
    surfaceDim = Neutral87,
    surfaceContainerLowest = Neutral100,
    surfaceContainerLow = Neutral96,
    surfaceContainer = Neutral94,
    surfaceContainerHigh = Neutral92,
    surfaceContainerHighest = Neutral90,
)

internal val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = Indigo80,
    onPrimary = Indigo20,
    primaryContainer = Indigo30,
    onPrimaryContainer = Indigo90,
    inversePrimary = Indigo40,
    secondary = Slate80,
    onSecondary = Slate20,
    secondaryContainer = Slate30,
    onSecondaryContainer = Slate90,
    tertiary = Amber80,
    onTertiary = Amber20,
    tertiaryContainer = Amber30,
    onTertiaryContainer = Amber90,
    error = Red80,
    onError = Red20,
    errorContainer = Red30,
    onErrorContainer = Red90,
    background = Neutral6,
    onBackground = Neutral90,
    surface = Neutral6,
    onSurface = Neutral90,
    surfaceVariant = NeutralVariant30,
    onSurfaceVariant = NeutralVariant80,
    surfaceTint = Indigo80,
    inverseSurface = Neutral90,
    inverseOnSurface = Neutral20,
    outline = NeutralVariant60,
    outlineVariant = NeutralVariant30,
    scrim = Color.Black,
    surfaceBright = Neutral24,
    surfaceDim = Neutral6,
    surfaceContainerLowest = Neutral4,
    surfaceContainerLow = Neutral10,
    surfaceContainer = Neutral12,
    surfaceContainerHigh = Neutral17,
    surfaceContainerHighest = Neutral22,
)

/** Colors the Material color scheme has no slot for. */
@Immutable
data class AppColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
)

internal val LightAppColors = AppColors(
    success = Green40,
    onSuccess = Neutral100,
    successContainer = Green90,
    onSuccessContainer = Green10,
)

internal val DarkAppColors = AppColors(
    success = Green80,
    onSuccess = Green20,
    successContainer = Green30,
    onSuccessContainer = Green90,
)

internal val LocalAppColors = staticCompositionLocalOf { LightAppColors }

val MaterialTheme.appColors: AppColors
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current
