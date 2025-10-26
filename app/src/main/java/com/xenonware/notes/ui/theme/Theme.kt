package com.xenonware.notes.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme.Companion.expressive
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat

data class ExtendedMaterialColorScheme(
    val inverseError: Color,
    val inverseOnError: Color,
    val inverseErrorContainer: Color,
    val inverseOnErrorContainer: Color,
    val label: Color,
    // val noteDefault: Color,
    // val noteRed: Color,
    // val noteOrange: Color,
    // val noteYellow: Color,
    // val noteGreen: Color,
    // val noteBlue: Color,
    // val noteTurquoise: Color,
    // val notePurple: Color,
    // val invertNoteRed: Color,
    // val invertNoteOrange: Color,
    // val invertNoteYellow: Color,
    // val invertNoteGreen: Color,
    // val invertNoteBlue: Color,
    // val invertNoteTurquoise: Color,
    // val invertNotePurple: Color,
)

val LocalExtendedMaterialColorScheme = staticCompositionLocalOf<ExtendedMaterialColorScheme> {
    error("No ExtendedMaterialColorScheme provided. Did you forget to wrap your Composable in TodolistTheme?")
}

val LocalIsDarkTheme = staticCompositionLocalOf<Boolean> {
    error("No IsDarkTheme provided")
}

val extendedMaterialColorScheme: ExtendedMaterialColorScheme
    @Composable @ReadOnlyComposable get() = LocalExtendedMaterialColorScheme.current


private val DarkColorScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark
)

private val LightColorScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight
)

private val BlueDarkColorScheme = darkColorScheme(
    primary = bluePrimaryDark,
    onPrimary = blueOnPrimaryDark,
    primaryContainer = bluePrimaryContainerDark,
    onPrimaryContainer = blueOnPrimaryContainerDark,
    secondary = blueSecondaryDark,
    onSecondary = blueOnSecondaryDark,
    secondaryContainer = blueSecondaryContainerDark,
    onSecondaryContainer = blueOnSecondaryContainerDark,
    tertiary = blueTertiaryDark,
    onTertiary = blueOnTertiaryDark,
    tertiaryContainer = blueTertiaryContainerDark,
    onTertiaryContainer = blueOnTertiaryContainerDark,
    error = blueErrorDark,
    onError = blueOnErrorDark,
    errorContainer = blueErrorContainerDark,
    onErrorContainer = blueOnErrorContainerDark,
    background = blueBackgroundDark,
    onBackground = blueOnBackgroundDark,
    surface = blueSurfaceDark,
    onSurface = blueOnSurfaceDark,
    surfaceVariant = blueSurfaceVariantDark,
    onSurfaceVariant = blueOnSurfaceVariantDark,
    outline = blueOutlineDark,
    outlineVariant = blueOutlineVariantDark,
    scrim = blueScrimDark,
    inverseSurface = blueInverseSurfaceDark,
    inverseOnSurface = blueInverseOnSurfaceDark,
    inversePrimary = blueInversePrimaryDark,
    surfaceDim = blueSurfaceDimDark,
    surfaceBright = blueSurfaceBrightDark,
    surfaceContainerLowest = blueSurfaceContainerLowestDark,
    surfaceContainerLow = blueSurfaceContainerLowDark,
    surfaceContainer = blueSurfaceContainerDark,
    surfaceContainerHigh = blueSurfaceContainerHighDark,
    surfaceContainerHighest = blueSurfaceContainerHighestDark
)

