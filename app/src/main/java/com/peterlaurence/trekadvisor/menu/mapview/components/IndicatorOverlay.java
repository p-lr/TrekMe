package com.peterlaurence.trekadvisor.menu.mapview.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.peterlaurence.trekadvisor.R;
import com.peterlaurence.trekadvisor.menu.mapview.MapViewFragment;

import java.text.NumberFormat;

/**
 * An overlay to show optional information. It can display :
 * <ul>
 * <li>The current speed</li>
 * </ul>
 *
 * @author peterLaurence on 03/06/17.
 */
public class IndicatorOverlay extends LinearLayout implements MapViewFragment.SpeedListener {
    private static final int BACKGROUND_COLOR_DEFAULT = 0x22FFFFFF;
    private TextView mSpeedTextView;
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

        mSpeedTextView = (TextView) findViewById(R.id.speed_id);
    }

    @Override
    public void onSpeed(float speed, SpeedUnit unit) {
        if (!mSpeedVisibility) return;

        float speedConverted = convertSpeed(speed, unit);

        /* Format the speed with only one fraction digit */
        NumberFormat formatter = NumberFormat.getNumberInstance();
        formatter.setMinimumFractionDigits(1);
        formatter.setMaximumFractionDigits(1);

        String speedText = formatter.format(speedConverted) + " " + getSpeedUnitI18n(unit);
        mSpeedTextView.setText(speedText);
    }

    @Override
    public void toggleSpeedVisibility() {
        mSpeedVisibility = !mSpeedVisibility;
        mSpeedTextView.setVisibility(mSpeedVisibility ? VISIBLE : GONE);
        updateVisibility();
    }

    private void updateVisibility() {
        setVisibility(mSpeedVisibility || mDistanceVisibility ? VISIBLE : GONE);
    }

    /**
     * Converts the given speed (assumed to be in m/s), using the provided unit.
     */
    private float convertSpeed(float speed, SpeedUnit unit) {
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
     * Converts a given {@link SpeedUnit} using the user's language.
     */
    private String getSpeedUnitI18n(SpeedUnit unit) {
        Context context = getContext();
        switch (unit) {
            case KM_H:
                return context.getString(R.string.km_h);
            case MPH:
                return context.getString(R.string.mph);
            default:
                return context.getString(R.string.km_h);
        }
    }

    public enum SpeedUnit {
        KM_H, MPH
    }
}
