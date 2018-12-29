package com.peterlaurence.trekme.ui.mapview.components;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.peterlaurence.trekme.R;
import com.peterlaurence.trekme.ui.mapview.DistanceLayer;
import com.peterlaurence.trekme.ui.mapview.MapViewFragment;

import java.text.NumberFormat;

/**
 * An overlay to show optional information. It can display :
 * <ul>
 * <li>The current speed</li>
 * <li>The distance between two points</li>
 * </ul>
 *
 * @author peterLaurence on 03/06/17.
 */
public class IndicatorOverlay extends RelativeLayout implements MapViewFragment.SpeedListener,
        DistanceLayer.DistanceListener {
    private static final int BACKGROUND_COLOR_DEFAULT = 0x22FFFFFF;
    private final String km_h_I18n;
    private final String mph_I18n;
    private final String km_I18n;
    private final String m_I18n;
    private TextView mSpeedTextView;
    private TextView mDistanceTextView;
    private boolean mSpeedVisibility = false;
    private boolean mDistanceVisibility = false;
    private NumberFormat mSpeedFormatter;
    private NumberFormat mDistanceFormatter;

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

        /* Formatter for the speed (only one fraction digit) */
        mSpeedFormatter = NumberFormat.getNumberInstance();
        mSpeedFormatter.setMinimumFractionDigits(1);
        mSpeedFormatter.setMaximumFractionDigits(1);

        mDistanceFormatter = NumberFormat.getNumberInstance();

        /* I18n strings */
        km_h_I18n = context.getString(R.string.km_h);
        mph_I18n = context.getString(R.string.mph);
        km_I18n = context.getString(R.string.km);
        m_I18n = context.getString(R.string.m);

        init(context);
    }

    private void init(Context context) {
        inflate(context, R.layout.map_indicator_overlay, this);

        mSpeedTextView = (TextView) findViewById(R.id.speed_id);
        mDistanceTextView = (TextView) findViewById(R.id.distance_id);
    }

    @Override
    public void onSpeed(float speed, MapViewFragment.SpeedUnit unit) {
        if (!mSpeedVisibility) return;

        float speedConverted = convertSpeed(speed, unit);

        String speedText = mSpeedFormatter.format(speedConverted) + " " + getSpeedUnitI18n(unit);
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

    /**
     * Converts the given speed (assumed to be in m/s), using the provided unit.
     */
    private float convertSpeed(float speed, MapViewFragment.SpeedUnit unit) {
        switch (unit) {
            case KM_H:
                return speed * 3.6f;
            case MPH:
                return speed * 2.23694f;
            default:
                return speed;
        }
    }

    /**
     * Converts a given {@link MapViewFragment.SpeedUnit} using the user's language.
     */
    private String getSpeedUnitI18n(MapViewFragment.SpeedUnit unit) {
        switch (unit) {
            case KM_H:
                return km_h_I18n;
            case MPH:
                return mph_I18n;
            default:
                return km_h_I18n;
        }
    }

    @Override
    public void toggleDistanceVisibility() {
        mDistanceVisibility = !mDistanceVisibility;
        mDistanceTextView.setVisibility(mDistanceVisibility ? VISIBLE : GONE);
        updateVisibility();
    }

    @Override
    public void hideDistance() {
        mDistanceVisibility = false;
        mDistanceTextView.setVisibility(GONE);
        updateVisibility();
    }

    @Override
    public void onDistance(float distance, DistanceLayer.DistanceUnit unit) {

        String distanceText = null;
        if (unit == null) {
            distanceText = representDistance(distance);
        } else {
            // TODO : implement when a unit is specified
        }
        mDistanceTextView.setText(distanceText);
    }

    /**
     * Convert a distance assumed to be in meters, into its {@link String} representation.
     */
    private String representDistance(float distance) {
        if (distance >= 1000) {
            mDistanceFormatter.setMaximumFractionDigits(3);
            return mDistanceFormatter.format(distance / 1000f) + " " + km_I18n;
        } else {
            mDistanceFormatter.setMaximumFractionDigits(0);
            return mDistanceFormatter.format(distance) + " " + m_I18n;
        }
    }
}