private val BlueLightColorScheme = lightColorScheme(
    primary = bluePrimaryLight,
    onPrimary = blueOnPrimaryLight,
    primaryContainer = bluePrimaryContainerLight,
    onPrimaryContainer = blueOnPrimaryContainerLight,
    secondary = blueSecondaryLight,
    onSecondary = blueOnSecondaryLight,
    secondaryContainer = blueSecondaryContainerLight,
    onSecondaryContainer = blueOnSecondaryContainerLight,
    tertiary = blueTertiaryLight,
    onTertiary = blueOnTertiaryLight,
    tertiaryContainer = blueTertiaryContainerLight,
    onTertiaryContainer = blueOnTertiaryContainerLight,
    error = blueErrorLight,
    onError = blueOnErrorLight,
    errorContainer = blueErrorContainerLight,
    onErrorContainer = blueOnErrorContainerLight,
    background = blueBackgroundLight,
    onBackground = blueOnBackgroundLight,
    surface = blueSurfaceLight,
    onSurface = blueOnSurfaceLight,
    surfaceVariant = blueSurfaceVariantLight,
    onSurfaceVariant = blueOnSurfaceVariantLight,
    outline = blueOutlineLight,
    outlineVariant = blueOutlineVariantLight,
    scrim = blueScrimLight,
    inverseSurface = blueInverseSurfaceLight,
    inverseOnSurface = blueInverseOnSurfaceLight,
    inversePrimary = blueInversePrimaryLight,
    surfaceDim = blueSurfaceDimLight,
    surfaceBright = blueSurfaceBrightLight,
    surfaceContainerLowest = blueSurfaceContainerLowestLight,
    surfaceContainerLow = blueSurfaceContainerLowLight,
    surfaceContainer = blueSurfaceContainerLight,
    surfaceContainerHigh = blueSurfaceContainerHighLight,
    surfaceContainerHighest = blueSurfaceContainerHighestLight
)

private val GreenDarkColorScheme = darkColorScheme(
    primary = greenPrimaryDark,
    onPrimary = greenOnPrimaryDark,
    primaryContainer = greenPrimaryContainerDark,
    onPrimaryContainer = greenOnPrimaryContainerDark,
    secondary = greenSecondaryDark,
    onSecondary = greenOnSecondaryDark,
    secondaryContainer = greenSecondaryContainerDark,
    onSecondaryContainer = greenOnSecondaryContainerDark,
    tertiary = greenTertiaryDark,
    onTertiary = greenOnTertiaryDark,
    tertiaryContainer = greenTertiaryContainerDark,
    onTertiaryContainer = greenOnTertiaryContainerDark,
    error = greenErrorDark,
    onError = greenOnErrorDark,
    errorContainer = greenErrorContainerDark,
    onErrorContainer = greenOnErrorContainerDark,
    background = greenBackgroundDark,
    onBackground = greenOnBackgroundDark,
    surface = greenSurfaceDark,
    onSurface = greenOnSurfaceDark,
    surfaceVariant = greenSurfaceVariantDark,
    onSurfaceVariant = greenOnSurfaceVariantDark,
    outline = greenOutlineDark,
    outlineVariant = greenOutlineVariantDark,
    scrim = greenScrimDark,
    inverseSurface = greenInverseSurfaceDark,
    inverseOnSurface = greenInverseOnSurfaceDark,
    inversePrimary = greenInversePrimaryDark,
    surfaceDim = greenSurfaceDimDark,
    surfaceBright = greenSurfaceBrightDark,
    surfaceContainerLowest = greenSurfaceContainerLowestDark,
    surfaceContainerLow = greenSurfaceContainerLowDark,
    surfaceContainer = greenSurfaceContainerDark,
    surfaceContainerHigh = greenSurfaceContainerHighDark,
    surfaceContainerHighest = greenSurfaceContainerHighestDark
)

private val GreenLightColorScheme = lightColorScheme(
    primary = greenPrimaryLight,
    onPrimary = greenOnPrimaryLight,
    primaryContainer = greenPrimaryContainerLight,
    onPrimaryContainer = greenOnPrimaryContainerLight,
    secondary = greenSecondaryLight,
    onSecondary = greenOnSecondaryLight,
    secondaryContainer = greenSecondaryContainerLight,
    onSecondaryContainer = greenOnSecondaryContainerLight,
    tertiary = greenTertiaryLight,
    onTertiary = greenOnTertiaryLight,
    tertiaryContainer = greenTertiaryContainerLight,
    onTertiaryContainer = greenOnTertiaryContainerLight,
    error = greenErrorLight,
    onError = greenOnErrorLight,
    errorContainer = greenErrorContainerLight,
    onErrorContainer = greenOnErrorContainerLight,
    background = greenBackgroundLight,
    onBackground = greenOnBackgroundLight,
    surface = greenSurfaceLight,
    onSurface = greenOnSurfaceLight,
    surfaceVariant = greenSurfaceVariantLight,
    onSurfaceVariant = greenOnSurfaceVariantLight,
    outline = greenOutlineLight,
    outlineVariant = greenOutlineVariantLight,
    scrim = greenScrimLight,
    inverseSurface = greenInverseSurfaceLight,
    inverseOnSurface = greenInverseOnSurfaceLight,
    inversePrimary = greenInversePrimaryLight,
    surfaceDim = greenSurfaceDimLight,
    surfaceBright = greenSurfaceBrightLight,
    surfaceContainerLowest = greenSurfaceContainerLowestLight,
    surfaceContainerLow = greenSurfaceContainerLowLight,
    surfaceContainer = greenSurfaceContainerLight,
    surfaceContainerHigh = greenSurfaceContainerHighLight,
    surfaceContainerHighest = greenSurfaceContainerHighestLight
)

