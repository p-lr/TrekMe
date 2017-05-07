package com.peterlaurence.trekadvisor.menu.mapview.components.markermanage;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.peterlaurence.trekadvisor.R;
import com.peterlaurence.trekadvisor.core.map.Map;
import com.peterlaurence.trekadvisor.core.map.gson.MarkerGson;
import com.peterlaurence.trekadvisor.menu.MapProvider;
import com.peterlaurence.trekadvisor.menu.MarkerProvider;

import static android.view.View.GONE;

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
    private MarkerGson.Marker mMarker;

    private TextInputEditText mNameEditText;
    private TextInputEditText mLatEditText;
    private TextInputEditText mLonEditText;
    private TextView mProjectionLabel;
    private TextInputEditText mProjectionX;
    private TextInputEditText mProjectionY;
    private EditText mComment;

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

        /* The view fields */
        mNameEditText = (TextInputEditText) rootView.findViewById(R.id.marker_name_id);
        mLatEditText = (TextInputEditText) rootView.findViewById(R.id.marker_lat_id);
        mLonEditText = (TextInputEditText) rootView.findViewById(R.id.marker_lon_id);
        mProjectionLabel = (TextView) rootView.findViewById(R.id.marker_proj_label_id);
        mProjectionX = (TextInputEditText) rootView.findViewById(R.id.marker_proj_x_id);
        mProjectionY = (TextInputEditText) rootView.findViewById(R.id.marker_proj_y_id);
        mComment = (EditText) rootView.findViewById(R.id.marker_comment_id);

        mMap = mMapProvider.getCurrentMap();
        mMarker = mMarkerProvider.getCurrentMarker();

        updateView();
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        /* Hide the app title */
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }

        /* Clear the existing action menu */
        menu.clear();

        /* .. and fill the new one */
        inflater.inflate(R.menu.menu_fragment_marker_manage, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save_marker_id:
                //TODO : implement makers save
                return true;
            case R.id.undo_marker_id:
                updateView();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateView() {
        mNameEditText.setText(mMarker.name);
        mLatEditText.setText(String.valueOf(mMarker.lat));
        mLonEditText.setText(String.valueOf(mMarker.lon));
        mComment.setText(mMarker.comment);

        /* Check whether projected coordinates fields should be shown or not */
        if (mMap.getProjection() == null) {
            mProjectionLabel.setVisibility(GONE);
            mProjectionX.setVisibility(GONE);
            mProjectionY.setVisibility(GONE);
            return;
        }

        mProjectionX.setText(String.valueOf(mMarker.proj_x));
        mProjectionY.setText(String.valueOf(mMarker.proj_y));
    }
}