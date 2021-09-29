package com.peterlaurence.trekme.ui.record.components;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.peterlaurence.trekme.R;
import com.peterlaurence.trekme.ui.record.components.widgets.HeartBeatIndicator;

/**
 * A View-Model that reports the status (started/stopped) of the
 * {@link com.peterlaurence.trekme.service.GpxRecordService}.
 *
 * @author P.Laurence on 23/12/17.
 */
public class StatusView extends CardView {
    private HeartBeatIndicator mHeartBeatIndicator;
    private TextView mStatusText;

    public StatusView(Context context) {
        this(context, null);
    }

    public StatusView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StatusView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        inflate(context, R.layout.record_status_layout, this);

        mHeartBeatIndicator = findViewById(R.id.heartBeatIndicator);
        mStatusText = findViewById(R.id.record_status_subtitle);
    }

    public void onServiceStarted() {
        mHeartBeatIndicator.beat();
        mStatusText.setText(getResources().getText(R.string.recording_status_started));
    }

    public void onServiceStopped() {
        mHeartBeatIndicator.off();
        mStatusText.setText(getResources().getText(R.string.recording_status_stopped));
    }

    public void onServicePaused() {
        mHeartBeatIndicator.off();
        mStatusText.setText(getResources().getText(R.string.recording_status_paused));
    }
}