private val OrangeDarkColorScheme = darkColorScheme(
    primary = orangePrimaryDark,
    onPrimary = orangeOnPrimaryDark,
    primaryContainer = orangePrimaryContainerDark,
    onPrimaryContainer = orangeOnPrimaryContainerDark,
    secondary = orangeSecondaryDark,
    onSecondary = orangeOnSecondaryDark,
    secondaryContainer = orangeSecondaryContainerDark,
    onSecondaryContainer = orangeOnSecondaryContainerDark,
    tertiary = orangeTertiaryDark,
    onTertiary = orangeOnTertiaryDark,
    tertiaryContainer = orangeTertiaryContainerDark,
    onTertiaryContainer = orangeOnTertiaryContainerDark,
    error = orangeErrorDark,
    onError = orangeOnErrorDark,
    errorContainer = orangeErrorContainerDark,
    onErrorContainer = orangeOnErrorContainerDark,
    background = orangeBackgroundDark,
    onBackground = orangeOnBackgroundDark,
    surface = orangeSurfaceDark,
    onSurface = orangeOnSurfaceDark,
    surfaceVariant = orangeSurfaceVariantDark,
    onSurfaceVariant = orangeOnSurfaceVariantDark,
    outline = orangeOutlineDark,
    outlineVariant = orangeOutlineVariantDark,
    scrim = orangeScrimDark,
    inverseSurface = orangeInverseSurfaceDark,
    inverseOnSurface = orangeInverseOnSurfaceDark,
    inversePrimary = orangeInversePrimaryDark,
    surfaceDim = orangeSurfaceDimDark,
    surfaceBright = orangeSurfaceBrightDark,
    surfaceContainerLowest = orangeSurfaceContainerLowestDark,
    surfaceContainerLow = orangeSurfaceContainerLowDark,
    surfaceContainer = orangeSurfaceContainerDark,
    surfaceContainerHigh = orangeSurfaceContainerHighDark,
    surfaceContainerHighest = orangeSurfaceContainerHighestDark
)

private val OrangeLightColorScheme = lightColorScheme(
    primary = orangePrimaryLight,
    onPrimary = orangeOnPrimaryLight,
    primaryContainer = orangePrimaryContainerLight,
    onPrimaryContainer = orangeOnPrimaryContainerLight,
    secondary = orangeSecondaryLight,
    onSecondary = orangeOnSecondaryLight,
    secondaryContainer = orangeSecondaryContainerLight,
    onSecondaryContainer = orangeOnSecondaryContainerLight,
    tertiary = orangeTertiaryLight,
    onTertiary = orangeOnTertiaryLight,
    tertiaryContainer = orangeTertiaryContainerLight,
    onTertiaryContainer = orangeOnTertiaryContainerLight,
    error = orangeErrorLight,
    onError = orangeOnErrorLight,
    errorContainer = orangeErrorContainerLight,
    onErrorContainer = orangeOnErrorContainerLight,
    background = orangeBackgroundLight,
    onBackground = orangeOnBackgroundLight,
    surface = orangeSurfaceLight,
    onSurface = orangeOnSurfaceLight,
    surfaceVariant = orangeSurfaceVariantLight,
    onSurfaceVariant = orangeOnSurfaceVariantLight,
    outline = orangeOutlineLight,
    outlineVariant = orangeOutlineVariantLight,
    scrim = orangeScrimLight,
    inverseSurface = orangeInverseSurfaceLight,
    inverseOnSurface = orangeInverseOnSurfaceLight,
    inversePrimary = orangeInversePrimaryLight,
    surfaceDim = orangeSurfaceDimLight,
    surfaceBright = orangeSurfaceBrightLight,
    surfaceContainerLowest = orangeSurfaceContainerLowestLight,
    surfaceContainerLow = orangeSurfaceContainerLowLight,
    surfaceContainer = orangeSurfaceContainerLight,
    surfaceContainerHigh = orangeSurfaceContainerHighLight,
    surfaceContainerHighest = orangeSurfaceContainerHighestLight
)

