package gts.trackmypath.data.database

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Recovers routes that were being tracked when the app crashed or Android killed it.
 *
 * Room calls [onOpen] when the app opens the database, before any query runs. No route is being tracked
 * at that point: tracking starts after the database opens, and it ends with the process. So a route that
 * isn't finished was interrupted. If it has photos, it's kept and finished at its last photo. If it has
 * none, it's deleted.
 *
 * If tracking ever resumes after the process restarts, this must skip the resumed route.
 */
object RecoverInterruptedRoutesCallback : RoomDatabase.Callback() {

    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        db.execSQL(
            """
            DELETE FROM routes
            WHERE route_finished_at IS NULL
            AND NOT EXISTS (SELECT 1 FROM photo_metadata WHERE photo_metadata.route_id = routes.routeId)
            """.trimIndent()
        )
        // After the DELETE, every unfinished route has at least one photo.
        db.execSQL(
            """
            UPDATE routes
            SET route_finished_at = (
                SELECT MAX(photo_created_at) FROM photo_metadata WHERE photo_metadata.route_id = routes.routeId
            )
            WHERE route_finished_at IS NULL
            """.trimIndent()
        )
    }
}
