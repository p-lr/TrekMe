package com.peterlaurence.trekadvisor.menu.record;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.peterlaurence.trekadvisor.R;
import com.peterlaurence.trekadvisor.menu.dialogs.EditFieldDialog;
import com.peterlaurence.trekadvisor.menu.events.RecordGpxStopEvent;
import com.peterlaurence.trekadvisor.menu.record.components.ActionsView;
import com.peterlaurence.trekadvisor.menu.record.components.RecordListView;
import com.peterlaurence.trekadvisor.menu.record.components.StatusView;
import com.peterlaurence.trekadvisor.menu.record.components.dialogs.MapChoiceDialog;
import com.peterlaurence.trekadvisor.menu.record.components.events.RecordingNameChangeEvent;
import com.peterlaurence.trekadvisor.menu.record.components.events.RequestChooseMap;
import com.peterlaurence.trekadvisor.menu.record.components.events.RequestEditRecording;
import com.peterlaurence.trekadvisor.menu.record.components.events.RequestStartEvent;
import com.peterlaurence.trekadvisor.menu.record.components.events.RequestStopEvent;
import com.peterlaurence.trekadvisor.service.LocationService;
import com.peterlaurence.trekadvisor.service.event.LocationServiceStatus;
import com.peterlaurence.trekadvisor.util.FileUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

/**
 * Holds controls and displays information about the
 * {@link com.peterlaurence.trekadvisor.service.LocationService}.
 */
public class RecordFragment extends Fragment {
    ActionsView mActionsView;
    StatusView mStatusView;
    RecordListView mRecordListView;

    /* Required empty public constructor */
    public RecordFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_record, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mActionsView = view.findViewById(R.id.record_actionsView);
        mStatusView = view.findViewById(R.id.record_statusView);
        mRecordListView = view.findViewById(R.id.record_listView);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        EventBus.getDefault().register(mActionsView);
        EventBus.getDefault().register(mRecordListView);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(mActionsView);
        EventBus.getDefault().unregister(mRecordListView);
        EventBus.getDefault().unregister(this);
        mRecordListView.cancelPendingJobs();
        super.onStop();
    }

    @Subscribe
    public void onRequestStartEvent(RequestStartEvent event) {
        Intent intent = new Intent(getActivity().getBaseContext(), LocationService.class);
        getActivity().startService(intent);
    }

    @Subscribe
    public void onRequestStopEvent(RequestStopEvent event) {
        EventBus.getDefault().post(new RecordGpxStopEvent());
    }

    @Subscribe
    public void onLocationServiceStatusEvent(LocationServiceStatus event) {
        if (event.started) {
            mStatusView.onServiceStarted();
        } else {
            mStatusView.onServiceStopped();
        }
    }

    /**
     * The {@link RecordFragment} is only used here to show the dialog.
     */
    @Subscribe
    public void onRequestEditRecording(RequestEditRecording event) {
        FragmentActivity fragmentActivity = getActivity();
        if (fragmentActivity != null) {
            String recordingName = FileUtils.getFileNameWithoutExtention(event.recording);
            RecordingNameChangeEvent eventBack = new RecordingNameChangeEvent("", "");
            EditFieldDialog editFieldDialog = EditFieldDialog.newInstance(getString(R.string.track_file_name_change), recordingName, eventBack);
            editFieldDialog.show(fragmentActivity.getSupportFragmentManager(), "EditFieldDialog" + event.recording.getName());
        }
    }

    @Subscribe
    public void onRequestChooseMap(RequestChooseMap event) {
        FragmentActivity fragmentActivity = getActivity();
        if (fragmentActivity != null) {
            MapChoiceDialog dialog = new MapChoiceDialog();
            dialog.show(fragmentActivity.getSupportFragmentManager(), "MapChoiceDialog");
        }
    }
}
