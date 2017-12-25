package com.peterlaurence.trekadvisor.menu.record;


import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.peterlaurence.trekadvisor.R;
import com.peterlaurence.trekadvisor.menu.events.RecordGpxStartEvent;
import com.peterlaurence.trekadvisor.menu.events.RecordGpxStopEvent;
import com.peterlaurence.trekadvisor.menu.record.components.ActionsView;
import com.peterlaurence.trekadvisor.menu.record.components.events.RequestStartEvent;
import com.peterlaurence.trekadvisor.menu.record.components.events.RequestStopEvent;
import com.peterlaurence.trekadvisor.service.LocationService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

/**
 * Holds controls and displays information about the
 * {@link com.peterlaurence.trekadvisor.service.LocationService}.
 */
public class RecordFragment extends Fragment {
    ActionsView mActionsView;

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
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        EventBus.getDefault().register(mActionsView);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(mActionsView);
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Subscribe
    public void onRequestStartEvent(RequestStartEvent event) {
        Intent intent = new Intent(getActivity().getBaseContext(), LocationService.class);
        getActivity().startService(intent);
        EventBus.getDefault().post(new RecordGpxStartEvent());
    }

    @Subscribe
    public void onRequestStopEvent(RequestStopEvent event) {
        EventBus.getDefault().post(new RecordGpxStopEvent());
    }
}
