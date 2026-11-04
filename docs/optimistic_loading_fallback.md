# Optimistic Loading with Fallback

## The Problem
When displaying images hosted by Google Places API, the URLs returned by the API (`awaitFetchResolvedPhotoUri`) are signed and highly ephemeral. They expire quickly (often within hours or days).

If the app caches these URLs in a local database and attempts to load them at a later time, the network request will fail with an HTTP 403 (Forbidden) or 404 (Not Found) error.

Naively fetching a fresh, signed URL *every time* the user scrolls past an image creates a catastrophic **N+1 API Problem**:
1. Massive API quota consumption (costly).
2. It breaks the image loader's (Coil) local disk cache because the URL string changes constantly, causing the app to needlessly re-download the same image bytes.
3. Severe performance degradation on scroll.

Furthermore, **Place IDs can become obsolete over time** (e.g., if a business closes or Google merges duplicate records). Attempting to refresh an expired photo for an invalid Place ID causes Google's API to return `9013 NOT_FOUND`. Without proper handling, this creates an **infinite failure loop** every time the user scrolls to that photo.

## The Solution: Optimistic Loading with Fallback

To mitigate this, we implemented the **Optimistic Load with Fallback** pattern utilizing an ImageLoader interceptor (Coil) and a single source of truth Repository.

### How It Works

1. **Stable Cache Keys**: When a request to load a `PhotoMetadata` object begins, the Interceptor constructs an `ImageRequest` that uses the ephemeral `photoUri` from the local database as the URL, but explicitly sets the `placeId` as both the `memoryCacheKey` and `diskCacheKey`.
   
   ```kotlin
   // In PhotoMetadataInterceptor.kt
   var newRequest = request.newBuilder()
       .data(data.photoUri)
       .memoryCacheKey(data.placeId) 
       .diskCacheKey(data.placeId) 
       .build()
   ```

2. **Optimistic Attempt**: The Interceptor lets the ImageLoader proceed. 
   - If the image is on the disk cache (mapped by the stable `placeId`), it loads instantly. No network calls are made.
   - If it's not on disk, Coil attempts to download the image using the database's `photoUri`.

3. **Fallback & Retry**: If the network request fails due to expiration (e.g., HTTP 403), Coil returns an `ErrorResult`. The Interceptor catches this exact scenario.
   
   ```kotlin
   if (result is ErrorResult && isUrlExpired(result.throwable)) {
       val freshUri = repository.refreshAndSavePhotoUri(data.placeId, data.photoUri)
       // ... retry ...
   }
   ```

4. **Synchronized Refresh**: The repository uses an "In-Flight Request" map combined with a time-based memory cache to solve two race conditions:
   - **Concurrent Calls**: If 10 list items fail simultaneously for the same Place, they are all routed to wait for a single active network call via the `activePhotoUriFetches` map. Only *one* request goes to Google.
     - That shared call runs in the application-wide `@ApplicationScope`, not in the coroutine of the item that started it. Coil cancels an image request when its item scrolls off screen. If that cancelled the shared call, every other item waiting on it would fail too. Instead, cancelling an item only cancels its own wait: the others still get the fresh URL, and it is still saved to the database for the next time the image is shown.
   - **Late Arrivals**: If a UI component fails to load an image a split-second *after* a refresh has already completed (due to stale UI state), the repository checks an in-memory `recentRefreshes` cache. If the image was successfully refreshed very recently (e.g., within the last 10 seconds), the repository completely skips the network call and instantly returns the URL stored in the local Room DB.

5. **Obsolete Place ID Cleanup**: If Google Places API returns `9013 NOT_FOUND` when attempting to refresh the URL (because the Place ID is no longer valid), `GooglePlacesClient` throws a `PlaceIdInvalidException`. The repository catches this, deletes the obsolete photo record from the Room database, and returns `null`. This breaks the infinite refresh loop and keeps the database clean.

### Why We Chose It
- **Zero Wasted API Calls**: We only hit the Google API if the image is both missing from disk *and* the local URL has expired.
- **Zero Wasted Bandwidth**: By stabilizing the cache keys, Coil never re-downloads image bytes it already has.
- **Thread Safety & No Memory Leaks**: The In-Flight Request map completely removes duplicate network calls. Both maps (`activePhotoUriFetches` and `recentRefreshes`) are only read and written under one `Mutex`, which is never held during a network call. When the network call finishes, it is immediately removed from the map, ensuring the app consumes zero memory for locks when idle. The repository is a `@Singleton`, so every caller shares the same maps.
- **Automatic Self-Cleaning Database**: Obsolete Place IDs are automatically purged when Google rejects them, preventing repetitive failed network requests.

## Late Arrival Scenario Example

