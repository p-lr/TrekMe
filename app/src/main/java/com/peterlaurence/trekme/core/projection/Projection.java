package com.peterlaurence.trekme.core.projection;

import android.support.annotation.Nullable;

/**
 * Interface which defines a projection.
 *
 * @author peterLaurence.
 */
public interface Projection {
    /* Called after object creation */
    void init();

    @Nullable
    double[] doProjection(double latitude, double longitude);

    @Nullable
    double[] undoProjection(double X, double Y);

    String getName();
}
