package com.peterlaurence.trekadvisor.menu.mapimport;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.peterlaurence.trekadvisor.R;
import com.peterlaurence.trekadvisor.core.map.Map;
import com.peterlaurence.trekadvisor.core.map.mapimporter.MapImporter;
import com.peterlaurence.trekadvisor.menu.mapimport.events.UnzipErrorEvent;
import com.peterlaurence.trekadvisor.menu.mapimport.events.UnzipFinishedEvent;
import com.peterlaurence.trekadvisor.menu.mapimport.events.UnzipProgressionEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * @author peterLaurence on 22/12/17.
 */
public class MapArchiveViewHolder extends RecyclerView.ViewHolder implements
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
        cardView = itemView.findViewById(R.id.cv_map_archive);
        mapArchiveName = itemView.findViewById(R.id.map_archive_name);
        progressBarHorizontal = itemView.findViewById(R.id.unzip_progressbar);
        progressBarHorizontal.setMax(100);
        stubProgressBarUnzip = itemView.findViewById(R.id.stub_extraction_ind_progressbar);
        iconMapExtracted = itemView.findViewById(R.id.extraction_done);
        iconMapExtractionError = itemView.findViewById(R.id.extraction_error);
        extractionLabel = itemView.findViewById(R.id.extraction_txtview);
        progressBarIndMapCreation = itemView.findViewById(R.id.mapcreation_ind_progressbar);
        progressBarIndMapCreation.getIndeterminateDrawable().setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
        iconMapCreated = itemView.findViewById(R.id.mapcreation_done);
        mapCreationLabel = itemView.findViewById(R.id.mapcreation_txtview);
        importButton = itemView.findViewById(R.id.unzip_archive_btn);
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
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    void unSubscribe() {
        EventBus.getDefault().unregister(this);
    }
}