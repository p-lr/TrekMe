package com.peterlaurence.trekadvisor.menu.mapview.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.peterlaurence.trekadvisor.R;
import com.peterlaurence.trekadvisor.menu.mapview.MapViewFragment;

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

    public IndicatorOverlay(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.IndicatorOverlay, 0, 0);

        try {
            int color = a.getColor(R.styleable.IndicatorOverlay_backgroundColor, BACKGROUND_COLOR_DEFAULT);
            setBackgroundColor(color);
            System.out.println("back : " + color);
        } finally {
            a.recycle();
        }


        init();
    }

    private void init() {
        inflate(getContext(), R.layout.map_indicator_overlay, this);

        mSpeedTextView = (TextView) findViewById(R.id.speed_id);
    }

    @Override
    public void onSpeed(float speed, String unit) {
        String speedText = String.valueOf(speed) + " " + unit;
        mSpeedTextView.setText(speedText);
    }
}
