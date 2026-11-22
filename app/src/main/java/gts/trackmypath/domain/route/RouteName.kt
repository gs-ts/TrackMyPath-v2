package gts.trackmypath.domain.route

/** The longest name the user can give a route. */
const val ROUTE_NAME_MAX_LENGTH = 50

/**
 * Returns the name to save: [raw] trimmed, or null when it's blank or longer than [ROUTE_NAME_MAX_LENGTH].
 *
 * A route name is only shown to the user, so it stays a `String` instead of a value class.
 */
fun normalizeRouteName(raw: String): String? =
    raw.trim().takeIf { it.isNotEmpty() && it.length <= ROUTE_NAME_MAX_LENGTH }
