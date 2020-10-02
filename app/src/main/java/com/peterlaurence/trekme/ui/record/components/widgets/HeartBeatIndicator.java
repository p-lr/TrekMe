package com.peterlaurence.trekme.ui.record.components.widgets;

import android.content.Context;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.VectorDrawable;
import androidx.appcompat.widget.AppCompatImageButton;
import android.util.AttributeSet;

import com.peterlaurence.trekme.R;

/**
 * An indicator which has two states : <br>
 * <ul>
 * <li> heart beating</li>
 * <li>heart stopped</li>
 * </ul>
 *
 * @author P.Laurence on 30/12/17.
 */
public class HeartBeatIndicator extends AppCompatImageButton {
    private AnimatedVectorDrawable mHeartBeatVectorDrawable;
    private VectorDrawable mCircleGray;

    public HeartBeatIndicator(Context context) {
        this(context, null);
    }

    public HeartBeatIndicator(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HeartBeatIndicator(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mHeartBeatVectorDrawable = (AnimatedVectorDrawable) context.getDrawable(
                R.drawable.avd_heartbeat);
        mCircleGray = (VectorDrawable) context.getDrawable(R.drawable.vd_circle_gray);

        /* Start in off mode */
        off();
    }

    public void beat() {
        setImageDrawable(mHeartBeatVectorDrawable);
        mHeartBeatVectorDrawable.start();
    }

    public void off() {
        if (mHeartBeatVectorDrawable.isRunning()) {
            mHeartBeatVectorDrawable.stop();
        }
        setImageDrawable(mCircleGray);
    }
}