This scenario explains how the **Time-Based Cache** in `refreshAndSavePhotoUri` prevents redundant network calls when multiple UI components react at slightly different times.

### The Setup
Imagine you have a photo of the **"Eiffel Tower"** in your current route.
*   **The Database:** Currently stores `URL_OLD`.
*   **The Reality:** `URL_OLD` has just expired on Google's servers.

---

### Step 1: The "Early Bird" (First Failure)
You open your **Route History** list. A small thumbnail of the Eiffel Tower tries to load `URL_OLD`.

1.  **Failure:** The load fails with an `HTTP 403 Forbidden`.
2.  **Request:** The UI calls `refreshAndSavePhotoUri(placeId = "EiffelTower", ...)`.
3.  **Action:** The repository starts a network call to Google, gets `URL_NEW`, and saves `URL_NEW` to the database.
4.  **Logging the Time:** The repository records exactly when this succeeded: `recentRefreshes["EiffelTower"] = System.currentTimeMillis()`.
5.  **Cleanup:** The "In-Flight" map (`activePhotoUriFetches`) is cleared.

### Step 2: The "Late Arrival" (Second Failure)
Imagine you also have a **Detail View** open for that same route in the background, or you scroll very fast back to that same item.

1.  **Cached Memory:** The Detail View still has `URL_OLD` in its memory from the last time it was drawn. It tries to load it a split-second **after** the first request (the thumbnail) already finished.
2.  **Failure:** It fails and calls `refreshAndSavePhotoUri(placeId = "EiffelTower", ...)`.
3.  **The "Late Arrival" Logic:**
   *   The Repository checks the `recentRefreshes` cache.
   *   It sees that `"EiffelTower"` was just successfully refreshed a few milliseconds ago.
   *   **The Check:** `System.currentTimeMillis() - lastRefreshTime < REFRESH_DEBOUNCE_WINDOW_MS` (e.g. 10 seconds).

> [!NOTE]
> The Repository realizes: *"I already fixed this for someone else 500 milliseconds ago. It is impossible for the URL to be expired again. Don't bother Google, just give them the URL from the DB."*

---

### The Result
Instead of calling Google's API a second time, the repository immediately returns `URL_NEW` from the Database.

#### Why this is better than comparing URL strings
Previously, we tried to detect late arrivals by comparing the old URL string with the new URL string in the DB. However, Coil's disk cache and Jetpack Compose's recomposition system can sometimes pass slightly out-of-sync string references, causing the repository to falsely block legitimate refreshes. 

By tracking **Time** instead of **Strings**, we created a bulletproof debounce mechanism that guarantees zero redundant API calls and zero false-positive blocks.

> [!TIP]
> **Summary:** The `recentRefreshes` time-based cache ensures that if one coroutine finishes a refresh, all other coroutines that arrive even a millisecond late (up to 10 seconds) are immediately satisfied by the database result without hitting the network.


## Obsolete Place ID Scenario Example

This scenario explains how the app handles dead or invalid Google Place IDs.

### The Setup
Imagine you saved a photo for a coffee shop called **"Central Perk"** six months ago.
*   **The Database:** Stores `placeId = "CentralPerk123"` and an expired `photoUri`.
*   **The Reality:** The coffee shop permanently closed, and Google removed `CentralPerk123` from Places API.

---

### Step 1: The Image Fails to Load
You scroll through your route history. Coil tries to load the image using the expired URL.

1.  **Failure:** The image load fails with an `HTTP 403 Forbidden` (URL expired).
2.  **Interceptor Action:** The interceptor catches the 403 error and calls `refreshAndSavePhotoUri(placeId = "CentralPerk123", expiredPhotoUri = ...)` to get a fresh URL.

### Step 2: Google Rejects the Refresh Request
The repository calls `GooglePlacesClient.fetchPhotoUriByPlaceId("CentralPerk123")`.

1.  **API Error:** Google's API returns `ApiException 9013: The provided Place ID is no longer valid.`
2.  **Client Exception:** `GooglePlacesClient` catches status code `9013 (PlacesStatusCodes.NOT_FOUND)` and throws `PlaceIdInvalidException`.

### Step 3: Eviction from Database
The repository catches `PlaceIdInvalidException`:

1.  **Database Cleanup:** The repository calls `photoMetadataDao.deleteByPlaceId("CentralPerk123")`.
2.  **Result:** The obsolete record is purged from the Room database, and `refreshAndSavePhotoUri` returns `null`.

> [!NOTE]
> The Repository realizes: *"Google says this Place ID no longer exists. Deleting it from our database so we never waste another network call on it."*

---

### The Result
*   The invalid item is permanently removed from local storage.
*   The UI displays a placeholder / error state gracefully.
*   Future app launches or scroll events will **never** attempt to fetch or refresh this place again, stopping the infinite failure loop and saving user data and API calls.