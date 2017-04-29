package com.peterlaurence.trekadvisor.menu.mapview.components.markermanage;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.peterlaurence.trekadvisor.R;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_marker_manage, container, false);
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        super.onCreateOptionsMenu(menu, inflater);
    }
}