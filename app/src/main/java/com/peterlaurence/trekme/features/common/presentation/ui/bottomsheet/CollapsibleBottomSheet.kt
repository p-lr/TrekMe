package com.peterlaurence.trekme.features.common.presentation.ui.bottomsheet

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.SwipeableState
import androidx.compose.material.swipeable
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import kotlin.math.roundToInt

enum class States {
    EXPANDED,
    PEAKED,
    COLLAPSED
}

/**
 * A bottom-sheet which has 3 states, among which [States.COLLAPSED].
 * In [States.EXPANDED], the bottom-sheet takes half the screen height.
 * In [States.PEAKED], the bottom-sheet takes a quarter of the screen height.
 * The [content] is a lazy list body.
 *
 *
 * This is inspired by the source code of material3 BottomSheetScaffold. It relies on material's
 * [SwipeableState], which still lives in material package as of 2023/05 (it actually does not
 * depend on material stuff).
 * Using nested scroll, this component provides the same quality UX as material component.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapsibleBottomSheet(
    swipeableState: SwipeableState<States>,
    lazyListState: LazyListState = rememberLazyListState(),
    header: @Composable () -> Unit,
    content: LazyListScope.() -> Unit
) {
    BoxWithConstraints {
        val maxHeightPx = with(LocalDensity.current) {
            maxHeight.toPx()
        }
        val peakHeight = maxHeightPx / 4

        val connection = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    val delta = available.y
                    return if (delta < 0 && source == NestedScrollSource.Drag) {
                        swipeableState.performDrag(delta).toOffset()
                    } else {
                        Offset.Zero
                    }
                }

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    return if (source == NestedScrollSource.Drag) {
                        swipeableState.performDrag(available.y).toOffset()
                    } else {
                        Offset.Zero
                    }
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    return if (available.y < 0 && lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0) {
                        swipeableState.performFling(available.y)
                        available
                    } else {
                        Velocity.Zero
                    }
                }

                override suspend fun onPostFling(
                    consumed: Velocity,
                    available: Velocity
                ): Velocity {
                    swipeableState.performFling(velocity = available.y)
                    return super.onPostFling(consumed, available)
                }

                private fun Float.toOffset() = Offset(0f, this)
            }
        }

        Box(
            Modifier
                .offset {
                    IntOffset(
                        0,
                        swipeableState.offset.value.roundToInt()
                    )
                }
                .swipeable(
                    state = swipeableState,
                    orientation = Orientation.Vertical,
                    anchors = mapOf(
                        maxHeightPx / 2 to States.EXPANDED,
                        maxHeightPx - peakHeight to States.PEAKED,
                        maxHeightPx to States.COLLAPSED,
                    )
                )
                .nestedScroll(connection)
        ) {
            Column(
                Modifier
                    .height(this@BoxWithConstraints.maxHeight / 2)
                    .clip(BottomSheetDefaults.ExpandedShape)
                    .background(Color.White)
            ) {
                header()
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    state = lazyListState
                ) {
                    content()
                }
            }
        }
    }
}