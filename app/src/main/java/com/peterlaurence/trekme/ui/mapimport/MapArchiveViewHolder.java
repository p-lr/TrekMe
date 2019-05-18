package com.peterlaurence.trekme.ui.mapimport;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.View;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.peterlaurence.trekme.R;
import com.peterlaurence.trekme.core.map.mapimporter.MapImporter;
import com.peterlaurence.trekme.ui.events.MapImportedEvent;
import com.peterlaurence.trekme.ui.mapimport.events.UnzipErrorEvent;
import com.peterlaurence.trekme.ui.mapimport.events.UnzipFinishedEvent;
import com.peterlaurence.trekme.ui.mapimport.events.UnzipProgressionEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

/**
 * @author peterLaurence on 22/12/17.
 */
public class MapArchiveViewHolder extends RecyclerView.ViewHolder {
    int mArchiveId;
    ConstraintLayout layout;
    TextView mapArchiveName;

    /* The indeterminate unzip progressBar and its stub */
    private ViewStub stubProgressBarUnzip;
    private ProgressBar progressBarIndUnzip;

    /* Those view below could also be loaded later using ViewStub */
    private ProgressBar progressBarHorizontal;
    private ImageView iconMapExtracted;
    private ImageView iconMapExtractionError;
    private TextView extractionLabel;
    private ProgressBar progressBarIndMapCreation;
    private ImageView iconMapCreated;
    private TextView mapCreationLabel;


    public MapArchiveViewHolder(View itemView) {
        super(itemView);
        layout = itemView.findViewById(R.id.map_archive_contraint_layout);
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
    }

    /**
     * Init views based on view stubs.
     */
    private void init() {
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMapImported(MapImportedEvent event) {
        if (event.archiveId == mArchiveId) {
            if (event.status == MapImporter.MapParserStatus.EXISTING_MAP) {
                mapCreationLabel.setText(R.string.imported_untouched);
            }
            progressBarIndMapCreation.setVisibility(View.GONE);
            iconMapCreated.setVisibility(View.VISIBLE);
            mapCreationLabel.setVisibility(View.VISIBLE);
        }
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