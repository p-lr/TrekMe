package com.peterlaurence.trekadvisor.menu;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

/**
 * @author peterLaurence on 16/04/17.
 */
public interface LocationProvider {
    void registerLocationListener(GoogleApiClient.ConnectionCallbacks connectionCallbacks,
                                  GoogleApiClient.OnConnectionFailedListener connectionFailedListener);

    void requestLocationUpdates(LocationListener listener, LocationRequest request);

    void removeLocationListener(LocationListener listener);

}
