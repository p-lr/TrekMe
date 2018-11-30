package com.peterlaurence.trekme.core.map.maploader.events;

public class MapListUpdateEvent {
    public boolean mapsFound;

    public MapListUpdateEvent(boolean mapsFound) {
        this.mapsFound = mapsFound;
    }
}
