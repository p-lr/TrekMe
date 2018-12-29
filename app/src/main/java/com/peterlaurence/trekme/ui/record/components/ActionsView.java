package com.peterlaurence.trekme.ui.record.components;

import android.content.Context;
import androidx.cardview.widget.CardView;
import android.util.AttributeSet;

import com.peterlaurence.trekme.R;
import com.peterlaurence.trekme.ui.record.components.events.RequestStartEvent;
import com.peterlaurence.trekme.ui.record.components.events.RequestStopEvent;
import com.peterlaurence.trekme.ui.record.components.widgets.DelayedButton;
import com.peterlaurence.trekme.service.event.LocationServiceStatus;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

/**
 * A set of controls (start & stop) over the {@link com.peterlaurence.trekme.service.LocationService}.
 *
 * @author peterLaurence on 23/12/17.
 */
public class ActionsView extends CardView {
    private DelayedButton mButton;

    public ActionsView(Context context) {
        this(context, null);
    }

    public ActionsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ActionsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }


    private void init(Context context, AttributeSet attrs) {
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
            mButton.setMode(DelayedButton.State.STOP);
        } else {
            mButton.setMode(DelayedButton.State.PLAY);
        }
    }

    /**
     * The containing {@link com.peterlaurence.trekme.ui.record.RecordFragment} will catch
     * this event and stop the service.
     */
    private void requestStop() {
        EventBus.getDefault().post(new RequestStopEvent());
    }

    /**
     * The containing {@link com.peterlaurence.trekme.ui.record.RecordFragment} will catch
     * this event and start the service.
     */
    private void requestStart() {
        EventBus.getDefault().post(new RequestStartEvent());
    }

    @Subscribe
    public void onLocationServiceStatus(LocationServiceStatus event) {
        // TODO : take into account some errors coming back from the service
    }
}