private val PurpleDarkColorScheme = darkColorScheme(
    primary = purplePrimaryDark,
    onPrimary = purpleOnPrimaryDark,
    primaryContainer = purplePrimaryContainerDark,
    onPrimaryContainer = purpleOnPrimaryContainerDark,
    secondary = purpleSecondaryDark,
    onSecondary = purpleOnSecondaryDark,
    secondaryContainer = purpleSecondaryContainerDark,
    onSecondaryContainer = purpleOnSecondaryContainerDark,
    tertiary = purpleTertiaryDark,
    onTertiary = purpleOnTertiaryDark,
    tertiaryContainer = purpleTertiaryContainerDark,
    onTertiaryContainer = purpleOnTertiaryContainerDark,
    error = purpleErrorDark,
    onError = purpleOnErrorDark,
    errorContainer = purpleErrorContainerDark,
    onErrorContainer = purpleOnErrorContainerDark,
    background = purpleBackgroundDark,
    onBackground = purpleOnBackgroundDark,
    surface = purpleSurfaceDark,
    onSurface = purpleOnSurfaceDark,
    surfaceVariant = purpleSurfaceVariantDark,
    onSurfaceVariant = purpleOnSurfaceVariantDark,
    outline = purpleOutlineDark,
    outlineVariant = purpleOutlineVariantDark,
    scrim = purpleScrimDark,
    inverseSurface = purpleInverseSurfaceDark,
    inverseOnSurface = purpleInverseOnSurfaceDark,
    inversePrimary = purpleInversePrimaryDark,
    surfaceDim = purpleSurfaceDimDark,
    surfaceBright = purpleSurfaceBrightDark,
    surfaceContainerLowest = purpleSurfaceContainerLowestDark,
    surfaceContainerLow = purpleSurfaceContainerLowDark,
    surfaceContainer = purpleSurfaceContainerDark,
    surfaceContainerHigh = purpleSurfaceContainerHighDark,
    surfaceContainerHighest = purpleSurfaceContainerHighestDark
)

private val PurpleLightColorScheme = lightColorScheme(
    primary = purplePrimaryLight,
    onPrimary = purpleOnPrimaryLight,
    primaryContainer = purplePrimaryContainerLight,
    onPrimaryContainer = purpleOnPrimaryContainerLight,
    secondary = purpleSecondaryLight,
    onSecondary = purpleOnSecondaryLight,
    secondaryContainer = purpleSecondaryContainerLight,
    onSecondaryContainer = purpleOnSecondaryContainerLight,
    tertiary = purpleTertiaryLight,
    onTertiary = purpleOnTertiaryLight,
    tertiaryContainer = purpleTertiaryContainerLight,
    onTertiaryContainer = purpleOnTertiaryContainerLight,
    error = purpleErrorLight,
    onError = purpleOnErrorLight,
    errorContainer = purpleErrorContainerLight,
    onErrorContainer = purpleOnErrorContainerLight,
    background = purpleBackgroundLight,
    onBackground = purpleOnBackgroundLight,
    surface = purpleSurfaceLight,
    onSurface = purpleOnSurfaceLight,
    surfaceVariant = purpleSurfaceVariantLight,
    onSurfaceVariant = purpleOnSurfaceVariantLight,
    outline = purpleOutlineLight,
    outlineVariant = purpleOutlineVariantLight,
    scrim = purpleScrimLight,
    inverseSurface = purpleInverseSurfaceLight,
    inverseOnSurface = purpleInverseOnSurfaceLight,
    inversePrimary = purpleInversePrimaryLight,
    surfaceDim = purpleSurfaceDimLight,
    surfaceBright = purpleSurfaceBrightLight,
    surfaceContainerLowest = purpleSurfaceContainerLowestLight,
    surfaceContainerLow = purpleSurfaceContainerLowLight,
    surfaceContainer = purpleSurfaceContainerLight,
    surfaceContainerHigh = purpleSurfaceContainerHighLight,
    surfaceContainerHighest = purpleSurfaceContainerHighestLight
)

