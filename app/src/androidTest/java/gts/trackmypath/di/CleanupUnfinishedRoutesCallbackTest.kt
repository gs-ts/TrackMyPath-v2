package gts.trackmypath.di

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import gts.trackmypath.data.database.AppDatabase
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
class CleanupUnfinishedRoutesCallbackTest {

    private val dbName = "test-track-my-path-db"

    @Before
    fun setup() {
        // clean up any old test database before starting
        ApplicationProvider.getApplicationContext<android.content.Context>().deleteDatabase(dbName)
    }

    @After
    fun teardown() {
        // clean up test database after test
        ApplicationProvider.getApplicationContext<android.content.Context>().deleteDatabase(dbName)
    }

    @Test
    fun `delete unfinished routes when app is launched`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        // create database WITHOUT the callback to insert test data.
        val initialDatabase = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            dbName
        ).build()

        // insert a completed route
        val completedRouteId = initialDatabase.routeDao().insertRoute(
            RouteEntity(displayName = "Completed Route")
        )
        // insert an unfinished route (displayName is null)
        val unfinishedRouteId = initialDatabase.routeDao().insertRoute(
            RouteEntity(displayName = null)
        )

        // verify data was inserted
        assertNotNull(initialDatabase.routeDao().getRouteById(completedRouteId))
        assertNotNull(initialDatabase.routeDao().getRouteById(unfinishedRouteId))

        // close the database to force re-opening
        initialDatabase.close()

        // open the database WITH the callback attached to test onOpen behavior.
        val callbackDatabase = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            dbName
        ).addCallback(CleanupUnfinishedRoutesCallback).build()

        // accessing the database triggers the onOpen callback
        val completedRoute = callbackDatabase.routeDao().getRouteById(completedRouteId)
        val unfinishedRoute = callbackDatabase.routeDao().getRouteById(unfinishedRouteId)

        // verify that the unfinished route was deleted and the completed one was kept
        assertNotNull("Completed route should not be deleted", completedRoute)
        assertEquals("Completed Route", completedRoute?.displayName)
        
        assertNull("Unfinished route should have been deleted by the callback", unfinishedRoute)

        callbackDatabase.close()
    }
}
