package com.peterlaurence.trekadvisor.menu;

import android.support.annotation.Nullable;

import com.peterlaurence.trekadvisor.core.map.gson.MapGson;

/**
 * Any object able to give the current marker requested for editing.
 *
 * @author peterLaurence on 29/04/17.
 */
public interface MarkerProvider {
    @Nullable
    MapGson.Marker getCurrentMarker();
}
