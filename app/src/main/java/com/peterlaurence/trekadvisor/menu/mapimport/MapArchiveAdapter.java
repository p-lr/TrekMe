package com.peterlaurence.trekadvisor.menu.mapimport;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.peterlaurence.trekadvisor.R;
import com.peterlaurence.trekadvisor.core.map.Map;
import com.peterlaurence.trekadvisor.core.map.MapArchive;
import com.peterlaurence.trekadvisor.core.map.maparchiver.MapArchiver;
import com.peterlaurence.trekadvisor.core.map.mapimporter.MapImporter;
import com.peterlaurence.trekadvisor.core.map.maploader.MapLoader;
import com.peterlaurence.trekadvisor.menu.mapimport.events.UnzipErrorEvent;
import com.peterlaurence.trekadvisor.menu.mapimport.events.UnzipFinishedEvent;
import com.peterlaurence.trekadvisor.menu.mapimport.events.UnzipProgressionEvent;
import com.peterlaurence.trekadvisor.util.UnzipTask;

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
public class MapArchiveAdapter extends RecyclerView.Adapter<MapArchiveAdapter.MapArchiveViewHolder>
        implements MapLoader.MapArchiveListUpdateListener {

    private List<MapArchive> mMapArchiveList;

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

    @Override
    public void onMapArchiveListUpdate() {
        mMapArchiveList = MapLoader.getInstance().getMapArchives();
        if (mMapArchiveList != null) {
            notifyDataSetChanged();
        }
    }

    public static class MapArchiveViewHolder extends RecyclerView.ViewHolder implements
            MapImporter.MapImportListener {
        int mArchiveId;
        CardView cardView;
        TextView mapArchiveName;
        Button importButton;

        /* The indeterminate unzip progressBar and its stub */
        ViewStub stubProgressBarUnzip;
        ProgressBar progressBarIndUnzip;

        /* Those view below could also be loaded later using ViewStub */
        ProgressBar progressBarHorizontal;
        ImageView iconMapExtracted;
        ImageView iconMapExtractionError;
        TextView extractionLabel;
        ProgressBar progressBarIndMapCreation;
        ImageView iconMapCreated;
        TextView mapCreationLabel;


        public MapArchiveViewHolder(View itemView) {
            super(itemView);
            cardView = (CardView) itemView.findViewById(R.id.cv_map_archive);
            mapArchiveName = (TextView) itemView.findViewById(R.id.map_archive_name);
            progressBarHorizontal = (ProgressBar) itemView.findViewById(R.id.unzip_progressbar);
            progressBarHorizontal.setMax(100);
            stubProgressBarUnzip = (ViewStub) itemView.findViewById(R.id.stub_extraction_ind_progressbar);
            iconMapExtracted = (ImageView) itemView.findViewById(R.id.extraction_done);
            iconMapExtractionError = (ImageView) itemView.findViewById(R.id.extraction_error);
            extractionLabel = (TextView) itemView.findViewById(R.id.extraction_txtview);
            progressBarIndMapCreation = (ProgressBar) itemView.findViewById(R.id.mapcreation_ind_progressbar);
            progressBarIndMapCreation.getIndeterminateDrawable().setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
            iconMapCreated = (ImageView) itemView.findViewById(R.id.mapcreation_done);
            mapCreationLabel = (TextView) itemView.findViewById(R.id.mapcreation_txtview);
            importButton = (Button) itemView.findViewById(R.id.unzip_archive_btn);
        }

        /**
         * Init views based on view stubs.
         */
        void init() {
            if (progressBarIndUnzip == null) {
                progressBarIndUnzip = (ProgressBar) stubProgressBarUnzip.inflate();
                progressBarIndUnzip.getIndeterminateDrawable().setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
            }
        }

        @Subscribe(threadMode = ThreadMode.MAIN)
        public void onProgressionEvent(UnzipProgressionEvent event) {
            if (event.archiveId == mArchiveId) {
                init();
                progressBarHorizontal.setVisibility(View.VISIBLE);
                progressBarIndUnzip.setVisibility(View.VISIBLE);
                extractionLabel.setVisibility(View.VISIBLE);
                progressBarHorizontal.setProgress(event.progression);
            }
        }

        @Subscribe(threadMode = ThreadMode.MAIN)
        public void onUnzipFinishedEvent(UnzipFinishedEvent event) {
            if (event.archiveId == mArchiveId) {
                progressBarHorizontal.setVisibility(View.GONE);
                progressBarIndUnzip.setVisibility(View.GONE);
                extractionLabel.setVisibility(View.VISIBLE);
                mapCreationLabel.setVisibility(View.VISIBLE);
                iconMapExtracted.setVisibility(View.VISIBLE);

                /* Import the extracted map */
                // TODO : for instance we only import LIBVIPS maps
                MapImporter.importFromFile(event.outputFolder, MapImporter.MapProvider.LIBVIPS, this);
                progressBarIndMapCreation.setVisibility(View.VISIBLE);
            }
        }

        @Subscribe(threadMode = ThreadMode.MAIN)
        public void onUnzipError(UnzipErrorEvent event) {
            if (event.archiveId == mArchiveId) {
                progressBarIndUnzip.setVisibility(View.GONE);
                iconMapExtractionError.setVisibility(View.VISIBLE);
                extractionLabel.setText(R.string.extraction_error);
            }
        }

        @Override
        public void onMapImported(Map map, MapImporter.MapParserStatus status) {
            if (status == MapImporter.MapParserStatus.EXISTING_MAP) {
                mapCreationLabel.setText(R.string.imported_untouched);
            }
            progressBarIndMapCreation.setVisibility(View.GONE);
            iconMapCreated.setVisibility(View.VISIBLE);
            mapCreationLabel.setVisibility(View.VISIBLE);
            importButton.setVisibility(View.VISIBLE);
        }

        @Override
        public void onMapImportError(MapImporter.MapParseException e) {
            // TODO : show an error message that something went wrong
        }

        void subscribe() {
            EventBus.getDefault().register(this);
        }

        void unSubscribe() {
            EventBus.getDefault().unregister(this);
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
