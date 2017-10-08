package com.peterlaurence.trekadvisor.menu.maplist;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.peterlaurence.trekadvisor.R;
import com.peterlaurence.trekadvisor.core.download.DownloadReceiver;
import com.peterlaurence.trekadvisor.core.download.DownloadService;
import com.peterlaurence.trekadvisor.core.map.Map;
import com.peterlaurence.trekadvisor.core.map.maploader.MapLoader;
import com.peterlaurence.trekadvisor.menu.maplist.dialogs.ArchiveMapDialog;
import com.peterlaurence.trekadvisor.menu.maplist.dialogs.MapDownloadDialog;
import com.peterlaurence.trekadvisor.util.ZipTask;

import java.io.File;

import static android.content.Context.NOTIFICATION_SERVICE;
import static com.peterlaurence.trekadvisor.core.download.DownloadService.FILE_NAME;
import static com.peterlaurence.trekadvisor.core.download.DownloadService.RECEIVER_PARAM;
import static com.peterlaurence.trekadvisor.core.download.DownloadService.URL_PARAM;

/**
 * A {@link Fragment} subclass that displays the list of available maps, using a {@link RecyclerView}.
 * <p>
 * Activities that contain this fragment must implement the
 * {@link MapListFragment.OnMapListFragmentInteractionListener} interface to handle interaction
 * events.
 * </p>
 */
public class MapListFragment extends Fragment implements
        MapAdapter.MapSelectionListener,
        MapAdapter.MapSettingsListener,
        MapAdapter.MapArchiveListener,
        MapLoader.MapListUpdateListener,
        ArchiveMapDialog.ArchiveMapListener {

    private FrameLayout rootView;
    private RecyclerView recyclerView;

    private OnMapListFragmentInteractionListener mListener;

    private Map mCurrentMap;   // The map selected by the user in the list
    private Map mSettingsMap;  // The map that the user wants to calibrate

    public MapListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnMapListFragmentInteractionListener) {
            mListener = (OnMapListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnMapListFragmentInteractionListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (rootView == null) {
            rootView = (FrameLayout) inflater.inflate(R.layout.fragment_map_list, container, false);
        }
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();

        if (recyclerView != null) {
            return;
        }

        generateMapList();
    }

    /**
     * Get a reference to the last {@link Map} that has been selected.
     */
    public Map getCurrentMap() {
        return mCurrentMap;
    }

    /**
     * Get a reference to the last {@link Map} that the user selected to edit (with the
     * settings button).
     */
    public Map getSettingsMap() {
        return mSettingsMap;
    }

    private void generateMapList() {
        Context ctx = getContext();
        recyclerView = new RecyclerView(ctx);
        recyclerView.setHasFixedSize(false);

        LinearLayoutManager llm = new LinearLayoutManager(ctx);
        recyclerView.setLayoutManager(llm);

        MapAdapter adapter = new MapAdapter(null, this, this, this,
                ctx.getColor(R.color.colorAccent),
                ctx.getColor(R.color.colorPrimaryTextWhite),
                ctx.getColor(R.color.colorPrimaryTextBlack));

        /**
         * The {@link MapAdapter} and this fragment are interested by the map list update event.
         */
        MapLoader.getInstance().addMapListUpdateListener(adapter);
        MapLoader.getInstance().addMapListUpdateListener(this);
        MapLoader.getInstance().clearAndGenerateMaps();
        recyclerView.setAdapter(adapter);

        rootView.addView(recyclerView, 0);
    }

    @Override
    public void onMapSelected(Map map) {
        mCurrentMap = map;
        if (mListener != null) {
            mListener.onMapSelectedFragmentInteraction(map);
        }
    }

    @Override
    public void onMapListUpdate(boolean mapsFound) {
        rootView.findViewById(R.id.loadingPanel).setVisibility(View.GONE);

        /* If no maps found, suggest to download a sample map */
        if (!mapsFound) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
            builder.setMessage(getString(R.string.no_maps_found_warning))
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.ok_dialog), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            MapDownloadDialog mapDownloadDialog = MapDownloadDialog.newInstance("World map");
                            mapDownloadDialog.show(getFragmentManager(), MapDownloadDialog.class.getName());

                            //TODO : put this in strings xml
                            Intent intent = new Intent(getActivity(), DownloadService.class);
                            intent.putExtra(URL_PARAM, "https://www.dropbox.com/s/cef6i12vskg92ci/world-map.zip?dl=1");
                            intent.putExtra(FILE_NAME, "world-map.zip");
                            intent.putExtra(RECEIVER_PARAM, new DownloadReceiver(new Handler(), mapDownloadDialog));

                            getActivity().startService(intent);
                        }
                    })
                    .setNegativeButton(getString(R.string.cancel_dialog_string), null);
            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        MapLoader.getInstance().clearMapListUpdateListener();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onMapSettings(Map map) {
        mSettingsMap = map;
        if (mListener != null) {
            mListener.onMapSettingsFragmentInteraction(map);
        }
    }

    @Override
    public void onMapArchive(Map map) {
        ArchiveMapDialog archiveMapDialog = ArchiveMapDialog.newInstance(map.getId());
        archiveMapDialog.show(getFragmentManager(), "ArchiveMapDialog");
    }


    /**
     * Process a request to archive a {@link Map}. This is typically called from a
     * {@link ArchiveMapDialog}. <p>
     * A {@link Notification} is sent to the user showing the progression in percent. The
     * {@link NotificationManager} only process one notification at a time, which is handy since
     * it prevents the application from using too much cpu.
     *
     * @param mapId The id of the {@link Map}.
     */
    @Override
    public void doArchiveMap(int mapId) {
        Map map = MapLoader.getInstance().getMap(mapId);
        if (map == null) return;

        /* Build the notification and issue it */
        final Notification.Builder builder = new Notification.Builder(getActivity())
                .setSmallIcon(R.drawable.ic_map_black_24dp)
                .setContentTitle(getString(R.string.archive_dialog_title))
                .setContentText(String.format(getString(R.string.archive_notification_msg), map.getName()));

        final int notificationId = mapId;
        final NotificationManager notifyMgr =
                (NotificationManager) getActivity().getSystemService(NOTIFICATION_SERVICE);
        notifyMgr.notify(notificationId, builder.build());

        /* Effectively launch the archive task */
        map.zip(new ZipTask.ZipProgressionListener() {
            @Override
            public void fileListAcquired() {

            }

            @Override
            public void onProgress(int p) {
                builder.setProgress(100, p, false);
                notifyMgr.notify(notificationId, builder.build());
            }

            @Override
            public void onZipFinished(File outputDirectory) {
                String archiveOkMsg = getContext().getString(R.string.archive_snackbar_finished);

                /* When the loop is finished, updates the notification */
                builder.setContentText(archiveOkMsg)
                        // Removes the progress bar
                        .setProgress(0, 0, false);
                notifyMgr.notify(notificationId, builder.build());

                View view = getView();
                if (view != null) {
                    Snackbar snackbar = Snackbar.make(view, archiveOkMsg, Snackbar.LENGTH_SHORT);
                    snackbar.show();
                }
            }

            @Override
            public void onZipError() {

            }
        });
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnMapListFragmentInteractionListener {
        void onMapSelectedFragmentInteraction(Map map);

        void onMapSettingsFragmentInteraction(Map map);
    }
}
