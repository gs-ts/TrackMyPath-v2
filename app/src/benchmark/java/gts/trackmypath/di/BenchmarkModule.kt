package gts.trackmypath.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import gts.trackmypath.domain.route.FakeObserveRouteWithPhotoMetadataUseCase
import gts.trackmypath.domain.route.ObserveRouteWithPhotoMetadataContract

/**
 * Provides the Fake implementation of [ObserveRouteWithPhotoMetadataContract] specifically
 * for Macrobenchmark tests.
 *
 * ARCHITECTURE NOTE: WHY ARE WE USING SOURCE SETS INSTEAD OF @TestInstallIn?
 * * 1. Macrobenchmark Constraints: Unlike standard Espresso UI tests, Macrobenchmarks run in a
 * completely separate process against a fully compiled, production-like APK. They do not
 * use the Hilt testing framework, meaning Hilt completely ignores `@TestInstallIn`.
 * * 2. The Duplicate Binding Problem: To inject this Fake, we must rely on Gradle build types.
 * However, Gradle merges source sets (e.g., `main` + `benchmark`). If the real
 * `RouteDomainModule` lived in `src/main`, the compiler would merge both the real and fake
 * modules together, causing Hilt to crash with a `DuplicateBindings` error.
 * * 3. The Source Set Split: To fix this, the real `RouteDomainModule` was removed from `src/main`
 * and placed into `src/debug` and `src/release`. This physically hides the real module from
 * the `benchmark` compiler, ensuring Hilt only finds this Fake module during performance tests.
 */
@Module
@InstallIn(ViewModelComponent::class)
abstract class BenchmarkModule {
    @Binds
    abstract fun bindObserveRouteWithPhotoMetadataContract(
        impl: FakeObserveRouteWithPhotoMetadataUseCase
    ): ObserveRouteWithPhotoMetadataContract
}
