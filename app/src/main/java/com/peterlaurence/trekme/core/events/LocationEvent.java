package com.peterlaurence.trekme.core.events;

import android.location.Location;

/**
 * @author peterLaurence on 17/12/17.
 */
public class LocationEvent {
    public Location location;

    public LocationEvent(Location location) {
        this.location = location;
    }
}
