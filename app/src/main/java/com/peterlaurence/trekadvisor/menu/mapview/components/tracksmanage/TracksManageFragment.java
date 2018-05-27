package com.peterlaurence.trekadvisor.menu.mapview.components.tracksmanage;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.peterlaurence.trekadvisor.MainActivity;
import com.peterlaurence.trekadvisor.R;
import com.peterlaurence.trekadvisor.core.map.Map;
import com.peterlaurence.trekadvisor.core.map.gson.RouteGson;
import com.peterlaurence.trekadvisor.core.map.maploader.MapLoader;
import com.peterlaurence.trekadvisor.core.track.TrackImporter;
import com.peterlaurence.trekadvisor.menu.MapProvider;

import java.util.HashMap;
import java.util.List;

/**
 * A {@link Fragment} subclass that shows the routes currently available for a given map, and
 * provides the ability to import new routes.
 *
 * @author peterLaurence on 01/03/17.
 */
public class TracksManageFragment extends Fragment implements TrackImporter.TrackFileParsedListener,
        TrackAdapter.TrackSelectionListener {
    public static final String TAG = "TracksManageFragment";
    private static final int TRACK_REQUEST_CODE = 1337;
    private static final String ROUTE_INDEX = "routeIndex";
    private FrameLayout rootView;
    private Map mMap;
    private MapProvider mMapProvider;
    private MenuItem mTrackRenameMenuItem;
    private TrackChangeListenerProvider mTrackChangeListenerProvider;
    private TrackChangeListener mTrackChangeListener;
    private TrackAdapter mTrackAdapter;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof MapProvider && context instanceof TrackChangeListenerProvider) {
            mMapProvider = (MapProvider) context;
            mTrackChangeListenerProvider = (TrackChangeListenerProvider) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement MapProvider and MapLoader.MapUpdateListener");
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
        mMap = mMapProvider.getCurrentMap();
        generateTracks(mMap);

        if (savedInstanceState != null) {
            int routeIndex = savedInstanceState.getInt(ROUTE_INDEX);
            if (routeIndex >= 0) {
                mTrackAdapter.restoreSelectionIndex(routeIndex);
            }
        }

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_fragment_tracks_manage, menu);
        mTrackRenameMenuItem = menu.findItem(R.id.track_rename_id);
        if (mTrackAdapter.getSelectedRouteIndex() >= 0) {
            mTrackRenameMenuItem.setVisible(true);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onStart() {
        super.onStart();

        /* The listener is acquired now in this fragment's life cycle, and not during onAttach
         * because at that moment the FragmentManager is not fully initialized (it may not have
         * attached all needed fragments) */
        mTrackChangeListener = mTrackChangeListenerProvider.getTrackChangeListener();
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
            case R.id.track_rename_id:
                ChangeRouteNameFragment fragment = new ChangeRouteNameFragment();
                fragment.show(getFragmentManager(), "rename route");
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
        Context ctx = getContext();
        RecyclerView recyclerView = new RecyclerView(ctx);
        recyclerView.setHasFixedSize(false);

        /* All cards are laid out vertically */
        LinearLayoutManager llm = new LinearLayoutManager(ctx);
        recyclerView.setLayoutManager(llm);

        /* Apply item decoration (add an horizontal divider) */
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(ctx,
                DividerItemDecoration.VERTICAL);
        Drawable divider = this.getContext().getDrawable(R.drawable.divider);
        if (divider != null) {
            dividerItemDecoration.setDrawable(divider);
        }
        recyclerView.addItemDecoration(dividerItemDecoration);

        mTrackAdapter = new TrackAdapter(map, this, ctx.getColor(R.color.colorAccent),
                ctx.getColor(R.color.colorPrimaryTextWhite),
                ctx.getColor(R.color.colorPrimaryTextBlack));
        recyclerView.setAdapter(mTrackAdapter);

        /* Swipe to dismiss functionality */
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                /* Remove the track from the list and from the map */
                mTrackAdapter.removeItem(viewHolder.getAdapterPosition());

                /* Update the view */
                if (mTrackChangeListener != null) {
                    mTrackChangeListener.onTrackVisibilityChanged();
                }

                /* Save */
                saveChanges();
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        rootView.addView(recyclerView, 0);
    }

    private void importTrack(Uri uri) {
        TrackImporter.importTrackFile(uri, this, mMap, getContext().getContentResolver());
    }

    private void saveChanges() {
        MapLoader.getInstance().saveRoutes(mMap);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mMapProvider = null;
        mTrackChangeListenerProvider = null;
    }

    @Override
    public void onTrackFileParsed(Map map, List<RouteGson.Route> routeList) {
        /* We want to append new routes, so the index to add new routes is equal to current length
         * of the data set. */
        int positionStart = mTrackAdapter.getItemCount();
        int newRouteCount = updateRouteList(map, routeList);
        if (mTrackChangeListener != null) {
            mTrackChangeListener.onTrackChanged(map, routeList);
        }
        mTrackAdapter.notifyItemRangeInserted(positionStart, newRouteCount);

        /* Save */
        saveChanges();
    }

    /**
     * Add new {@link RouteGson.Route}s to a {@link Map}.
     *
     * @return the number of {@link RouteGson.Route} that have been appended to the list.
     */
    private int updateRouteList(Map map, List<RouteGson.Route> newRouteList) {
        if (newRouteList == null) return 0;
        java.util.Map<String, RouteGson.Route> hashMap = new HashMap<>();
        List<RouteGson.Route> routeList = map.getRoutes();
        if (routeList != null) {
            for (RouteGson.Route route : routeList) {
                hashMap.put(route.name, route);
            }
        }

        int newRouteCount = 0;
        for (RouteGson.Route route : newRouteList) {
            if (hashMap.containsKey(route.name)) {
                RouteGson.Route existingRoute = hashMap.get(route.name);
                existingRoute.copyRoute(route);
            } else {
                map.addRoute(route);
                newRouteCount++;
            }
        }

        return newRouteCount;
    }

    @Override
    public void onError(String message) {
        Log.e(TAG, message);
    }

    @Override
    public void onTrackSelected() {
        mTrackRenameMenuItem.setVisible(true);
    }

    @Override
    public void onVisibilityToggle(RouteGson.Route route) {
        if (mTrackChangeListener != null) {
            mTrackChangeListener.onTrackVisibilityChanged();
        }

        /* Save */
        saveChanges();
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ROUTE_INDEX, mTrackAdapter.getSelectedRouteIndex());
    }

    public interface TrackChangeListenerProvider {
        TrackChangeListener getTrackChangeListener();
    }

    public interface TrackChangeListener {
        /**
         * When new {@link RouteGson.Route} are added or modified, this method is called.
         *
         * @param map       the {@link Map} associated with the change
         * @param routeList a list of {@link RouteGson.Route}
         */
        void onTrackChanged(Map map, List<RouteGson.Route> routeList);

        /**
         * When the visibility of a {@link RouteGson.Route} is changed, this method is called.
         */
        void onTrackVisibilityChanged();
    }

    public static class ChangeRouteNameFragment extends DialogFragment {
        private TracksManageFragment mTracksManageFragment;
        private String mText;

        /**
         * The first time this fragment is created, the activity exists and so does the
         * {@link TracksManageFragment}. But, upon configuration change, the
         * {@link TracksManageFragment} is not yet attached to the fragment manager when
         * {@link #onAttach(Context)} is called. <br>
         * So, we get a reference to it later in {@link #onActivityCreated(Bundle)}.
         * In the meanwhile, we don't have to retrieve the route's name because the framework
         * automatically saves the {@link EditText} state upon configuration change.
         */
        @Override
        public void onAttach(Context context) {
            super.onAttach(context);

            try {
                mTracksManageFragment = ((MainActivity) getActivity()).getTracksManageFragment();

                final RouteGson.Route route = mTracksManageFragment.mTrackAdapter.getSelectedRoute();
                if (route != null) {
                    mText = route.name;
                }
            } catch (NullPointerException e) {
                /* The fragment is being recreated upon configuration change */
            }
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            if (mTracksManageFragment == null) {
                mTracksManageFragment = ((MainActivity) getActivity()).getTracksManageFragment();
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = getActivity().getLayoutInflater();
            final View view = inflater.inflate(R.layout.change_trackname_fragment, null);

            if (mText != null) {
                EditText editText = (EditText) view.findViewById(R.id.track_name_edittext);
                editText.setText(mText);
            }

            builder.setView(view);
            builder.setMessage(R.string.track_name_change)
                    .setPositiveButton(R.string.ok_dialog, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            RouteGson.Route route = mTracksManageFragment.mTrackAdapter.getSelectedRoute();
                            EditText editText = (EditText) view.findViewById(R.id.track_name_edittext);
                            if (route != null) {
                                route.name = editText.getText().toString();
                                mTracksManageFragment.mTrackAdapter.notifyDataSetChanged();
                                mTracksManageFragment.saveChanges();
                            }
                        }
                    })
                    .setNegativeButton(R.string.cancel_dialog_string, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // Do nothing. This empty listener is used just to create the Cancel button.
                        }
                    });
            return builder.create();
        }
    }
}
