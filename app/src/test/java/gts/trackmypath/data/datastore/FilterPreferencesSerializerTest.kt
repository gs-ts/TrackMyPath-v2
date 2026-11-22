package gts.trackmypath.data.datastore

import androidx.datastore.core.CorruptionException
import kotlinx.coroutines.test.runTest
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FilterPreferencesSerializerTest {

    @Test
    fun `reads what it writes`() = runTest {
        val preferences = FilterPreferences(selectedPlaceFilters = setOf("CULTURE", "FOOD_AND_DRINKS"))
        val output = ByteArrayOutputStream()

        FilterPreferencesSerializer.writeTo(preferences, output)

        assertEquals(expected = preferences, actual = read(output.toByteArray().decodeToString()))
    }

    @Test
    fun `ignores fields it doesn't know`() = runTest {
        // For example, a field that a later version removed or renamed.
        val preferences = read("""{"selectedPlaceFilters":["CULTURE"],"removedField":true}""")

        assertEquals(expected = FilterPreferences(selectedPlaceFilters = setOf("CULTURE")), actual = preferences)
    }

    @Test
    fun `reports unreadable data as corruption, so DataStore replaces the file`() = runTest {
        assertFailsWith<CorruptionException> { read("not json") }
    }

    private suspend fun read(json: String): FilterPreferences =
        FilterPreferencesSerializer.readFrom(ByteArrayInputStream(json.encodeToByteArray()))
}
