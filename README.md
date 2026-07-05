# Description

This application tracks your path and creates a continuous stream of photos 📸 by fetching images based on your real-time location 📍.

You can trigger the Start button ▶️ to begin tracking your journey and tap it again ⏹️ to stop. 

Once finished, you can save the route by giving it a name. All of these saved journeys are easily accessible later from the Past Routes screen 🗺️.

To customize the photo stream, you can also apply specific place filters to see exactly what interests you along the way, such as Culture 🏛️, Food & Drinks ☕, Entertainment 🎢, and more!

## Technologies used
- Kotlin
- Kotlin Coroutines
- [KotlinX](https://github.com/kotlin/kotlinx.serialization) serialization
- Compose for UI
- [Navigation 3](https://developer.android.com/guide/navigation/navigation-3)
- Hilt for dependency injection
- Room for database
- Datastore for data persistence
- [LocationServices](https://developer.android.com/develop/sensors-and-location/location/retrieve-current) for fetching user's real-time location
- [Google Places API](https://developers.google.com/maps/documentation/places/web-service/overview) for fetching places based on location
- [Coil3](https://github.com/coil-kt/coil) for image loading
- [turbine](https://github.com/cashapp/turbine) for Kotlin coroutines testing
- [UI Automator](https://developer.android.com/training/testing/other-components/ui-automator) and `benchmarkMacroJunit4` for [Macrobenchmark](https://developer.android.com/topic/performance/benchmarking/benchmarking-overview#macrobenchmark)
- [Detekt](https://github.com/detekt/detekt ) (v2) and [Compose Rules](https://mrmans0n.github.io/compose-rules/0.6.2/) for static code analysis
- [Compose Stability Analyzer](https://proandroiddev.com/compose-stability-analyzer-real-time-stability-insights-for-jetpack-compose-1399924a0a64)

## Architecture and Design Patterns
- **Pragmatic Clean Architecture**: The project is modularized by layers: `ui`, `domain`, and `data`. 
The domain layer defines the core business logic (UseCases) and abstractions (Repository interfaces), 
while the data layer implements these abstractions, strictly following the Dependency Inversion Principle.
- **MVVM/MVI**: The presentation layer is driven by ViewModels and keeping the Compose UI strictly declarative.
Inside the MVVM setup, UDF is heavily applied.
- **UseCase / Interactor Pattern**
- **Repository Pattern**
- **Adapter Pattern** (Ports and Adapters): Abstract framework dependencies behind interfaces (like `LocationServiceManager`). 
Instead of relying on mock libraries (like Mockito/MockK), custom Fake implementations are used for your unit tests.
- **Observer / Reactive Pattern**: with Kotlin Coroutines and Flows
- **State-Driven One-Off Events**: Rather than using Channel or SharedFlow to fire one-off UI events (like Snackbars or Dialogs), 
I model them purely as boolean flags in the UI State (e.g., `showSnackbarRouteSavedConfirmation = true`), 
which the UI consumes and then dispatches an Action to reset back to false. 
This aligns perfectly with the latest Jetpack Compose best practices.
- [ViewModels scoped to a composable](https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-apis#vm-api-composable): see `PlaceFilterBottomSheet`

## Unit Testing & Quality Assurance
- **ViewModel Testing**: The app uses turbine by Cash App for testing Coroutine StateFlow emissions inside ViewModels (e.g., `ActivePathViewModelTest`).
- **No Mocks Policy**: Instead of relying on mock-generation libraries (like Mockito or MockK), 
the project relies on manually created Fake implementations of dependencies (e.g., `LocationServiceManagerFake`). 
This leads to less brittle tests, faster execution, and enforces better architectural boundaries via interfaces.
- **Dispatcher Injection**: A custom JUnit Rule (`MainDispatcherRule`) combined with injected 
CoroutineDispatchers ensures predictable, synchronous test execution for coroutine-heavy code.

## Performance Benchmarking
- **Macrobenchmark & UI Automator**: The project includes a dedicated benchmark module leveraging 
`MacrobenchmarkRule` and UI Automator to measure real-device performance metrics.
- **Frame Timing Metrics**: A primary focus of the benchmarking suite is observing 
`FrameTimingMetric` (frameCount, frameDurationCpuMs, frameOverrunMs).
- **Real-world simulation**: The `PhotoStreamBenchmark` simulates complex UI behavior—rapid user scrolling 
while the app dynamically fetches and prepends new photos to the list. 
It proves with actual numbers how optimizations, such as switching from standard Kotlin 
List to PersistentList/ImmutableList, reduce P99 frame overruns and eliminate UI jank.

## Continuous Integration (CI) & Static Analysis
- **GitHub Actions**: The project enforces code quality automatically on every push to master using a dedicated android-ci.yml workflow.
- **Static Code Analysis**: Detekt (v2) is integrated into the build pipeline to catch code smells and 
maintain Kotlin formatting standards. Additionally, Compose Rules are enforced via Detekt plugins 
to ensure Jetpack Compose best practices (like proper State hoisting and Modifier ordering) are strictly followed.

### Useful links

- **One-off events**: https://proandroiddev.com/android-one-off-events-approaches-evolution-anti-patterns-add887cd0250
- **ViewModel Unit Testing**: https://marcellogalhardo.dev/posts/unit-testing-viewmodels/
- **Adapter architecture**: https://marcellogalhardo.dev/posts/no-mocks-allowed/
- **Dispatchers and EmptyCoroutineContext**: https://code.cash.app/dispatchers-unconfined 
