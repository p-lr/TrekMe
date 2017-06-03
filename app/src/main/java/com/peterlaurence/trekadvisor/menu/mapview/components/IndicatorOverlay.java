package com.peterlaurence.trekadvisor.menu.mapview.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.peterlaurence.trekadvisor.R;

/**
 * An overlay to show optional information. It can display :
 * <ul>
 * <li>The current speed</li>
 * </ul>
 *
 * @author peterLaurence on 03/06/17.
 */
public class IndicatorOverlay extends LinearLayout {
    private static final int BACKGROUND_COLOR_DEFAULT = 0x22FFFFFF;

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
    }


}
