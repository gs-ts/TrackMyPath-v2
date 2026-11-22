package gts.trackmypath.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import gts.trackmypath.data.database.photometadata.PhotoMetadataEntity
import gts.trackmypath.data.database.route.RouteEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecoverInterruptedRoutesCallbackTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val dbName = "test-track-my-path-db"

    @Before
    fun setup() {
        // clean up any old test database before starting
        context.deleteDatabase(dbName)
    }

    @After
    fun teardown() {
        // clean up test database after test
        context.deleteDatabase(dbName)
    }

    @Test
    fun `keeps a finished route as it is`() = runBlocking {
        val routeId = withDatabase {
            routeDao().insertRoute(RouteEntity(displayName = null, createdAt = 1_000L, finishedAt = 2_000L))
        }

        val route = withDatabase(RecoverInterruptedRoutesCallback) { routeDao().getRouteById(routeId) }

        assertNotNull("A finished route should be kept", route)
        assertEquals(2_000L, route?.finishedAt)
    }

    @Test
    fun `keeps an interrupted route with photos and finishes it at its last photo`() = runBlocking {
        val routeId = withDatabase {
            val id = routeDao().insertRoute(RouteEntity(displayName = null, createdAt = 1_000L))
            photoMetadataDao().insert(photo(routeId = id, placeId = "place-1", createdAt = 3_000L))
            photoMetadataDao().insert(photo(routeId = id, placeId = "place-2", createdAt = 2_000L))
            id
        }

        val route = withDatabase(RecoverInterruptedRoutesCallback) { routeDao().getRouteById(routeId) }

        assertNotNull("An interrupted route with photos should be kept", route)
        assertEquals(3_000L, route?.finishedAt)
    }

    @Test
    fun `deletes an interrupted route without photos`() = runBlocking {
        val routeId = withDatabase {
            routeDao().insertRoute(RouteEntity(displayName = null, createdAt = 1_000L))
        }

        val route = withDatabase(RecoverInterruptedRoutesCallback) { routeDao().getRouteById(routeId) }

        assertNull("An interrupted route without photos should be deleted", route)
    }

    // Opens the database, runs block, and closes it, so the next call opens it again and runs onOpen.
    private suspend fun <T> withDatabase(
        callback: RoomDatabase.Callback? = null,
        block: suspend AppDatabase.() -> T
    ): T {
        val database = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .apply { if (callback != null) addCallback(callback) }
            .build()
        return try {
            database.block()
        } finally {
            database.close()
        }
    }

    private fun photo(routeId: Long, placeId: String, createdAt: Long) = PhotoMetadataEntity(
        routeId = routeId,
        placeId = placeId,
        createdAt = createdAt,
        displayName = null,
        location = PhotoMetadataEntity.Location(latitude = 0.0, longitude = 0.0),
        photoUri = "https://example.com/$placeId.jpg",
        googleMapsUri = null,
        generativeSummary = null,
        neighborhoodSummary = null,
    )
}
