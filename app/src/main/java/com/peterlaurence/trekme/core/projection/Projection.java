package com.peterlaurence.trekme.core.projection;

import androidx.annotation.Nullable;

/**
 * Interface which defines a projection.
 *
 * @author P.Laurence
 */
public interface Projection {
    /* Called after object creation */
    void init();

    /**
     * @param latitude the latitude in decimal degrees
     * @param longitude the longitude in decimal degrees
     * @return a {@code double[]} of size 2. First element is the projected coordinate in X, the
     * second is the projected coordinate in Y.
     */
    @Nullable
    double[] doProjection(double latitude, double longitude);

    /**
     * @param X the projected coordinate in X
     * @param Y the projected coordinate in Y
     * @return a {@code double[]} of size 2. First element is the longitude, the second is the
     * latitude. Values are expressed in decimal degrees.
     */
    @Nullable
    double[] undoProjection(double X, double Y);

    String getName();

    int getSrid();
}
