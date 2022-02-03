package com.peterlaurence.trekme


import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.*
import com.peterlaurence.trekme.matchers.checkMapViewScale
import com.peterlaurence.trekme.matchers.childAtPosition
import org.hamcrest.Matchers.*
import org.junit.Test

/**
 * This test suite checks scale values of the [MapView] when changing of max scale in the app settings.
 * More precisely, it tests the following scenario:
 * 1. User opens and navigates to the first map, while the max scale is at its default value (2f),
 * 2. User presses the center-on-position button. The scale should be 1f,
 * 3. User navigates to app settings and changes the max scale to 8f,
 * 4. User navigates back the map, and presses the center-on-position button. The scale should be 4f,
 * 5. User navigates again to app settings, and reset the max scale to its default (2f),
 * 6. User navigates back the map, and presses the center-on-position button. The scale should be 1f.
 *
 * @author P.Laurence on 2021/01/30
 */
class MaxScaleTest : AbstractTest() {
    @Test
    fun maxScaleTest() {
        Thread.sleep(1000)

        // TODO: now that the map list is built in compose, this test is broken. Find a way to mix
        // compose ui test and views test?
//        val recyclerView = onView(withId(R.id.map_list))
//        recyclerView.perform(actionOnItemAtPosition<ViewHolder>(0, click()))
    }
}
