package com.peterlaurence.trekadvisor.menu.record;


import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.peterlaurence.trekadvisor.R;

/**
 * Holds controls and displays information about the
 * {@link com.peterlaurence.trekadvisor.service.LocationService}.
 */
public class RecordFragment extends Fragment {

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

}
