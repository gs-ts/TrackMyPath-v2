package gts.trackmypath.data.database.converter

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json

class MetadataConverter {

    @TypeConverter
    fun fromString(value: String): Map<String, String> {
        return Json.decodeFromString(value)
    }

    @TypeConverter
    fun fromMap(map: Map<String, String>): String {
        return Json.encodeToString(map)
    }
}
