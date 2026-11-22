package gts.trackmypath.data.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

object FilterPreferencesSerializer : Serializer<FilterPreferences> {
    override val defaultValue: FilterPreferences = FilterPreferences()

    // Skips fields this version doesn't know, so removing or renaming a field later
    // doesn't make saved files unreadable.
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun readFrom(input: InputStream): FilterPreferences {
        try {
            return json.decodeFromString(
                input.readBytes().decodeToString()
            )
        } catch (exception: SerializationException) {
            throw CorruptionException("Unable to read FilterPreferences", exception)
        }
    }

    override suspend fun writeTo(t: FilterPreferences, output: OutputStream) {
        output.write(json.encodeToString(value = t).encodeToByteArray())
    }
}
