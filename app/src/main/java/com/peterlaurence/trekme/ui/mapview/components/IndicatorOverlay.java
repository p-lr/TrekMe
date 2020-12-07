package com.peterlaurence.trekme.ui.mapview.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.peterlaurence.trekme.R;
import com.peterlaurence.trekme.core.units.UnitFormatter;
import com.peterlaurence.trekme.ui.mapview.DistanceLayer;
import com.peterlaurence.trekme.ui.mapview.MapViewFragment;

/**
 * An overlay to show optional information. It can display :
 * <ul>
 * <li>The current speed</li>
 * <li>The distance between two points</li>
 * </ul>
 *
 * @author P.Laurence on 03/06/17.
 */
public class IndicatorOverlay extends RelativeLayout implements MapViewFragment.SpeedListener,
        DistanceLayer.DistanceListener {
    private static final int BACKGROUND_COLOR_DEFAULT = 0x22FFFFFF;
    private TextView mSpeedTextView;
    private TextView mDistanceTextView;
    private boolean mSpeedVisibility = false;
    private boolean mDistanceVisibility = false;

    public IndicatorOverlay(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.IndicatorOverlay, 0, 0);

        try {
            int color = a.getColor(R.styleable.IndicatorOverlay_backgroundColor, BACKGROUND_COLOR_DEFAULT);
            setBackgroundColor(color);
        } finally {
            a.recycle();
        }

        init(context);
    }

    private void init(Context context) {
        inflate(context, R.layout.map_indicator_overlay, this);

        mSpeedTextView = findViewById(R.id.speed_id);
        mDistanceTextView = findViewById(R.id.distance_id);
    }

    @Override
    public void onSpeed(float speed) {
        if (!mSpeedVisibility) return;

        String speedText = UnitFormatter.INSTANCE.formatSpeed(speed);
        mSpeedTextView.setText(speedText);
    }

    @Override
    public void toggleSpeedVisibility() {
        mSpeedVisibility = !mSpeedVisibility;
        mSpeedTextView.setVisibility(mSpeedVisibility ? VISIBLE : GONE);
        updateVisibility();
    }

    @Override
    public void hideSpeed() {
        mSpeedVisibility = false;
        mSpeedTextView.setVisibility(GONE);
        updateVisibility();
    }

    private void updateVisibility() {
        setVisibility(mSpeedVisibility || mDistanceVisibility ? VISIBLE : GONE);
    }

    @Override
    public void toggleDistanceVisibility() {
        mDistanceVisibility = !mDistanceVisibility;
        mDistanceTextView.setVisibility(mDistanceVisibility ? VISIBLE : GONE);
        updateVisibility();
    }

    @Override
    public void showDistance() {
        mDistanceVisibility = true;
        mDistanceTextView.setVisibility(VISIBLE);
        updateVisibility();
    }

    @Override
    public void hideDistance() {
        mDistanceVisibility = false;
        mDistanceTextView.setVisibility(GONE);
        updateVisibility();
    }

    @Override
    public void onDistance(float distance) {
        String distanceText = UnitFormatter.INSTANCE.formatDistance(distance);
        mDistanceTextView.setText(distanceText);
    }
}