private val RedDarkColorScheme = darkColorScheme(
    primary = redPrimaryDark,
    onPrimary = redOnPrimaryDark,
    primaryContainer = redPrimaryContainerDark,
    onPrimaryContainer = redOnPrimaryContainerDark,
    secondary = redSecondaryDark,
    onSecondary = redOnSecondaryDark,
    secondaryContainer = redSecondaryContainerDark,
    onSecondaryContainer = redOnSecondaryContainerDark,
    tertiary = redTertiaryDark,
    onTertiary = redOnTertiaryDark,
    tertiaryContainer = redTertiaryContainerDark,
    onTertiaryContainer = redOnTertiaryContainerDark,
    error = redErrorDark,
    onError = redOnErrorDark,
    errorContainer = redErrorContainerDark,
    onErrorContainer = redOnErrorContainerDark,
    background = redBackgroundDark,
    onBackground = redOnBackgroundDark,
    surface = redSurfaceDark,
    onSurface = redOnSurfaceDark,
    surfaceVariant = redSurfaceVariantDark,
    onSurfaceVariant = redOnSurfaceVariantDark,
    outline = redOutlineDark,
    outlineVariant = redOutlineVariantDark,
    scrim = redScrimDark,
    inverseSurface = redInverseSurfaceDark,
    inverseOnSurface = redInverseOnSurfaceDark,
    inversePrimary = redInversePrimaryDark,
    surfaceDim = redSurfaceDimDark,
    surfaceBright = redSurfaceBrightDark,
    surfaceContainerLowest = redSurfaceContainerLowestDark,
    surfaceContainerLow = redSurfaceContainerLowDark,
    surfaceContainer = redSurfaceContainerDark,
    surfaceContainerHigh = redSurfaceContainerHighDark,
    surfaceContainerHighest = redSurfaceContainerHighestDark
)

private val RedLightColorScheme = lightColorScheme(
    primary = redPrimaryLight,
    onPrimary = redOnPrimaryLight,
    primaryContainer = redPrimaryContainerLight,
    onPrimaryContainer = redOnPrimaryContainerLight,
    secondary = redSecondaryLight,
    onSecondary = redOnSecondaryLight,
    secondaryContainer = redSecondaryContainerLight,
    onSecondaryContainer = redOnSecondaryContainerLight,
    tertiary = redTertiaryLight,
    onTertiary = redOnTertiaryLight,
    tertiaryContainer = redTertiaryContainerLight,
    onTertiaryContainer = redOnTertiaryContainerLight,
    error = redErrorLight,
    onError = redOnErrorLight,
    errorContainer = redErrorContainerLight,
    onErrorContainer = redOnErrorContainerLight,
    background = redBackgroundLight,
    onBackground = redOnBackgroundLight,
    surface = redSurfaceLight,
    onSurface = redOnSurfaceLight,
    surfaceVariant = redSurfaceVariantLight,
    onSurfaceVariant = redOnSurfaceVariantLight,
    outline = redOutlineLight,
    outlineVariant = redOutlineVariantLight,
    scrim = redScrimLight,
    inverseSurface = redInverseSurfaceLight,
    inverseOnSurface = redInverseOnSurfaceLight,
    inversePrimary = redInversePrimaryLight,
    surfaceDim = redSurfaceDimLight,
    surfaceBright = redSurfaceBrightLight,
    surfaceContainerLowest = redSurfaceContainerLowestLight,
    surfaceContainerLow = redSurfaceContainerLowLight,
    surfaceContainer = redSurfaceContainerLight,
    surfaceContainerHigh = redSurfaceContainerHighLight,
    surfaceContainerHighest = redSurfaceContainerHighestLight
)

