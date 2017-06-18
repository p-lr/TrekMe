package com.peterlaurence.trekadvisor.menu.mapview;

import android.content.Context;
import android.view.View;

import com.peterlaurence.trekadvisor.core.map.Map;
import com.peterlaurence.trekadvisor.menu.mapview.components.DistanceMarker;
import com.qozix.tileview.TileView;

/**
 * @author peterLaurence on 17/06/17.
 */
public class DistanceLayer {
    private View mParentView;
    private Context mContext;
    private DistanceMarker mDistanceMarkerFirst;
    private DistanceMarker mDistanceMarkerSecond;

    private Map mMap;
    private TileView mTileView;

    DistanceLayer(View parentView, Context context) {
    }

    void init(Map map, TileView tileView) {
        mMap = map;
    }
}
