package com.peterlaurence.trekadvisor.core.projection;

import android.os.AsyncTask;

import java.lang.ref.WeakReference;

/**
 * Computes a projection. On a WGS84 coordinates update, we need to compute the coordinates of the
 * currently visible map's projection.
 *
 * @author peterLaurence
 */
public class ProjectionTask extends AsyncTask<Object, Void, Object> {
    private WeakReference<ProjectionUpdateLister> mProjectionUpdateListerWeakReference;
    private double mLatitude;
    private double mLongitude;
    private Projection mProjection;

    public interface ProjectionUpdateLister {
        void onProjectionUpdate(Projection projection);
    }

    public ProjectionTask(ProjectionUpdateLister projectionUpdateLister, double latitude,
                          double longitude, Projection projection) {
        mProjectionUpdateListerWeakReference = new WeakReference<>(projectionUpdateLister);
        mLatitude = latitude;
        mLongitude = longitude;
        mProjection = projection;
    }

    @Override
    protected Object doInBackground(Object[] params) {
        mProjection.doProjection(mLatitude, mLongitude);
        return null;
    }

    @Override
    protected void onPostExecute(Object result) {
        ProjectionUpdateLister projectionUpdateLister = mProjectionUpdateListerWeakReference.get();
        if (projectionUpdateLister != null) {
            projectionUpdateLister.onProjectionUpdate(mProjection);
        }
    }
}
