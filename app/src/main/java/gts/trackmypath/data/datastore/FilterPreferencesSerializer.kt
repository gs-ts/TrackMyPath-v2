package gts.trackmypath.data.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

object FilterPreferencesSerializer : Serializer<FilterPreferences> {
    override val defaultValue: FilterPreferences = FilterPreferences()

    override suspend fun readFrom(input: InputStream): FilterPreferences {
        try {
            return Json.decodeFromString(
                input.readBytes().decodeToString()
            )
        } catch (exception: SerializationException) {
            throw CorruptionException("Unable to read FilterPreferences", exception)
        }
    }

    override suspend fun writeTo(t: FilterPreferences, output: OutputStream) {
        output.write(Json.encodeToString(value = t).encodeToByteArray())
    }
}
