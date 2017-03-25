package com.peterlaurence.trekadvisor.menu.tracksmanage;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.peterlaurence.trekadvisor.R;
import com.peterlaurence.trekadvisor.core.map.Map;
import com.peterlaurence.trekadvisor.core.map.gson.MapGson;
import com.peterlaurence.trekadvisor.core.track.TrackImporter;
import com.peterlaurence.trekadvisor.menu.CurrentMapProvider;

import java.util.HashMap;
import java.util.List;

/**
 * A {@link Fragment} subclass that shows the routes currently available for a given map, and
 * provides the ability to import new routes.
 *
 * @author peterLaurence on 01/03/17.
 */
public class TracksManageFragment extends Fragment implements TrackImporter.TrackFileParsedListener {
    public static final String TAG = "TracksManageFragment";
    private static final int TRACK_REQUEST_CODE = 1337;
    private FrameLayout rootView;
    private Map mMap;
    private CurrentMapProvider mCurrentMapProvider;
    private TrackChangeListener mTrackChangeListener;
    private TrackAdapter mTrackAdapter;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof CurrentMapProvider && context instanceof TrackChangeListener) {
            mCurrentMapProvider = (CurrentMapProvider) context;
            mTrackChangeListener = (TrackChangeListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement CurrentMapProvider and MapLoader.MapUpdateListener");
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

        rootView = (FrameLayout) inflater.inflate(R.layout.fragment_tracks_manage, container, false);
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_fragment_tracks_manage, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onStart() {
        super.onStart();

        mMap = mCurrentMapProvider.getCurrentMap();
        generateTracks(mMap);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.import_tracks_id:
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);

                /* Search for all documents available via installed storage providers */
                intent.setType("*/*");
                startActivityForResult(intent, TRACK_REQUEST_CODE);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        /* Check if the request code is the one we are interested in */
        if (requestCode == TRACK_REQUEST_CODE && resultCode == Activity.RESULT_OK) {

            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();

                if (!TrackImporter.isFileSupported(uri)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
                    LayoutInflater inflater = getActivity().getLayoutInflater();
                    builder.setView(inflater.inflate(R.layout.track_warning, null));
                    builder.setCancelable(false)
                            .setPositiveButton(getString(R.string.ok_dialog), null);
                    AlertDialog alert = builder.create();
                    alert.show();
                }

                /* Import the file */
                importTrack(uri);
            }
        }
    }

    private void generateTracks(Map map) {
        RecyclerView recyclerView = new RecyclerView(this.getContext());
        recyclerView.setHasFixedSize(false);

        LinearLayoutManager llm = new LinearLayoutManager(this.getContext());
        recyclerView.setLayoutManager(llm);

        /* Apply item decoration (add an horizontal divider) */
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this.getContext(), DividerItemDecoration.VERTICAL);
        Drawable divider = this.getContext().getDrawable(R.drawable.divider);
        if (divider != null) {
            dividerItemDecoration.setDrawable(divider);
        }
        recyclerView.addItemDecoration(dividerItemDecoration);

        mTrackAdapter = new TrackAdapter(map);
        recyclerView.setAdapter(mTrackAdapter);

        rootView.addView(recyclerView, 0);
    }

    private void importTrack(Uri uri) {
        TrackImporter.importTrackFile(uri, this, mMap, getContext().getContentResolver());
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCurrentMapProvider = null;
    }

    @Override
    public void onTrackFileParsed(Map map, List<MapGson.Route> routeList) {
        Log.d(TAG, "Track file parsed");
        updateRouteList(routeList);
        mTrackChangeListener.onTrackChanged(map, routeList);
        mTrackAdapter.notifyDataSetChanged();
    }

    /**
     * Add new {@link MapGson.Route}s to the {@link Map}.
     */
    private void updateRouteList(List<MapGson.Route> routeList) {
        java.util.Map<String, MapGson.Route> hashMap = new HashMap<>();
        for (MapGson.Route route : mMap.getMapGson().routes) {
            hashMap.put(route.name, route);
        }

        for (MapGson.Route route : routeList) {
            if (hashMap.containsKey(route.name)) {
                MapGson.Route existingRoute = hashMap.get(route.name);
                existingRoute.copyRoute(route);
            } else {
                mMap.addRoute(route);
            }
        }
    }

    @Override
    public void onError(String message) {
        Log.e(TAG, message);
    }

    public interface TrackChangeListener {
        /**
         * When new {@link MapGson.Route} are added or modified, this method is called.
         *
         * @param map       the {@link Map} associated with the change
         * @param routeList a list of {@link MapGson.Route}
         */
        void onTrackChanged(Map map, List<MapGson.Route> routeList);
    }
}