private val TurquoiseDarkColorScheme = darkColorScheme(
    primary = turquoisePrimaryDark,
    onPrimary = turquoiseOnPrimaryDark,
    primaryContainer = turquoisePrimaryContainerDark,
    onPrimaryContainer = turquoiseOnPrimaryContainerDark,
    secondary = turquoiseSecondaryDark,
    onSecondary = turquoiseOnSecondaryDark,
    secondaryContainer = turquoiseSecondaryContainerDark,
    onSecondaryContainer = turquoiseOnSecondaryContainerDark,
    tertiary = turquoiseTertiaryDark,
    onTertiary = turquoiseOnTertiaryDark,
    tertiaryContainer = turquoiseTertiaryContainerDark,
    onTertiaryContainer = turquoiseOnTertiaryContainerDark,
    error = turquoiseErrorDark,
    onError = turquoiseOnErrorDark,
    errorContainer = turquoiseErrorContainerDark,
    onErrorContainer = turquoiseOnErrorContainerDark,
    background = turquoiseBackgroundDark,
    onBackground = turquoiseOnBackgroundDark,
    surface = turquoiseSurfaceDark,
    onSurface = turquoiseOnSurfaceDark,
    surfaceVariant = turquoiseSurfaceVariantDark,
    onSurfaceVariant = turquoiseOnSurfaceVariantDark,
    outline = turquoiseOutlineDark,
    outlineVariant = turquoiseOutlineVariantDark,
    scrim = turquoiseScrimDark,
    inverseSurface = turquoiseInverseSurfaceDark,
    inverseOnSurface = turquoiseInverseOnSurfaceDark,
    inversePrimary = turquoiseInversePrimaryDark,
    surfaceDim = turquoiseSurfaceDimDark,
    surfaceBright = turquoiseSurfaceBrightDark,
    surfaceContainerLowest = turquoiseSurfaceContainerLowestDark,
    surfaceContainerLow = turquoiseSurfaceContainerLowDark,
    surfaceContainer = turquoiseSurfaceContainerDark,
    surfaceContainerHigh = turquoiseSurfaceContainerHighDark,
    surfaceContainerHighest = turquoiseSurfaceContainerHighestDark
)

private val TurquoiseLightColorScheme = lightColorScheme(
    primary = turquoisePrimaryLight,
    onPrimary = turquoiseOnPrimaryLight,
    primaryContainer = turquoisePrimaryContainerLight,
    onPrimaryContainer = turquoiseOnPrimaryContainerLight,
    secondary = turquoiseSecondaryLight,
    onSecondary = turquoiseOnSecondaryLight,
    secondaryContainer = turquoiseSecondaryContainerLight,
    onSecondaryContainer = turquoiseOnSecondaryContainerLight,
    tertiary = turquoiseTertiaryLight,
    onTertiary = turquoiseOnTertiaryLight,
    tertiaryContainer = turquoiseTertiaryContainerLight,
    onTertiaryContainer = turquoiseOnTertiaryContainerLight,
    error = turquoiseErrorLight,
    onError = turquoiseOnErrorLight,
    errorContainer = turquoiseErrorContainerLight,
    onErrorContainer = turquoiseOnErrorContainerLight,
    background = turquoiseBackgroundLight,
    onBackground = turquoiseOnBackgroundLight,
    surface = turquoiseSurfaceLight,
    onSurface = turquoiseOnSurfaceLight,
    surfaceVariant = turquoiseSurfaceVariantLight,
    onSurfaceVariant = turquoiseOnSurfaceVariantLight,
    outline = turquoiseOutlineLight,
    outlineVariant = turquoiseOutlineVariantLight,
    scrim = turquoiseScrimLight,
    inverseSurface = turquoiseInverseSurfaceLight,
    inverseOnSurface = turquoiseInverseOnSurfaceLight,
    inversePrimary = turquoiseInversePrimaryLight,
    surfaceDim = turquoiseSurfaceDimLight,
    surfaceBright = turquoiseSurfaceBrightLight,
    surfaceContainerLowest = turquoiseSurfaceContainerLowestLight,
    surfaceContainerLow = turquoiseSurfaceContainerLowLight,
    surfaceContainer = turquoiseSurfaceContainerLight,
    surfaceContainerHigh = turquoiseSurfaceContainerHighLight,
    surfaceContainerHighest = turquoiseSurfaceContainerHighestLight
)

