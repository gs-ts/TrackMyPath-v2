package gts.trackmypath.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import gts.trackmypath.data.database.route.RouteEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RouteDaoTest {

    private lateinit var database: AppDatabase

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .build()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `finishing a route again keeps the first finish time`() = runBlocking {
        val routeDao = database.routeDao()
        val routeId = routeDao.insertRoute(RouteEntity(displayName = null, createdAt = 1_000L))

        routeDao.finishRouteById(routeId = routeId, finishedAt = 2_000L, metadata = emptyMap())
        routeDao.finishRouteById(routeId = routeId, finishedAt = 3_000L, metadata = emptyMap()) // e.g. a second Stop

        assertEquals(2_000L, routeDao.getRouteById(routeId)?.finishedAt)
    }
}
