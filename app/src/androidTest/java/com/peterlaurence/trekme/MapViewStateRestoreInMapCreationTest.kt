package com.peterlaurence.trekme


import android.view.View
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.*
import ovh.plrapps.mapview.MapView
import com.peterlaurence.trekme.matchers.checkMapViewScale
import com.peterlaurence.trekme.matchers.childAtPosition
import org.hamcrest.Matcher
import org.hamcrest.Matchers.*
import org.junit.Test

/**
 * When creating a map, some providers provide the ability to change of layer. When changing of layer,
 * the current [MapView] is destroyed and re-created using a different configuration. However, a
 * specific mechanism is set in place to restore the previous state (scale and scroll).
 * This test aims at checking that [MapView] state restoration when changing of layer.
 *
 * @author P.Laurence
 */
class MapViewStateRestoreInMapCreationTest : AbstractTest() {
    private val scaleTest = 6.1035156E-5f

    @Test
    fun mainTest() {
        Thread.sleep(1000)

        val appCompatImageButton = onView(
                allOf(withContentDescription("Open navigation drawer"),
                        childAtPosition(
                                allOf(withId(R.id.toolbar),
                                        childAtPosition(
                                                withClassName(`is`("com.google.android.material.appbar.AppBarLayout")),
                                                0)),
                                1),
                        isDisplayed()))
        appCompatImageButton.perform(click())

        /* Wait before navigating to map creation */
        Thread.sleep(1000)

        val navigationMenuItemView = onView(
                allOf(withId(R.id.nav_create),
                        childAtPosition(
                                allOf(withId(R.id.design_navigation_view),
                                        childAtPosition(
                                                withId(R.id.nav_view),
                                                0)),
                                2),
                        isDisplayed()))
        navigationMenuItemView.perform(click())

        /* Wait before selecting a map provider */
        Thread.sleep(1000)

        /* Select IGN France provider */
        val recyclerView = onView(
                allOf(withId(R.id.recyclerview_map_create),
                        childAtPosition(
                                withClassName(`is`("androidx.constraintlayout.widget.ConstraintLayout")),
                                0)))
        recyclerView.perform(actionOnItemAtPosition<ViewHolder>(1, click()))

        Thread.sleep(1000)

        /* Check that the MapView is displayed */
        onView(withId(R.id.tileview_ign_id)).check(matches(isDisplayed()))

        /* Set a predefined scale to the MapView */
        onView(withId(R.id.tileview_ign_id)).perform(MapViewsScalingAction(scaleTest))

        /* Wait a few secs for the tiles to appear */
        Thread.sleep(3000)

        /* Now, click on layer button */
        val actionMenuItemView = onView(
                allOf(withId(R.id.map_layer_menu_id), withContentDescription("Choose area"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.toolbar),
                                        1),
                                1),
                        isDisplayed()))
        actionMenuItemView.perform(click())

        Thread.sleep(1000)

        /* Choose another layer */
        val appCompatCheckedTextView = onData(anything())
                .inAdapterView(allOf(withId(R.id.select_dialog_listview),
                        childAtPosition(
                                withId(R.id.contentPanel),
                                0)))
                .atPosition(2)
        appCompatCheckedTextView.perform(click())

        Thread.sleep(1000)

        /* Confirm the selection */
        val appCompatButton = onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.buttonPanel),
                                        0),
                                3)))
        appCompatButton.perform(scrollTo(), click())

        Thread.sleep(2000)

        /* Check that the new displayed map has the same scale as before */
        onView(withId(R.id.tileview_ign_id)).check(matches(checkMapViewScale(scaleTest)))
    }
}

private class MapViewsScalingAction(private val scale: Float) : ViewAction {
    override fun getConstraints(): Matcher<View> {
        return isAssignableFrom(MapView::class.java)
    }

    override fun getDescription(): String {
        return "Scaling MapView"
    }

    override fun perform(uiController: UiController?, view: View?) {
        val mapView = view as? MapView
        mapView?.scale = scale
    }
}
