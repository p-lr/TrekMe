package com.peterlaurence.trekadvisor.menu.record.components;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.widget.ImageButton;

import com.peterlaurence.trekadvisor.R;
import com.peterlaurence.trekadvisor.menu.events.RecordGpxStartEvent;
import com.peterlaurence.trekadvisor.menu.record.components.events.RequestStartEvent;
import com.peterlaurence.trekadvisor.menu.record.components.events.RequestStopEvent;
import com.peterlaurence.trekadvisor.service.event.LocationServiceStatus;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

/**
 * A set of controls (start & stop) over the {@link com.peterlaurence.trekadvisor.service.LocationService}.
 *
 * @author peterLaurence on 23/12/17.
 */
public class ActionsView extends CardView {
    private ImageButton mButton;
    private State mState = State.WaitingStatus;

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
        inflate(context, R.layout.record_actions_card, this);

        mButton = getRootView().findViewById(R.id.recordControlButton);
        mButton.setOnClickListener(v -> toggleRecord());

        initState();
    }

    /**
     * If there is a subscriber for the {@link RecordGpxStartEvent}, we assume that
     * the {@link com.peterlaurence.trekadvisor.service.LocationService} is started. <br>
     * Else we assume that the service isn't started.
     */
    private void initState() {

        if (EventBus.getDefault().hasSubscriberForEvent(RecordGpxStartEvent.class)) {
            mState = State.ServiceStarted;
        } else {
            mState = State.ServiceStopped;
        }

        processState();
    }

    private void processState() {
        switch (mState) {
            case ServiceStarted:
                mButton.setImageResource(R.drawable.ic_stop_black_24dp);
                mButton.setEnabled(true);
                break;
            case ServiceStopped:
                mButton.setImageResource(R.drawable.ic_play_arrow_black_24dp);
                mButton.setEnabled(true);
                break;
            case WaitingStatus:
                mButton.setEnabled(false);
                break;
            default:
                // don't care
        }
    }

    private void toggleRecord() {
        switch (mState) {
            case ServiceStarted:
                requestStop();
                break;
            case ServiceStopped:
                requestStart();
                break;
            case WaitingStatus:
                /* This should never happen */
                processState();
            default:
                // don't care
        }
    }

    /**
     * The containing {@link com.peterlaurence.trekadvisor.menu.record.RecordFragment} will catch
     * this event and stop the service.
     */
    private void requestStop() {
        EventBus.getDefault().post(new RequestStopEvent());

        /* Then we assume the service is stopped */
        mState = State.ServiceStopped;
        processState();
    }

    /**
     * The containing {@link com.peterlaurence.trekadvisor.menu.record.RecordFragment} will catch
     * this event and start the service.
     */
    private void requestStart() {
        EventBus.getDefault().post(new RequestStartEvent());

        /* Then wait for the start event confirmation */
        mState = State.WaitingStatus;
        processState();
    }

    @Subscribe
    public void onLocationServiceStatus(LocationServiceStatus event) {
        mState = event.started ? State.ServiceStarted : State.ServiceStopped;
        processState();
    }

    private enum State {
        ServiceStarted, ServiceStopped, WaitingStatus
    }
}
