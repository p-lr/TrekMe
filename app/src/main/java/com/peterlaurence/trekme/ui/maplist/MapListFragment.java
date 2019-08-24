package com.peterlaurence.trekme.ui.maplist;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.peterlaurence.trekme.R;
import com.peterlaurence.trekme.core.map.Map;
import com.peterlaurence.trekme.core.map.maploader.MapLoader;
import com.peterlaurence.trekme.model.map.MapModel;
import com.peterlaurence.trekme.ui.maplist.events.ZipFinishedEvent;
import com.peterlaurence.trekme.ui.maplist.events.ZipProgressEvent;
import com.peterlaurence.trekme.viewmodel.maplist.MapListViewModel;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.ref.WeakReference;
import java.util.List;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * A {@link Fragment} that displays the list of available maps, using a {@link RecyclerView}.
 * <p>
 * Activities that contain this fragment must implement the
 * {@link MapListFragment.OnMapListFragmentInteractionListener} interface to handle interaction
 * events.
 * </p>
 */
public class MapListFragment extends Fragment implements
        MapAdapter.MapSelectionListener,
        MapAdapter.MapSettingsListener,
        MapAdapter.MapDeleteListener,
        MapLoader.MapDeletedListener {

    private FrameLayout rootView;
    private RecyclerView recyclerView;
    private MapAdapter adapter;

    private MapListViewModel viewModel;
    private OnMapListFragmentInteractionListener mListener;

    /* Used for map saving */
    private Notification.Builder builder;
    private NotificationManager notifyMgr;

    public MapListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull Context context) {
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

        FragmentActivity activity = getActivity();
        if (activity != null) {
            viewModel = ViewModelProviders.of(activity).get(MapListViewModel.class);
            viewModel.getMaps().observe(this, maps -> {
                if (maps != null) {
                    onMapListUpdate(maps);
                }
            });
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (rootView == null) {
            rootView = (FrameLayout) inflater.inflate(R.layout.fragment_map_list, container, false);
        }
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        /* When modifications happened outside of the context of this fragment, e.g if a map image
         * was changed in the settings fragment, we need to refresh the view. */
        adapter.notifyDataSetChanged();
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

    private void generateMapList() {
        Context ctx = getContext();
        if (ctx != null) {
            recyclerView = new RecyclerView(ctx);
            recyclerView.setHasFixedSize(false);

            LinearLayoutManager llm = new LinearLayoutManager(ctx);
            recyclerView.setLayoutManager(llm);

            adapter = new MapAdapter(null, this, this, this,
                    ctx.getColor(R.color.colorAccent),
                    ctx.getColor(R.color.colorPrimaryTextWhite),
                    ctx.getColor(R.color.colorPrimaryTextBlack));

            recyclerView.setAdapter(adapter);

            rootView.addView(recyclerView, 0);
        }
    }

    @Override
    public void onMapSelected(Map map) {
        viewModel.setMap(map);
        if (mListener != null) {
            mListener.onMapSelectedFragmentInteraction(map);
        }
    }

    /**
     * This fragment and its {@link MapAdapter} need to take action on map list update.
     */
    private void onMapListUpdate(List<Map> mapList) {
        rootView.findViewById(R.id.loadingPanel).setVisibility(View.GONE);
        adapter.onMapListUpdate(mapList);

        /* If no maps found, suggest to navigate to map creation */
        if (mapList.size() == 0) {
            rootView.findViewById(R.id.emptyMapPanel).setVisibility(View.VISIBLE);
            Button btn = rootView.findViewById(R.id.button_go_to_map_create);
            btn.setOnClickListener((e) -> mListener.onGoToMapCreation());
        } else {
            rootView.findViewById(R.id.emptyMapPanel).setVisibility(View.GONE);
        }
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onMapSettings(Map map) {
        MapModel.INSTANCE.setSettingsMap(map);
        if (mListener != null) {
            mListener.onMapSettingsFragmentInteraction(map);
        }
    }

    @Override
    public void onMapDelete(Map map) {
        MapSettingsFragment.ConfirmDeleteFragment f = new MapSettingsFragment.ConfirmDeleteFragment();
        f.setMapWeakRef(new WeakReference<>(map));
        f.setDeleteMapListener(this);
        f.show(getFragmentManager(), "delete");
    }

    @Override
    public void onMapDeleted() {
        adapter.notifyDataSetChanged();
    }

    /**
     * A {@link Notification} is sent to the user showing the progression in percent. The
     * {@link NotificationManager} only process one notification at a time, which is handy since
     * it prevents the application from using too much cpu.
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onZipProgressEvent(ZipProgressEvent event) {
        final String notificationChannelId = "trekadvisor_map_save";

        if (builder == null || notifyMgr == null) {
            /* Build the notification and issue it */
            builder = new Notification.Builder(getActivity())
                    .setSmallIcon(R.drawable.ic_map_black_24dp)
                    .setContentTitle(getString(R.string.archive_dialog_title));

            try {
                notifyMgr = (NotificationManager) getActivity().getSystemService(NOTIFICATION_SERVICE);
            } catch (Exception e) {
                // notifyMgr will be null
            }

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
                notifyMgr.notify(event.getMapId(), builder.build());
            }
        }

        builder.setContentText(String.format(getString(R.string.archive_notification_msg), event.getMapName()));
        builder.setProgress(100, event.getP(), false);
        if (notifyMgr != null) {
            notifyMgr.notify(event.getMapId(), builder.build());
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onZipFinishedEvent(ZipFinishedEvent event) {
        String archiveOkMsg = getString(R.string.archive_snackbar_finished);

        /* When the loop is finished, updates the notification */
        builder.setContentText(archiveOkMsg)
                // Removes the progress bar
                .setProgress(0, 0, false);
        notifyMgr.notify(event.getMapId(), builder.build());

        View view = getView();
        if (view != null) {
            Snackbar snackbar = Snackbar.make(view, archiveOkMsg, Snackbar.LENGTH_SHORT);
            snackbar.show();
        }
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

        void onGoToMapCreation();
    }
}
