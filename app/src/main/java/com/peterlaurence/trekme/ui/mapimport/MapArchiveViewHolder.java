package com.peterlaurence.trekme.ui.mapimport;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.View;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.peterlaurence.trekme.R;
import com.peterlaurence.trekme.core.map.mapimporter.MapImporter;

/**
 * @author peterLaurence on 22/12/17.
 */
public class MapArchiveViewHolder extends RecyclerView.ViewHolder {
    int position;
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

    public void init() {
        progressBarHorizontal.setVisibility(View.GONE);
        if (progressBarIndUnzip != null) {
            progressBarIndUnzip.setVisibility(View.GONE);
        }
        iconMapExtracted.setVisibility(View.GONE);
        iconMapExtractionError.setVisibility(View.GONE);
        extractionLabel.setVisibility(View.GONE);
        progressBarIndMapCreation.setVisibility(View.GONE);
        iconMapCreated.setVisibility(View.GONE);
        mapCreationLabel.setVisibility(View.GONE);
    }

    /**
     * Init views based on view stubs.
     */
    private void initProgressBar() {
        if (progressBarIndUnzip == null) {
            progressBarIndUnzip = (ProgressBar) stubProgressBarUnzip.inflate();
            progressBarIndUnzip.getIndeterminateDrawable().setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
        }
    }

    public void onProgress(int p) {
        initProgressBar();
        progressBarHorizontal.setVisibility(View.VISIBLE);
        progressBarIndUnzip.setVisibility(View.VISIBLE);
        extractionLabel.setVisibility(View.VISIBLE);
        progressBarHorizontal.setProgress(p);
    }

    public void onUnzipFinished() {
        progressBarHorizontal.setVisibility(View.GONE);
        progressBarIndUnzip.setVisibility(View.GONE);
        extractionLabel.setVisibility(View.VISIBLE);
        mapCreationLabel.setVisibility(View.VISIBLE);
        iconMapExtracted.setVisibility(View.VISIBLE);
        progressBarIndMapCreation.setVisibility(View.VISIBLE);
    }

    public void onUnzipError() {
        initProgressBar();
        progressBarIndUnzip.setVisibility(View.GONE);
        iconMapExtractionError.setVisibility(View.VISIBLE);
        extractionLabel.setText(R.string.extraction_error);
    }

    public void onMapImported(MapImporter.MapParserStatus status) {
        switch (status) {
            case EXISTING_MAP:
                mapCreationLabel.setText(R.string.imported_untouched);
                break;
            case UNKNOWN_MAP_ORIGIN:
            case NO_MAP:
                mapCreationLabel.setText(R.string.map_import_error);
                break;
        }
        progressBarIndMapCreation.setVisibility(View.GONE);
        iconMapCreated.setVisibility(View.VISIBLE);
        mapCreationLabel.setVisibility(View.VISIBLE);
    }
}