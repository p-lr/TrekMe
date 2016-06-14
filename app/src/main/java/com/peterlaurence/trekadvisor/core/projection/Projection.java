package com.peterlaurence.trekadvisor.core.projection;

/**
 * Interface which defines a projection.
 *
 * @author peterLaurence.
 */
public interface Projection {
    /* Called after object creation */
    void init();

    void doProjection(double latitude, double longitude);

    void undoProjection(double X, double Y);

    double[] getProjectedValues();

    double[] getWgs84Coords();

    String getName();
}
