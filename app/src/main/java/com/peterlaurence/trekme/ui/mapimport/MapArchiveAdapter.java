package com.peterlaurence.trekme.ui.mapimport;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;

import com.peterlaurence.trekme.R;
import com.peterlaurence.trekme.core.events.MapArchiveListUpdateEvent;
import com.peterlaurence.trekme.core.map.MapArchive;
import com.peterlaurence.trekme.core.map.maparchiver.MapArchiver;
import com.peterlaurence.trekme.core.map.maploader.MapLoader;
import com.peterlaurence.trekme.ui.mapimport.events.UnzipErrorEvent;
import com.peterlaurence.trekme.ui.mapimport.events.UnzipFinishedEvent;
import com.peterlaurence.trekme.ui.mapimport.events.UnzipProgressionEvent;
import com.peterlaurence.trekme.util.UnzipTask;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Adapter to provide access to the data set (here a list of {@link MapArchive}). <br>
 * For example purpose, one of the view components that is only visible when the user extracts a map
 * is loaded using a {@link ViewStub}. So it is only inflated at the very last moment, not at layout
 * inflation.
 *
 * @author peterLaurence on 08/06/16.
 */
public class MapArchiveAdapter extends RecyclerView.Adapter<MapArchiveViewHolder> {

    private List<MapArchive> mMapArchiveList;

    void subscribeEventBus() {
        EventBus.getDefault().register(this);
    }

    void unSubscribeEventBus() {
        EventBus.getDefault().unregister(this);
    }

    @Override
    public MapArchiveViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context ctx = parent.getContext();
        View v = LayoutInflater.from(ctx).inflate(R.layout.map_archive_card, parent, false);

        return new MapArchiveViewHolder(v);
    }

    @Override
    public void onBindViewHolder(MapArchiveViewHolder holder, int position) {
        final MapArchive mapArchive = mMapArchiveList.get(position);
        holder.mArchiveId = mapArchive.getId();
        holder.mapArchiveName.setText(mapArchive.getName());

        holder.importButton.setOnClickListener(new UnzipButtonClickListener(holder, this));
        holder.subscribe();
    }

    @Override
    public int getItemCount() {
        return mMapArchiveList == null ? 0 : mMapArchiveList.size();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMapArchiveListUpdate(MapArchiveListUpdateEvent event) {
        mMapArchiveList = MapLoader.getInstance().getMapArchives();
        if (mMapArchiveList != null) {
            notifyDataSetChanged();
        }
    }

    /**
     * The click listener for a settings button of a {@link MapArchiveViewHolder}
     * It has a reference to the {@link MapArchiveAdapter} as it needs to access the {@link MapArchive} container.
     */
    private static class UnzipButtonClickListener implements View.OnClickListener {
        WeakReference<MapArchiveViewHolder> mMapArchiveViewHolderWeakReference;
        WeakReference<MapArchiveAdapter> mMapArchiveAdapterWeakReference;

        UnzipButtonClickListener(MapArchiveViewHolder mapArchiveViewHolder, MapArchiveAdapter mapArchiveAdapter) {
            mMapArchiveViewHolderWeakReference = new WeakReference<>(mapArchiveViewHolder);
            mMapArchiveAdapterWeakReference = new WeakReference<>(mapArchiveAdapter);
        }

        @Override
        public void onClick(View v) {
            if (mMapArchiveAdapterWeakReference != null && mMapArchiveViewHolderWeakReference != null) {
                MapArchiveViewHolder mapArchiveViewHolder = mMapArchiveViewHolderWeakReference.get();
                MapArchiveAdapter mapArchiveAdapter = mMapArchiveAdapterWeakReference.get();

                if (mapArchiveAdapter != null && mapArchiveViewHolder != null) {
                    mapArchiveViewHolder.init();

                    mapArchiveViewHolder.importButton.setVisibility(View.GONE);
                    final MapArchive mapArchive = mapArchiveAdapter.mMapArchiveList.get(mapArchiveViewHolder.getAdapterPosition());
                    mapArchiveViewHolder.iconMapExtracted.setVisibility(View.GONE);
                    mapArchiveViewHolder.extractionLabel.setVisibility(View.GONE);
                    mapArchiveViewHolder.iconMapExtractionError.setVisibility(View.GONE);
                    mapArchiveViewHolder.iconMapCreated.setVisibility(View.GONE);
                    mapArchiveViewHolder.mapCreationLabel.setVisibility(View.GONE);

                    MapArchiver.archiveMap(mapArchive, new UnzipTask.UnzipProgressionListener() {

                        @Override
                        public void onProgress(int p) {
                            EventBus.getDefault().post(new UnzipProgressionEvent(mapArchive.getId(), p));
                        }

                        @Override
                        public void onUnzipFinished(File outputDirectory) {
                            EventBus.getDefault().post(new UnzipFinishedEvent(mapArchive.getId(), outputDirectory));
                        }

                        @Override
                        public void onUnzipError() {
                            EventBus.getDefault().post(new UnzipErrorEvent(mapArchive.getId()));
                        }
                    });
                }
            }
        }
    }
}
