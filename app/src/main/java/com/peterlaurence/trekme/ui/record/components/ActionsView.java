package com.peterlaurence.trekme.ui.record.components;

import android.content.Context;
import android.util.AttributeSet;

import androidx.cardview.widget.CardView;

import com.peterlaurence.trekme.R;
import com.peterlaurence.trekme.ui.record.components.widgets.DelayedButton;
import com.peterlaurence.trekme.ui.record.events.RecordEventBus;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * A set of controls (start & stop) over the GPX recording service.
 *
 * @author P.Laurence on 23/12/17.
 */
@AndroidEntryPoint
public class ActionsView extends CardView {

    @Inject
    public RecordEventBus mEventBus;

    private DelayedButton mButton;

    public ActionsView(Context context) {
        this(context, null);
    }

    public ActionsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ActionsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }


    private void init(Context context) {
        inflate(context, R.layout.record_actions_layout, this);

        mButton = getRootView().findViewById(R.id.delayedButton);
        mButton.setListener(new DelayedButton.PlayStopListener() {
            @Override
            public void onPlay() {
                requestStart();
            }

            @Override
            public void onStop() {
                requestStop();
            }
        });
    }

    public void onServiceStarted() {
        mButton.setMode(DelayedButton.State.STOP);
    }

    public void onServiceStopped() {
        mButton.setMode(DelayedButton.State.PLAY);
    }

    /**
     * The containing {@link com.peterlaurence.trekme.ui.record.RecordFragment} will catch
     * this event and stop the service.
     */
    private void requestStop() {
        mEventBus.stopGpxRecording();
    }

    /**
     * The containing {@link com.peterlaurence.trekme.ui.record.RecordFragment} will catch
     * this event and start the service.
     */
    private void requestStart() {
        mEventBus.startGpxRecording();
    }
}
