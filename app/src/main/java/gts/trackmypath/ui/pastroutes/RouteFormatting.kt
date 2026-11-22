package gts.trackmypath.ui.pastroutes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import gts.trackmypath.R
import gts.trackmypath.ui.model.RouteWithPhotoMetadataUiState
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.time.Instant
import kotlin.time.toJavaInstant

// Route text is formatted here, not in a ViewModel, so it follows the user's current language and time zone.

/** The route's name, or a title made from the day it started when the user didn't name it. */
@Composable
@ReadOnlyComposable
internal fun routeTitle(route: RouteWithPhotoMetadataUiState): String =
    route.displayName ?: stringResource(
        R.string.route_fallback_title,
        localizedDate(instant = route.createdAt, style = FormatStyle.MEDIUM)
    )

@Composable
@ReadOnlyComposable
internal fun routeDate(route: RouteWithPhotoMetadataUiState): String =
    stringResource(R.string.route_date, localizedDate(instant = route.createdAt, style = FormatStyle.SHORT))

@Composable
@ReadOnlyComposable
private fun localizedDate(instant: Instant, style: FormatStyle): String =
    DateTimeFormatter.ofLocalizedDate(style)
        .withLocale(LocalConfiguration.current.locales[0])
        .format(instant.toJavaInstant().atZone(ZoneId.systemDefault()))
