package com.peterlaurence.trekme.ui.record.components;

import android.content.Context;
import androidx.cardview.widget.CardView;
import android.util.AttributeSet;

import com.peterlaurence.trekme.R;
import com.peterlaurence.trekme.ui.record.components.widgets.HeartBeatIndicator;
import com.peterlaurence.trekme.service.event.LocationServiceStatus;

import org.greenrobot.eventbus.EventBus;

/**
 * A View-Model that reports the status (started/stopped) of the
 * {@link com.peterlaurence.trekme.service.LocationService}.
 *
 * @author peterLaurence on 23/12/17.
 */
public class StatusView extends CardView {
    private HeartBeatIndicator mHeartBeatIndicator;

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

        initState();
    }

    /**
     * If the sticky {@link LocationServiceStatus} event reports that the
     * {@link com.peterlaurence.trekme.service.LocationService} is started, we init the view
     * accordingly.
     */
    private void initState() {
        LocationServiceStatus event = EventBus.getDefault().getStickyEvent(LocationServiceStatus.class);
        if (event != null && event.started) {
            onServiceStarted();
        } else {
            onServiceStopped();
        }
    }

    public void onServiceStarted() {
        mHeartBeatIndicator.beat();
    }

    public void onServiceStopped() {
        mHeartBeatIndicator.off();
    }
}
