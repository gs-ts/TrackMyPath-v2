@file:Suppress("PropertyName")

package gts.trackmypath.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = ColorLightTokens.Primary,
    onPrimary = ColorLightTokens.OnPrimary,
    primaryContainer = ColorLightTokens.PrimaryContainer,
    onPrimaryContainer = ColorLightTokens.OnPrimaryContainer,
    inversePrimary = ColorLightTokens.InversePrimary,
    secondary = ColorLightTokens.Secondary,
    onSecondary = ColorLightTokens.OnSecondary,
    secondaryContainer = ColorLightTokens.SecondaryContainer,
    onSecondaryContainer = ColorLightTokens.OnSecondaryContainer,
    tertiary = ColorLightTokens.Tertiary,
    onTertiary = ColorLightTokens.OnTertiary,
    tertiaryContainer = ColorLightTokens.TertiaryContainer,
    onTertiaryContainer = ColorLightTokens.OnTertiaryContainer,
    background = ColorLightTokens.Background,
    onBackground = ColorLightTokens.OnBackground,
    surface = ColorLightTokens.Surface,
    onSurface = ColorLightTokens.OnSurface,
    surfaceVariant = ColorLightTokens.SurfaceVariant,
    onSurfaceVariant = ColorLightTokens.OnSurfaceVariant,
    surfaceTint = ColorLightTokens.Primary,
    inverseSurface = ColorLightTokens.InverseSurface,
    inverseOnSurface = ColorLightTokens.InverseOnSurface,
    error = ColorLightTokens.Error,
    onError = ColorLightTokens.OnError,
    errorContainer = ColorLightTokens.ErrorContainer,
    onErrorContainer = ColorLightTokens.OnErrorContainer,
    outline = ColorLightTokens.Outline,
    outlineVariant = ColorLightTokens.OutlineVariant,
    scrim = ColorLightTokens.Scrim,
    surfaceBright = ColorLightTokens.SurfaceBright,
    surfaceDim = ColorLightTokens.SurfaceDim,
    surfaceContainer = ColorLightTokens.SurfaceContainer,
    surfaceContainerHigh = ColorLightTokens.SurfaceContainerHigh,
    surfaceContainerHighest = ColorLightTokens.SurfaceContainerHighest,
    surfaceContainerLow = ColorLightTokens.SurfaceContainerLow,
    surfaceContainerLowest = ColorLightTokens.SurfaceContainerLowest,
    primaryFixed = ColorLightTokens.PrimaryFixed,
    primaryFixedDim = ColorLightTokens.PrimaryFixedDim,
    onPrimaryFixed = ColorLightTokens.OnPrimaryFixed,
    onPrimaryFixedVariant = ColorLightTokens.OnPrimaryFixedVariant,
    secondaryFixed = ColorLightTokens.SecondaryFixed,
    secondaryFixedDim = ColorLightTokens.SecondaryFixedDim,
    onSecondaryFixed = ColorLightTokens.OnSecondaryFixed,
    onSecondaryFixedVariant = ColorLightTokens.OnSecondaryFixedVariant,
    tertiaryFixed = ColorLightTokens.TertiaryFixed,
    tertiaryFixedDim = ColorLightTokens.TertiaryFixedDim,
    onTertiaryFixed = ColorLightTokens.OnTertiaryFixed,
    onTertiaryFixedVariant = ColorLightTokens.OnTertiaryFixedVariant,
)

private val DarkColorScheme = darkColorScheme(
    primary = ColorDarkTokens.Primary,
    onPrimary = ColorDarkTokens.OnPrimary,
    primaryContainer = ColorDarkTokens.PrimaryContainer,
    onPrimaryContainer = ColorDarkTokens.OnPrimaryContainer,
    inversePrimary = ColorDarkTokens.InversePrimary,
    secondary = ColorDarkTokens.Secondary,
    onSecondary = ColorDarkTokens.OnSecondary,
    secondaryContainer = ColorDarkTokens.SecondaryContainer,
    onSecondaryContainer = ColorDarkTokens.OnSecondaryContainer,
    tertiary = ColorDarkTokens.Tertiary,
    onTertiary = ColorDarkTokens.OnTertiary,
    tertiaryContainer = ColorDarkTokens.TertiaryContainer,
    onTertiaryContainer = ColorDarkTokens.OnTertiaryContainer,
    background = ColorDarkTokens.Background,
    onBackground = ColorDarkTokens.OnBackground,
    surface = ColorDarkTokens.Surface,
    onSurface = ColorDarkTokens.OnSurface,
    surfaceVariant = ColorDarkTokens.SurfaceVariant,
    onSurfaceVariant = ColorDarkTokens.OnSurfaceVariant,
    surfaceTint = ColorDarkTokens.Primary,
    inverseSurface = ColorDarkTokens.InverseSurface,
    inverseOnSurface = ColorDarkTokens.InverseOnSurface,
    error = ColorDarkTokens.Error,
    onError = ColorDarkTokens.OnError,
    errorContainer = ColorDarkTokens.ErrorContainer,
    onErrorContainer = ColorDarkTokens.OnErrorContainer,
    outline = ColorDarkTokens.Outline,
    outlineVariant = ColorDarkTokens.OutlineVariant,
    scrim = ColorDarkTokens.Scrim,
    surfaceBright = ColorDarkTokens.SurfaceBright,
    surfaceDim = ColorDarkTokens.SurfaceDim,
    surfaceContainer = ColorDarkTokens.SurfaceContainer,
    surfaceContainerHigh = ColorDarkTokens.SurfaceContainerHigh,
    surfaceContainerHighest = ColorDarkTokens.SurfaceContainerHighest,
    surfaceContainerLow = ColorDarkTokens.SurfaceContainerLow,
    surfaceContainerLowest = ColorDarkTokens.SurfaceContainerLowest,
    primaryFixed = ColorDarkTokens.PrimaryFixed,
    primaryFixedDim = ColorDarkTokens.PrimaryFixedDim,
    onPrimaryFixed = ColorDarkTokens.OnPrimaryFixed,
    onPrimaryFixedVariant = ColorDarkTokens.OnPrimaryFixedVariant,
    secondaryFixed = ColorDarkTokens.SecondaryFixed,
    secondaryFixedDim = ColorDarkTokens.SecondaryFixedDim,
    onSecondaryFixed = ColorDarkTokens.OnSecondaryFixed,
    onSecondaryFixedVariant = ColorDarkTokens.OnSecondaryFixedVariant,
    tertiaryFixed = ColorDarkTokens.TertiaryFixed,
    tertiaryFixedDim = ColorDarkTokens.TertiaryFixedDim,
    onTertiaryFixed = ColorDarkTokens.OnTertiaryFixed,
    onTertiaryFixedVariant = ColorDarkTokens.OnTertiaryFixedVariant,
)

@Composable
fun TrackMyPathV2Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content,
    )
}
