package com.peterlaurence.trekme.ui.mapview.components.tracksmanage;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.google.android.material.snackbar.Snackbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.peterlaurence.trekme.MainActivity;
import com.peterlaurence.trekme.R;
import com.peterlaurence.trekme.core.map.Map;
import com.peterlaurence.trekme.core.map.gson.MarkerGson;
import com.peterlaurence.trekme.core.map.gson.RouteGson;
import com.peterlaurence.trekme.core.map.maploader.MapLoader;
import com.peterlaurence.trekme.core.track.TrackImporter;
import com.peterlaurence.trekme.ui.mapview.events.TrackVisibilityChangedEvent;
import com.peterlaurence.trekme.model.map.MapProvider;

import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;

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
    private ConstraintLayout emptyRoutePanel;
    private Map mMap;
    private MenuItem mTrackRenameMenuItem;
    private TrackAdapter mTrackAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = (FrameLayout) inflater.inflate(R.layout.fragment_tracks_manage, container, false);
        emptyRoutePanel = rootView.findViewById(R.id.emptyRoutePanel);
        mMap = MapProvider.INSTANCE.getCurrentMap();
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

        updateEmptyRoutePanelVisibility();
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

            Uri uri;
            if (resultData != null) {
                uri = resultData.getData();
                if (uri == null) return;

                if (!TrackImporter.INSTANCE.isFileSupported(uri)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
                    LayoutInflater inflater = getActivity().getLayoutInflater();
                    builder.setView(inflater.inflate(R.layout.track_warning, null));
                    builder.setCancelable(false)
                            .setPositiveButton(getString(R.string.ok_dialog), null);
                    AlertDialog alert = builder.create();
                    alert.show();
                }

                /* Import the file */
                TrackImporter.INSTANCE.importTrackUri(uri, this, mMap, getContext().getContentResolver());
            }
        }
    }

    private void generateTracks(Map map) {
        Context ctx = getContext();
        if (ctx == null) return;
        RecyclerView recyclerView = new RecyclerView(ctx);
        recyclerView.setHasFixedSize(false);

        /* All cards are laid out vertically */
        LinearLayoutManager llm = new LinearLayoutManager(ctx);
        recyclerView.setLayoutManager(llm);

        /* Apply item decoration (add an horizontal divider) */
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(ctx,
                DividerItemDecoration.VERTICAL);
        Drawable divider = ctx.getDrawable(R.drawable.divider);
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
                EventBus.getDefault().post(new TrackVisibilityChangedEvent());

                /* Save */
                saveChanges();
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        rootView.addView(recyclerView, 0);
    }

    private void saveChanges() {
        MapLoader.getInstance().saveRoutes(mMap);
    }

    @Override
    public void onTrackFileParsed(@NotNull Map map, @NotNull List<RouteGson.Route> routeList, @NotNull List<? extends MarkerGson.Marker> wayPoints, int newRouteCount, int addedMarkers) {
        /* We want to append new routes, so the index to add new routes is equal to current length
         * of the data set. */
        int positionStart = mTrackAdapter.getItemCount();
        mTrackAdapter.notifyItemRangeInserted(positionStart, newRouteCount);

        /* Display to the user a recap of how many tracks and waypoints were imported */
        Activity activity = getActivity();
        if (activity != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(getString(R.string.import_result_title));
            View view = activity.getLayoutInflater().inflate(R.layout.import_gpx_result, null);

            TextView trackCountTextView = view.findViewById(R.id.tracksCount);
            trackCountTextView.setText(String.valueOf(newRouteCount));
            TextView waypointCountTextView = view.findViewById(R.id.waypointsCount);
            waypointCountTextView.setText(String.valueOf(addedMarkers));

            builder.setView(view);
            builder.show();
        }

        /* Since new routes may have added, update the empty panel visibility */
        updateEmptyRoutePanelVisibility();

        /* Save */
        saveChanges();
    }

    @Override
    public void onError(String message) {
        View view = getView();
        if (view != null) {
            Snackbar snackbar = Snackbar.make(getView(), R.string.gpx_import_error_msg, Snackbar.LENGTH_LONG);
            snackbar.show();
        }
        Log.e(TAG, message);
    }

    @Override
    public void onTrackSelected() {
        mTrackRenameMenuItem.setVisible(true);
    }

    @Override
    public void onVisibilityToggle(RouteGson.Route route) {
        EventBus.getDefault().post(new TrackVisibilityChangedEvent());

        /* Save */
        saveChanges();
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ROUTE_INDEX, mTrackAdapter.getSelectedRouteIndex());
    }

    /* Show or hide the panel indicating that there is no routes */
    private void updateEmptyRoutePanelVisibility() {
        if (mTrackAdapter.getItemCount() > 0) {
            emptyRoutePanel.setVisibility(View.GONE);
        } else {
            emptyRoutePanel.setVisibility(View.VISIBLE);
        }
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
