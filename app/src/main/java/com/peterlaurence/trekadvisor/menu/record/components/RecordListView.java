package com.peterlaurence.trekadvisor.menu.record.components;

import android.content.Context;
import android.graphics.Color;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;

import com.peterlaurence.trekadvisor.R;
import com.peterlaurence.trekadvisor.core.track.TrackImporter;
import com.peterlaurence.trekadvisor.core.track.TrackTools;
import com.peterlaurence.trekadvisor.menu.record.components.events.RecordingNameChangeEvent;
import com.peterlaurence.trekadvisor.menu.record.components.events.RequestChooseMap;
import com.peterlaurence.trekadvisor.menu.record.components.events.RequestEditRecording;
import com.peterlaurence.trekadvisor.menu.tools.RecyclerItemClickListener;
import com.peterlaurence.trekadvisor.service.event.GpxFileWriteEvent;
import com.peterlaurence.trekadvisor.util.FileUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * List of recordings.
 *
 * @author peterLaurence on 23/12/17.
 */
public class RecordListView extends CardView {
    private boolean mIsMultiSelectMode = false;
    private ArrayList<File> mSelectedRecordings = new ArrayList<>();
    private ArrayList<File> mRecordings = new ArrayList<>();
    private RecordingAdapter mRecordingAdapter;

    public RecordListView(Context context) {
        this(context, null);
    }

    public RecordListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecordListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        updateRecordings();
        init(context, attrs);
    }

    private void updateRecordings() {
        mRecordings.clear();
        File[] recordings = TrackImporter.getRecordings();
        if (recordings != null) {
            mRecordings.addAll(Arrays.asList(recordings));
        }
    }

    private void init(Context context, AttributeSet attrs) {
        inflate(context, R.layout.record_list_layout, this);

        Context ctx = getContext();
        RecyclerView recyclerView = findViewById(R.id.recordings_recycler_id);
        ImageButton editNameButton = findViewById(R.id.edit_recording_button);
        ImageButton importButton = findViewById(R.id.import_track_button);
        ImageButton deleteRecordingButton = findViewById(R.id.delete_recording_button);

        editNameButton.setEnabled(false);
        editNameButton.setOnClickListener(v -> {
            if (mSelectedRecordings.size() == 1) {
                File recording = mSelectedRecordings.get(0);
                EventBus.getDefault().post(new RequestEditRecording(recording));
            }
        });

        importButton.setEnabled(false);
        importButton.setOnClickListener(v -> EventBus.getDefault().post(new RequestChooseMap()));

        deleteRecordingButton.setOnClickListener(v -> {
            boolean success = true;
            for (File file : mSelectedRecordings) {
                if (file.exists()) {
                    if (file.delete()) {
                        mRecordings.remove(file);
                    } else {
                        success = false;
                    }
                }
            }
            mRecordingAdapter.notifyDataSetChanged();

            /* Alert the user that some files could not be deleted */
            if (!success) {
                Snackbar snackbar = Snackbar.make(getRootView(), R.string.files_could_not_be_deleted,
                        Snackbar.LENGTH_SHORT);
                snackbar.show();
            }
        });

        LinearLayoutManager llm = new LinearLayoutManager(ctx);
        recyclerView.setLayoutManager(llm);

        mRecordingAdapter = new RecordingAdapter(mRecordings, mSelectedRecordings);
        recyclerView.setAdapter(mRecordingAdapter);

        recyclerView.addOnItemTouchListener(new RecyclerItemClickListener(this.getContext(),
                recyclerView, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (mIsMultiSelectMode) {
                    multiSelect(position);

                    mRecordingAdapter.setSelectedRecordings(mSelectedRecordings);
                    mRecordingAdapter.notifyItemChanged(position);
                } else {
                    singleSelect(position);
                    editNameButton.setEnabled(true);
                    editNameButton.getDrawable().setTint(getResources().getColor(R.color.colorAccent, null));

                    importButton.setEnabled(true);
                    importButton.getDrawable().setTint(getResources().getColor(R.color.colorAccent, null));

                    mRecordingAdapter.setSelectedRecordings(mSelectedRecordings);
                    mRecordingAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {
                mSelectedRecordings = new ArrayList<>();
                if (!mIsMultiSelectMode) {
                    mIsMultiSelectMode = true;
                    editNameButton.setEnabled(false);
                    editNameButton.getDrawable().setTint(Color.GRAY);
                    importButton.setEnabled(false);
                    importButton.getDrawable().setTint(Color.GRAY);
                    deleteRecordingButton.setVisibility(View.VISIBLE);
                    multiSelect(position);
                    mRecordingAdapter.setSelectedRecordings(mSelectedRecordings);
                    mRecordingAdapter.notifyDataSetChanged();
                } else {
                    mIsMultiSelectMode = false;
                    deleteRecordingButton.setVisibility(View.GONE);
                    mRecordingAdapter.setSelectedRecordings(mSelectedRecordings);
                    mRecordingAdapter.notifyDataSetChanged();
                }
            }
        }));
    }

    private void multiSelect(int position) {
        File recording = mRecordings.get(position);
        if (mSelectedRecordings.contains(recording)) {
            mSelectedRecordings.remove(recording);
        } else {
            mSelectedRecordings.add(recording);
        }
    }

    private void singleSelect(int position) {
        File recording = mRecordings.get(position);
        mSelectedRecordings.clear();
        mSelectedRecordings.add(recording);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGpxFileWriteEvent(GpxFileWriteEvent event) {
        updateRecordings();
        mRecordingAdapter.notifyDataSetChanged();
    }

    @Subscribe
    public void onRecordingNameChangeEvent(RecordingNameChangeEvent event) {
        for (File recording : mRecordings) {
            if (FileUtils.getFileNameWithoutExtention(recording).equals(event.getInitialValue())) {
                TrackTools.INSTANCE.renameTrack(recording, event.getNewValue());
            }
        }
        updateRecordings();
        mRecordingAdapter.setRecordings(mRecordings);
    }
}
