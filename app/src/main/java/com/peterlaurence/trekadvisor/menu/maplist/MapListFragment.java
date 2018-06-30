package com.peterlaurence.trekadvisor.menu.maplist;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.peterlaurence.trekadvisor.R;
import com.peterlaurence.trekadvisor.core.TrekAdvisorContext;
import com.peterlaurence.trekadvisor.core.download.DownloadTask;
import com.peterlaurence.trekadvisor.core.download.UrlDownloadTaskExecutor;
import com.peterlaurence.trekadvisor.core.map.Map;
import com.peterlaurence.trekadvisor.core.map.maploader.MapLoader;
import com.peterlaurence.trekadvisor.menu.maplist.dialogs.ArchiveMapDialog;
import com.peterlaurence.trekadvisor.menu.maplist.dialogs.UrlDownloadDialog;
import com.peterlaurence.trekadvisor.menu.maplist.dialogs.events.UrlDownloadEvent;
import com.peterlaurence.trekadvisor.menu.maplist.dialogs.events.UrlDownloadFinishedEvent;
import com.peterlaurence.trekadvisor.util.ZipTask;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;

import static android.content.Context.NOTIFICATION_SERVICE;

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
        MapLoader.MapListUpdateListener {

    private FrameLayout rootView;
    private RecyclerView recyclerView;

    private OnMapListFragmentInteractionListener mListener;

    private Map mCurrentMap;   // The map selected by the user in the list
    private Map mSettingsMap;  // The map that the user wants to calibrate
    private String mDefaultMapUrl;

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
        setHasOptionsMenu(true);

        mDefaultMapUrl = getString(R.string.default_map_url);
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        /* Clear the existing action menu */
        menu.clear();

        /* Fill the new one */
        inflater.inflate(R.menu.menu_fragment_map_list, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sample_map_menu_id:
                showSampleMapDownloadDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);

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
                            showSampleMapDownloadDialog();
                        }
                    })
                    .setNegativeButton(getString(R.string.cancel_dialog_string), null);
            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    private void showSampleMapDownloadDialog() {
        UrlDownloadDialog urlDownloadDialog = UrlDownloadDialog.newInstance("World map", mDefaultMapUrl);
        urlDownloadDialog.show(getFragmentManager(), UrlDownloadDialog.class.getName());

        final File outputFile = new File(TrekAdvisorContext.DEFAULT_APP_DIR, "world-map.zip");
        final int urlHash = mDefaultMapUrl.hashCode();
        UrlDownloadTaskExecutor.startUrlDownload(mDefaultMapUrl, outputFile, new DownloadTask.UrlDownloadListener() {
            @Override
            public void onDownloadProgress(int percent) {
                EventBus.getDefault().post(new UrlDownloadEvent(percent, urlHash));
            }

            @Override
            public void onDownloadFinished(boolean success) {
                EventBus.getDefault().post(new UrlDownloadFinishedEvent(success, urlHash));

                /* If success, notify the activity */
                if (success) {
                    mListener.onDefaultMapDownloaded();
                }
            }
        });
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
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
     * @param event The {@link com.peterlaurence.trekadvisor.menu.maplist.dialogs.ArchiveMapDialog.SaveMapEvent}
     *              which contains the id of the {@link Map}.
     */
    @Subscribe
    public void onSaveMapEvent(ArchiveMapDialog.SaveMapEvent event) {
        Map map = MapLoader.getInstance().getMap(event.mapId);
        if (map == null) return;

        final String notificationChannelId = "trekadvisor_map_save";

        /* Build the notification and issue it */
        final Notification.Builder builder = new Notification.Builder(getActivity())
                .setSmallIcon(R.drawable.ic_map_black_24dp)
                .setContentTitle(getString(R.string.archive_dialog_title))
                .setContentText(String.format(getString(R.string.archive_notification_msg), map.getName()));

        final int notificationId = event.mapId;
        final NotificationManager notifyMgr =
                (NotificationManager) getActivity().getSystemService(NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= 26) {
            //This only needs to be run on Devices on Android O and above
            NotificationChannel mChannel = new NotificationChannel(notificationChannelId,
                    getText(R.string.archive_dialog_title), NotificationManager.IMPORTANCE_LOW);
            mChannel.enableLights(true);
            mChannel.setLightColor(Color.YELLOW);
            if (notifyMgr != null) {
                notifyMgr.createNotificationChannel(mChannel);
            }
            builder.setChannelId(notificationChannelId);
        }

        if (notifyMgr != null) {
            notifyMgr.notify(notificationId, builder.build());
        }

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

        void onDefaultMapDownloaded();
    }
}
