package com.peterlaurence.trekadvisor.menu.mapview.components.markermanage;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.peterlaurence.trekadvisor.R;
import com.peterlaurence.trekadvisor.core.map.Map;
import com.peterlaurence.trekadvisor.core.map.gson.MapGson;
import com.peterlaurence.trekadvisor.menu.MapProvider;
import com.peterlaurence.trekadvisor.menu.MarkerProvider;

/**
 * A {@link Fragment} subclass that provides tools to :
 * <ul>
 * <li>Edit a marker's associated comment</li>
 * <li>See the WGS84 and projected coordinates of the marker, if possible</li>
 * <li>Delete the marker</li>
 * </ul>
 *
 * @author peterLaurence on 23/04/2017.
 */

public class MarkerManageFragment extends Fragment {
    private View rootView;
    private MarkerProvider mMarkerProvider;
    private MapProvider mMapProvider;

    private Map mMap;
    private MapGson.Marker mMarker;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof MapProvider && context instanceof MarkerProvider) {
            mMapProvider = (MapProvider) context;
            mMarkerProvider = (MarkerProvider) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement MapProvider and MarkerProvider");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_marker_manage, container, false);
        mMap = mMapProvider.getCurrentMap();
        mMarker = mMarkerProvider.getCurrentMarker();
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        super.onCreateOptionsMenu(menu, inflater);
    }
}