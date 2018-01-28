package com.peterlaurence.trekadvisor.menu.record.components;

import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.peterlaurence.trekadvisor.R;

import java.io.File;
import java.util.ArrayList;

/**
 * Adapter class for recordings. It holds the view logic associated with the {@link RecyclerView}
 * defined in the {@link RecordListView}.
 *
 * @author peterLaurence on 27/01/18.
 */
public class RecordingAdapter extends RecyclerView.Adapter<RecordingAdapter.RecordingViewHolder> {
    private ArrayList<File> mRecordings;
    private ArrayList<File> mSelectedRecordings;


    RecordingAdapter(ArrayList<File> recordings, ArrayList<File> selectedRecordings) {
        mRecordings = recordings;
        mSelectedRecordings = selectedRecordings;
    }

    @Override
    public RecordingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.record_item, parent, false);
        return new RecordingViewHolder(v);
    }

    @Override
    public void onBindViewHolder(RecordingViewHolder holder, int position) {
        holder.recordingName.setText(mRecordings.get(position).getName());

        if (mSelectedRecordings.contains(mRecordings.get(position))) {
            holder.layout.setBackgroundColor(0x882196F3);
        } else {
            if (position % 2 == 0) {
                holder.layout.setBackgroundColor(0xFFEDEDED);
            } else {
                holder.layout.setBackgroundColor(0xFFFFFFFF);
            }
        }
    }

    @Override
    public int getItemCount() {
        return mRecordings.size();
    }

    static class RecordingViewHolder extends RecyclerView.ViewHolder {
        ConstraintLayout layout;
        TextView recordingName;

        RecordingViewHolder(View itemView) {
            super(itemView);

            layout = itemView.findViewById(R.id.record_item_layout);
            recordingName = itemView.findViewById(R.id.recording_name_id);
        }
    }

    void setRecordings(ArrayList<File> recordings) {
        mRecordings = recordings;
    }

    void setSelectedRecordings(ArrayList<File> selectedRecordings) {
        mSelectedRecordings = selectedRecordings;
    }
}
