package com.peterlaurence.trekadvisor.menu.mapimport;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.peterlaurence.trekadvisor.R;
import com.peterlaurence.trekadvisor.core.map.MapArchive;
import com.peterlaurence.trekadvisor.core.map.MapImporter;
import com.peterlaurence.trekadvisor.core.map.MapLoader;
import com.peterlaurence.trekadvisor.util.UnzipTask;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Adapter to provide access to the data set (here a list of {@link MapArchive}).
 *
 * @author peterLaurence on 08/06/16.
 */
public class MapArchiveAdapter extends RecyclerView.Adapter<MapArchiveAdapter.MapArchiveViewHolder>
        implements MapLoader.MapArchiveListUpdateListener {

    private List<MapArchive> mMapArchiveList;

    public static class MapArchiveViewHolder extends RecyclerView.ViewHolder implements UnzipTask.UnzipProgressionListener {
        CardView cardView;
        TextView mapArchiveName;
        ProgressBar progressBar;
        Button unzipButton;

        public MapArchiveViewHolder(View itemView) {
            super(itemView);
            cardView = (CardView) itemView.findViewById(R.id.cv_map_archive);
            mapArchiveName = (TextView) itemView.findViewById(R.id.map_archive_name);
            progressBar = (ProgressBar) itemView.findViewById(R.id.unzip_progressbar);
            progressBar.setMax(100);
            unzipButton = (Button) itemView.findViewById(R.id.unzip_archive_btn);
        }

        @Override
        public void onProgress(int p) {
            progressBar.setProgress(p);
        }

        @Override
        public void onFinished(File outputDirectory) {
            progressBar.setProgress(100);

            /* Import the extracted map */
            // TODO : for instance we only import LIBVIPS maps
            MapImporter.importFromFile(outputDirectory, MapImporter.MapProvider.LIBVIPS, MapLoader.getInstance());
        }
    }

    public MapArchiveAdapter(@Nullable List<MapArchive> mapArchives) {
        mMapArchiveList = mapArchives;
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
        holder.mapArchiveName.setText(mapArchive.getName());

        holder.unzipButton.setOnClickListener(new UnzipButtonClickListener(holder, this));
    }

    @Override
    public int getItemCount() {
        return mMapArchiveList == null ? 0 : mMapArchiveList.size();
    }

    @Override
    public void onMapArchiveListUpdate() {
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
                    MapArchive mapArchive = mapArchiveAdapter.mMapArchiveList.get(mapArchiveViewHolder.getAdapterPosition());
                    mapArchiveViewHolder.progressBar.setVisibility(View.VISIBLE);

                    mapArchive.unZip(mapArchiveViewHolder);
                }
            }
        }
    }
}
