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

        val recyclerView = onView(withId(R.id.map_list))
        recyclerView.perform(actionOnItemAtPosition<ViewHolder>(0, click()))

        Thread.sleep(1000)

        /* Check that the MapView is displayed */
        onView(withId(R.id.tileview_id)).check(ViewAssertions.matches(isDisplayed()))

        /* Center on position */
        val centerOnPosFab = onView(
                allOf(withId(R.id.fab_position), withContentDescription("Center on position button"),
                        childAtPosition(
                                allOf(withId(R.id.map_view_frgmt_layout),
                                        childAtPosition(
                                                withId(R.id.nav_host_fragment),
                                                0)),
                                2),
                        isDisplayed()))
        centerOnPosFab.perform(click())

        Thread.sleep(1000)

        /* At app install, max scale is 2f and zoom is 50% by default. So the scale should be 1f
         * when centering on position */
        onView(withId(R.id.tileview_id)).check(ViewAssertions.matches(checkMapViewScale(1f)))

        /* Open the menu */
        val appCompatImageButton = onView(
                allOf(withContentDescription("Open navigation drawer"),
                        childAtPosition(
                                allOf(withId(R.id.toolbar),
                                        childAtPosition(
                                                withClassName(`is`("com.google.android.material.appbar.AppBarLayout")),
                                                0)),
                                0),
                        isDisplayed()))
        appCompatImageButton.perform(click())

        Thread.sleep(1000)

        /* Go to settings */
        val navigationMenuItemView = onView(
                allOf(withId(R.id.nav_settings),
                        childAtPosition(
                                allOf(withId(R.id.design_navigation_view),
                                        childAtPosition(
                                                withId(R.id.nav_view),
                                                0)),
                                6),
                        isDisplayed()))
        navigationMenuItemView.perform(click())

        Thread.sleep(1000)

        /* Click on max scale setting */
        val maxScaleSetting = onView(
                allOf(withId(R.id.recycler_view),
                        childAtPosition(
                                withId(android.R.id.list_container),
                                0)))
        maxScaleSetting.perform(actionOnItemAtPosition<ViewHolder>(6, click()))

        Thread.sleep(1000)

        /* Select scale 8 */
        val appCompatCheckedTextView = onData(anything())
                .inAdapterView(allOf(withId(R.id.select_dialog_listview),
                        childAtPosition(
                                withId(R.id.contentPanel),
                                0)))
                .atPosition(2)
        appCompatCheckedTextView.perform(click())

        Thread.sleep(1000)

        /* Go back to the map */
        pressBack()

        Thread.sleep(1000)

        /* Center on position */
        centerOnPosFab.perform(click())

        Thread.sleep(1000)

        /* Since the max scale is 8f and zoom is still 50%, the scale should be 4f */
        onView(withId(R.id.tileview_id)).check(ViewAssertions.matches(checkMapViewScale(4f)))

        /* Open the menu */
        appCompatImageButton.perform(click())

        Thread.sleep(1000)

        /* Go to settings */
        navigationMenuItemView.perform(click())

        Thread.sleep(1000)

        /* Select max scale setting */
        maxScaleSetting.perform(actionOnItemAtPosition<ViewHolder>(6, click()))

        Thread.sleep(1000)

        /* Select max scale 2f */
        val appCompatCheckedTextView2 = onData(anything())
                .inAdapterView(allOf(withId(R.id.select_dialog_listview),
                        childAtPosition(
                                withId(R.id.contentPanel),
                                0)))
                .atPosition(0)
        appCompatCheckedTextView2.perform(click())

        Thread.sleep(1000)

        /* Go back to the map */
        pressBack()

        /* Since previous scale was 4f, and the max scale is now 2f, the current scale should be 2f,
         * because we haven't centered on position yet */
        onView(withId(R.id.tileview_id)).check(ViewAssertions.matches(checkMapViewScale(2f)))

        /* Center on position */
        centerOnPosFab.perform(click())

        Thread.sleep(1000)

        /* Finally, after centering on position, since the zoom is 50% and max scale is 2f, the
         * scale should be 1f */
        onView(withId(R.id.tileview_id)).check(ViewAssertions.matches(checkMapViewScale(1f)))
    }
}
