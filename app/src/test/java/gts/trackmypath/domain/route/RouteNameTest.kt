package gts.trackmypath.domain.route

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RouteNameTest {

    @Test
    fun `normalizeRouteName trims surrounding whitespace`() {
        assertEquals(expected = "Morning walk", actual = normalizeRouteName("  Morning walk \n"))
    }

    @Test
    fun `normalizeRouteName rejects a blank name`() {
        assertNull(actual = normalizeRouteName(""))
        assertNull(actual = normalizeRouteName("   "))
    }

    @Test
    fun `normalizeRouteName accepts up to ROUTE_NAME_MAX_LENGTH characters after trimming`() {
        val longestName = "a".repeat(ROUTE_NAME_MAX_LENGTH)

        assertEquals(expected = longestName, actual = normalizeRouteName(" $longestName "))
        assertNull(actual = normalizeRouteName(longestName + "a"))
    }
}
