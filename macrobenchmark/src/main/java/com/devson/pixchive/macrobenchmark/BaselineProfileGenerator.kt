package com.devson.pixchive.macrobenchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() {
        baselineProfileRule.collect(
            packageName = "com.devson.pixchive",
            profileBlock = {
                // Start the app directly
                pressHome()
                startActivityAndWait()

                // Wait for the main image grid to be visible
                // Jetpack Compose testTag maps to UI Automator resource-id
                val gridSelector = By.res("MainImageGrid")
                device.wait(Until.hasObject(gridSelector), 5000)

                // Scroll the grid
                val grid = device.findObject(gridSelector)
                if (grid != null) {
                    // Set a margin so we don't trigger system gestures
                    grid.setGestureMargin(device.displayWidth / 5)
                    
                    // Pre-compile the list items by scrolling down
                    grid.fling(Direction.DOWN)
                    device.waitForIdle()

                    // Scroll back up
                    grid.fling(Direction.UP)
                    device.waitForIdle()

                    // Click the first item to pre-compile reader screen rendering
                    val children = grid.children
                    if (children.isNotEmpty()) {
                        children[0].click()
                        device.waitForIdle()
                    }
                }
            }
        )
    }
}