private val YellowDarkColorScheme = darkColorScheme(
    primary = yellowPrimaryDark,
    onPrimary = yellowOnPrimaryDark,
    primaryContainer = yellowPrimaryContainerDark,
    onPrimaryContainer = yellowOnPrimaryContainerDark,
    secondary = yellowSecondaryDark,
    onSecondary = yellowOnSecondaryDark,
    secondaryContainer = yellowSecondaryContainerDark,
    onSecondaryContainer = yellowOnSecondaryContainerDark,
    tertiary = yellowTertiaryDark,
    onTertiary = yellowOnTertiaryDark,
    tertiaryContainer = yellowTertiaryContainerDark,
    onTertiaryContainer = yellowOnTertiaryContainerDark,
    error = yellowErrorDark,
    onError = yellowOnErrorDark,
    errorContainer = yellowErrorContainerDark,
    onErrorContainer = yellowOnErrorContainerDark,
    background = yellowBackgroundDark,
    onBackground = yellowOnBackgroundDark,
    surface = yellowSurfaceDark,
    onSurface = yellowOnSurfaceDark,
    surfaceVariant = yellowSurfaceVariantDark,
    onSurfaceVariant = yellowOnSurfaceVariantDark,
    outline = yellowOutlineDark,
    outlineVariant = yellowOutlineVariantDark,
    scrim = yellowScrimDark,
    inverseSurface = yellowInverseSurfaceDark,
    inverseOnSurface = yellowInverseOnSurfaceDark,
    inversePrimary = yellowInversePrimaryDark,
    surfaceDim = yellowSurfaceDimDark,
    surfaceBright = yellowSurfaceBrightDark,
    surfaceContainerLowest = yellowSurfaceContainerLowestDark,
    surfaceContainerLow = yellowSurfaceContainerLowDark,
    surfaceContainer = yellowSurfaceContainerDark,
    surfaceContainerHigh = yellowSurfaceContainerHighDark,
    surfaceContainerHighest = yellowSurfaceContainerHighestDark
)

private val YellowLightColorScheme = lightColorScheme(
    primary = yellowPrimaryLight,
    onPrimary = yellowOnPrimaryLight,
    primaryContainer = yellowPrimaryContainerLight,
    onPrimaryContainer = yellowOnPrimaryContainerLight,
    secondary = yellowSecondaryLight,
    onSecondary = yellowOnSecondaryLight,
    secondaryContainer = yellowSecondaryContainerLight,
    onSecondaryContainer = yellowOnSecondaryContainerLight,
    tertiary = yellowTertiaryLight,
    onTertiary = yellowOnTertiaryLight,
    tertiaryContainer = yellowTertiaryContainerLight,
    onTertiaryContainer = yellowOnTertiaryContainerLight,
    error = yellowErrorLight,
    onError = yellowOnErrorLight,
    errorContainer = yellowErrorContainerLight,
    onErrorContainer = yellowOnErrorContainerLight,
    background = yellowBackgroundLight,
    onBackground = yellowOnBackgroundLight,
    surface = yellowSurfaceLight,
    onSurface = yellowOnSurfaceLight,
    surfaceVariant = yellowSurfaceVariantLight,
    onSurfaceVariant = yellowOnSurfaceVariantLight,
    outline = yellowOutlineLight,
    outlineVariant = yellowOutlineVariantLight,
    scrim = yellowScrimLight,
    inverseSurface = yellowInverseSurfaceLight,
    inverseOnSurface = yellowInverseOnSurfaceLight,
    inversePrimary = yellowInversePrimaryLight,
    surfaceDim = yellowSurfaceDimLight,
    surfaceBright = yellowSurfaceBrightLight,
    surfaceContainerLowest = yellowSurfaceContainerLowestLight,
    surfaceContainerLow = yellowSurfaceContainerLowLight,
    surfaceContainer = yellowSurfaceContainerLight,
    surfaceContainerHigh = yellowSurfaceContainerHighLight,
    surfaceContainerHighest = yellowSurfaceContainerHighestLight
)

    

