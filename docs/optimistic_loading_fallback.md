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

4. **Synchronized Refresh**: The repository uses an "In-Flight Request" map combined with a database check to solve two race conditions:
   - **Concurrent Calls**: If 10 list items fail simultaneously for the same Place, they are all routed to wait for a single active network call. Only *one* request goes to Google.
   - **Late Arrivals**: If a UI component requests a refresh *after* the first network call has already finished, the repository checks the database first. It sees that the database no longer contains the expired URL, realizes it was already updated by the previous call, and instantly returns the new URL without hitting the network.

5. **Obsolete Place ID Cleanup**: If Google Places API returns `9013 NOT_FOUND` when attempting to refresh the URL (because the Place ID is no longer valid), `GooglePlacesClient` throws a `PlaceIdInvalidException`. The repository catches this, deletes the obsolete photo record from the Room database, and returns `null`. This breaks the infinite refresh loop and keeps the database clean.

### Why We Chose It
- **Zero Wasted API Calls**: We only hit the Google API if the image is both missing from disk *and* the local URL has expired.
- **Zero Wasted Bandwidth**: By stabilizing the cache keys, Coil never re-downloads image bytes it already has.
- **Thread Safety & No Memory Leaks**: The In-Flight Request map completely removes duplicate network calls. When the network call finishes, it is immediately removed from the map, ensuring the app consumes zero memory for locks when idle.
- **Automatic Self-Cleaning Database**: Obsolete Place IDs are automatically purged when Google rejects them, preventing repetitive failed network requests.


## Late Arrival Scenario Example

This scenario explains how the **Step 1 (Database Check)** in `refreshAndSavePhotoUri` prevents redundant network calls when multiple UI components react at slightly different times.

### The Setup
Imagine you have a photo of the **"Eiffel Tower"** in your current route.
*   **The Database:** Currently stores `URL_OLD`.
*   **The Reality:** `URL_OLD` has just expired on Google's servers.

---

### Step 1: The "Early Bird" (First Failure)
You open your **Route History** list. A small thumbnail of the Eiffel Tower tries to load `URL_OLD`.

1.  **Failure:** The load fails with an `HTTP 403 Forbidden`.
2.  **Request:** The UI calls `refreshAndSavePhotoUri(placeId = "EiffelTower", expiredPhotoUri = "URL_OLD")`.
3.  **Action:** The repository starts a network call to Google, gets `URL_NEW`, and **saves `URL_NEW` to the database.**
4.  **Cleanup:** The "Sign-up Sheet" (`activePhotoUriFetches`) is cleared because the job is done.

### Step 2: The "Late Arrival" (Second Failure)
Imagine you also have a **Detail View** open for that same route in the background, or you scroll very fast back to that same item.

1.  **Cached Memory:** The Detail View still has `URL_OLD` in its memory from the last time it was drawn. It tries to load it a split-second **after** the first request (the thumbnail) already finished.
2.  **Failure:** It fails and calls `refreshAndSavePhotoUri(placeId = "EiffelTower", expiredPhotoUri = "URL_OLD")`.
3.  **The "Late Arrival" Logic:**
   *   The Repository looks at the Database and sees `URL_NEW`.
   *   It looks at the request and sees the UI is providing `expiredPhotoUri = "URL_OLD"`.
   *   **The Check:** `currentPhotoUriInDb ("URL_NEW") != expiredPhotoUri ("URL_OLD")`.

> [!NOTE]
> The Repository realizes: *"I already fixed this for someone else a second ago; here is the new version, don't bother Google again."*

---

### The Result
Instead of calling Google's API a second time, the repository immediately returns `URL_NEW` from the Database.

#### Why this is a "Race Condition"
Without the Database check, this is what would happen:
1.  Thumbnail refreshes the URL.
2.  Detail View doesn't know the Thumbnail just finished.
3.  Detail View starts **another** expensive API call to Google.
4.  You just wasted your API quota on two identical requests.

> [!TIP]
> **Summary:** The "Late Arrival" check ensures that if one coroutine finishes a refresh, all other coroutines that arrive even a millisecond late are immediately satisfied by the database result without hitting the network.


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
