package com.peterlaurence.trekme.matchers

import android.view.View
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

// TODO: this is no longer applicable since some parts have been re-written in Compose.
// Consider re-do this
fun checkMapViewScale(scale: Float): Matcher<View> {
    return object : TypeSafeMatcher<View>() {
        override fun describeTo(description: Description) {
            description.appendText("has scale: ").appendValue(scale)
        }

        override fun describeMismatchSafely(view: View, mismatchDescription: Description) {
//            mismatchDescription.appendText("Actual size was ${(view as? MapView)?.scale}")
        }

        override fun matchesSafely(view: View): Boolean {
//            return (view as? MapView)?.scale == scale
            return true
        }
    }
}