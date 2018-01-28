package com.peterlaurence.trekadvisor.menu.record.components;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

import com.peterlaurence.trekadvisor.R;
import com.peterlaurence.trekadvisor.core.track.TrackImporter;
import com.peterlaurence.trekadvisor.menu.tools.RecyclerItemClickListener;

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
    private ArrayList<File> mRecordings;

    public RecordListView(Context context) {
        this(context, null);
    }

    public RecordListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecordListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mRecordings = new ArrayList<>(Arrays.asList(TrackImporter.getRecordings()));
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        inflate(context, R.layout.record_list_card, this);

        Context ctx = getContext();
        RecyclerView recyclerView = findViewById(R.id.recordings_recycler_id);
        LinearLayoutManager llm = new LinearLayoutManager(ctx);
        recyclerView.setLayoutManager(llm);

        RecordingAdapter recordingAdapter = new RecordingAdapter(mRecordings, mSelectedRecordings);
        recyclerView.setAdapter(recordingAdapter);

        recyclerView.addOnItemTouchListener(new RecyclerItemClickListener(this.getContext(),
                recyclerView, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (mIsMultiSelectMode) {
                    multiSelect(position);

                    recordingAdapter.setSelectedRecordings(mSelectedRecordings);
                    recordingAdapter.notifyItemChanged(position);
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {
                mSelectedRecordings = new ArrayList<>();
                if (!mIsMultiSelectMode) {
                    mIsMultiSelectMode = true;
                    multiSelect(position);
                    recordingAdapter.setSelectedRecordings(mSelectedRecordings);
                    recordingAdapter.notifyItemChanged(position);
                } else {
                    mIsMultiSelectMode = false;
                    recordingAdapter.setSelectedRecordings(mSelectedRecordings);
                    recordingAdapter.notifyDataSetChanged();
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
}
