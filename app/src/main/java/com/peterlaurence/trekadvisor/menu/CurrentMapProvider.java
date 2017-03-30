package com.peterlaurence.trekadvisor.menu;

import android.support.annotation.Nullable;

import com.peterlaurence.trekadvisor.core.map.Map;

/**
 * A {@link CurrentMapProvider} is any object able give the {@link Map} currently used.
 *
 * @author peterLaurence on 13/03/17.
 */
public interface CurrentMapProvider {
    @Nullable
    Map getCurrentMap();
}
