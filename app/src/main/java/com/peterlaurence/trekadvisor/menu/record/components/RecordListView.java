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
    public RecordListView(Context context) {
        this(context, null);
    }

    public RecordListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecordListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        inflate(context, R.layout.record_list_card, this);

        Context ctx = getContext();
        RecyclerView recyclerView = findViewById(R.id.recordings_recycler_id);
        LinearLayoutManager llm = new LinearLayoutManager(ctx);
        recyclerView.setLayoutManager(llm);

        ArrayList<File> selectedItems = new ArrayList<>();

        RecordingAdapter recordingAdapter = new RecordingAdapter(
                new ArrayList<>(Arrays.asList(TrackImporter.getRecordings())), selectedItems);
        recyclerView.setAdapter(recordingAdapter);


        recyclerView.addOnItemTouchListener(new RecyclerItemClickListener(this.getContext(),
                recyclerView, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                System.out.println("click");
            }

            @Override
            public void onItemLongClick(View view, int position) {
                System.out.println("long click");
            }
        }));
    }
}
