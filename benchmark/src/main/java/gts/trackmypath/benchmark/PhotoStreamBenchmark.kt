package gts.trackmypath.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Benchmark, which will execute on an Android device.
 * It captures the FrameTimingMetric:
 * https://developer.android.com/topic/performance/benchmarking/macrobenchmark-metrics#frame-timing
 *
 * frameCount — How many frames were drawn per iteration.
 * more frames = more scrolling activity happened.
 *
 * frameDurationCpuMs — How long each frame took the CPU to produce
 * the critical number here is 16.6ms at 60fps.
 *
 * frameOverrunMs — Did frames miss their deadline?
 * Negative = frame beat its deadline (invisible to user).
 * Positive = frame missed its deadline (potential jank).
 *
 * -------------------------------------------------------------------------------------------------
 *
 * The benchmark measures frame rendering performance when a new photo arrives and the list
 * automatically scrolls back to the top while the user is actively scrolling down.
 *
 * This simulates the real app behavior: user is walking, scrolling through the photo
 * stream, and every ~50 meters a new photo is fetched and prepended to the top,
 * triggering animateScrollToItem(0) mid-scroll.
 *
 * What we're looking for in the results:
 * - frameDurationCpuMs P99 < 16ms (frame budget at 60fps)
 * - frameOverrunMs P99 negative or close to 0 (frames beating their deadline)
 *
 * -------------------------------------------------------------------------------------------------
 *
 * Results with List in ActivePathViewModel and ActivePathScreen:
 * frameCount           min 2,129.0,   median 2,226.0,   max 2,277.0
 * frameDurationCpuMs   P50      3.6,   P90      4.8,   P95      5.3,   P99      8.8
 * frameOverrunMs       P50     -3.4,   P90     -2.2,   P95     -0.9,   P99      2.3
 *
 * Results with PersistentList + ImmutableList in ActivePathViewModel and ActivePathScreen:
 * frameCount           min 2,145.0,   median 2,215.0,   max 2,242.0
 * frameDurationCpuMs   P50      3.6,   P90      4.8,   P95      5.1,   P99      6.4
 * frameOverrunMs       P50     -3.4,   P90     -2.3,   P95     -2.0,   P99     -0.4
 *
 * Focus on P99 since that's where jank lives:
 * The critical one is P99 frameOverrunMs.
 * With List, the worst 1% of frames are missing their deadline by 2.3ms, those are real dropped frames, visible jank.
 * With ImmutableList, that same P99 is -0.4ms, meaning even the worst frames are still beating the deadline.
 *
 */
@RunWith(AndroidJUnit4::class)
class PhotoStreamBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun `benchmark frame timing metric when new photo fetched during scrolling`() {
        benchmarkRule.measureRepeated(
            packageName = "gts.trackmypath",
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Full(),
            startupMode = StartupMode.COLD, // kill and restart the app before each iteration for a clean slate
            iterations = 10,
            setupBlock = {
                // grant permissions upfront so no permission dialogs interrupt the benchmark
                val pkg = "gts.trackmypath"
                device.executeShellCommand("pm grant $pkg android.permission.ACCESS_FINE_LOCATION")
                device.executeShellCommand("pm grant $pkg android.permission.ACCESS_COARSE_LOCATION")

                if (android.os.Build.VERSION.SDK_INT >= 33) {
                    device.executeShellCommand("pm grant $pkg android.permission.POST_NOTIFICATIONS")
                }

                // force stop ensures a truly cold start, no leftover state from previous iteration
                device.executeShellCommand("am force-stop $pkg")
                pressHome()
            }
        ) {
            startActivityAndWait() // launch the app and wait for the first frame to be drawn
            device.waitForIdle()

            val startButton = device.wait(
                Until.findObject(By.descContains("Start path tracking")), 10_000
            ) ?: throw AssertionError("Start button not found")
            startButton.click()

            val list = device.wait(
                Until.findObject(By.res("photo_list_scrollable")), 15_000
            ) ?: throw AssertionError("photo_list_scrollable not found")

            // keep gestures away from screen edges to avoid triggering system navigation
            val margin = (device.displayHeight * 0.2).toInt()
            list.setGestureMargin(margin)

            val bounds = list.visibleBounds
            val centerX = bounds.centerX()
            val startY = bounds.bottom - margin
            val endY = bounds.top + margin

            // Fling down quickly to get deep into the list (~item 80+).
            // Flings cover much more distance than swipes so we can reach far down
            // before the first new photo arrives at ~8 seconds.
            repeat(4) {
                list.fling(Direction.DOWN)
            }
            device.waitForIdle()

            // Swipe continuously while new photos arrive every 5 seconds.
            // Each new photo triggers animateScrollToItem(0) which fights against the user's downward scrolling.
            // This contention is what FrameTimingMetric captures.
            repeat(100) {
                // step=10 means a very fast, forceful swipe (~50ms gesture duration)
                device.swipe(centerX, startY, centerX, endY, 10)
                device.waitForIdle()
            }
        }
    }
}