fun Color.decreaseBrightness(factor: Float): Color {
    val hsv = FloatArray(3)
    ColorUtils.colorToHSL(this.toArgb(), hsv)

    hsv[2] = hsv[2] * factor.coerceIn(0f, 1f)

    return Color(ColorUtils.HSLToColor(hsv))
}
fun ColorScheme.toBlackedOut(): ColorScheme {
    return this.copy(
        background = surfaceDimDark.decreaseBrightness(0.5f),
        surfaceContainer = Color.Black,
        surfaceBright = surfaceDimDark
    )
}
fun ColorScheme.toCoverMode(): ColorScheme {
    return this.copy(
        background = Color.Black,
        surfaceContainer = Color.Black,
        surfaceBright = Color.Black
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun XenonTheme(
    darkTheme: Boolean,
    useBlackedOutDarkTheme: Boolean = false,
    isCoverMode: Boolean = false,
    dynamicColor: Boolean = true,
    useDefaultTheme: Boolean = true,
    useBlueTheme: Boolean = false,
    useGreenTheme: Boolean = false,
    useOrangeTheme: Boolean = false,
    usePurpleTheme: Boolean = false,
    useRedTheme: Boolean = false,
    useTurquoiseTheme: Boolean = false,
    useYellowTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    val baseColorScheme: ColorScheme = if (darkTheme) {
        val baseDarkScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dynamicDarkColorScheme(context)
        } else {
            when {
                useBlueTheme -> BlueDarkColorScheme
                useGreenTheme -> GreenDarkColorScheme
                useOrangeTheme -> OrangeDarkColorScheme
                usePurpleTheme -> PurpleDarkColorScheme
                useRedTheme -> RedDarkColorScheme
                useTurquoiseTheme -> TurquoiseDarkColorScheme
                useYellowTheme -> YellowDarkColorScheme
                else -> DarkColorScheme
            }
        }
        when {
            isCoverMode -> baseDarkScheme.toCoverMode()
            useBlackedOutDarkTheme -> baseDarkScheme.toBlackedOut()
            else -> baseDarkScheme
        }
    } else {
        if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dynamicLightColorScheme(context)
        } else {
            when {
                useBlueTheme -> BlueLightColorScheme
                useGreenTheme -> GreenLightColorScheme
                useOrangeTheme -> OrangeLightColorScheme
                usePurpleTheme -> PurpleLightColorScheme
                useRedTheme -> RedLightColorScheme
                useTurquoiseTheme -> TurquoiseLightColorScheme
                useYellowTheme -> YellowLightColorScheme
                else -> LightColorScheme
            }
        }
    }

    val extendedColorScheme = remember(darkTheme) {
        if (darkTheme) {
            ExtendedMaterialColorScheme(
                inverseError = inverseErrorDark,
                inverseOnError = inverseOnErrorDark,
                inverseErrorContainer = inverseErrorContainerDark,
                inverseOnErrorContainer = inverseOnErrorContainerDark,
                label = labelDark,
                // noteDefault = surfaceContainerDark,
                // noteRed = noteRedDark,
                // noteOrange = noteOrangeDark,
                // noteYellow = noteYellowDark,
                // noteGreen = noteGreenDark,
                // noteBlue = noteBlueDark,
                // noteTurquoise = noteTurquoiseDark,
                // notePurple = notePurpleDark,

                // invertNoteRed = invertNoteRedDark,
                // invertNoteOrange = invertNoteOrangeDark,
                // invertNoteYellow = invertNoteYellowDark,
                // invertNoteGreen = invertNoteGreenDark,
                // invertNoteBlue = invertNoteBlueDark,
                // invertNoteTurquoise = invertNoteTurquoiseDark,
                // invertNotePurple = invertNotePurpleDark

            )
        } else {
            ExtendedMaterialColorScheme(
                inverseError = inverseErrorLight,
                inverseOnError = inverseOnErrorLight,
                inverseErrorContainer = inverseErrorContainerLight,
                inverseOnErrorContainer = inverseOnErrorContainerLight,
                label = labelLight,
                // noteDefault = surfaceContainerLight,
                // noteRed = noteRedLight,
                // noteOrange = noteOrangeLight,
                // noteYellow = noteYellowLight,
                // noteGreen = noteGreenLight,
                // noteBlue = noteBlueLight,
                // noteTurquoise = noteTurquoiseLight,
                // notePurple = notePurpleLight,
                // invertNoteRed = invertNoteRedLight,
                // invertNoteOrange = invertNoteOrangeLight,
                // invertNoteYellow = invertNoteYellowLight,
                // invertNoteGreen = invertNoteGreenLight,
                // invertNoteBlue = invertNoteBlueLight,
                // invertNoteTurquoise = invertNoteTurquoiseLight,
                // invertNotePurple = invertNotePurpleLight
            )
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)

            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalExtendedMaterialColorScheme provides extendedColorScheme,
        LocalIsDarkTheme provides darkTheme
    ) {
        MaterialTheme(
            colorScheme = baseColorScheme, typography = Typography, motionScheme = expressive(), content = content
        )
    }
}
