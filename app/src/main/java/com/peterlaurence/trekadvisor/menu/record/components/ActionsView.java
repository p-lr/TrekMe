package com.peterlaurence.trekadvisor.menu.record.components;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;

import com.peterlaurence.trekadvisor.R;
import com.peterlaurence.trekadvisor.menu.events.RecordGpxStartEvent;
import com.peterlaurence.trekadvisor.menu.record.components.events.RequestStartEvent;
import com.peterlaurence.trekadvisor.menu.record.components.events.RequestStopEvent;
import com.peterlaurence.trekadvisor.menu.record.components.widgets.DelayedButton;
import com.peterlaurence.trekadvisor.service.event.LocationServiceStatus;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

/**
 * A set of controls (start & stop) over the {@link com.peterlaurence.trekadvisor.service.LocationService}.
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
        inflate(context, R.layout.record_actions_card, this);

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
     * If there is a subscriber for the {@link RecordGpxStartEvent}, we assume that
     * the {@link com.peterlaurence.trekadvisor.service.LocationService} is started. <br>
     * Else we assume that the service isn't started.
     */
    private void initState() {

        if (EventBus.getDefault().hasSubscriberForEvent(RecordGpxStartEvent.class)) {
            mButton.setMode(DelayedButton.State.STOP);
        } else {
            mButton.setMode(DelayedButton.State.PLAY);
        }
    }

    /**
     * The containing {@link com.peterlaurence.trekadvisor.menu.record.RecordFragment} will catch
     * this event and stop the service.
     */
    private void requestStop() {
        EventBus.getDefault().post(new RequestStopEvent());
    }

    /**
     * The containing {@link com.peterlaurence.trekadvisor.menu.record.RecordFragment} will catch
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
