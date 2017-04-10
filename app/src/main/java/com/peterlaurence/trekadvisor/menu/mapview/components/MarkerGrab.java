package com.peterlaurence.trekadvisor.menu.mapview.components;

import android.content.Context;
import android.support.v7.widget.AppCompatImageView;

import com.peterlaurence.trekadvisor.R;

/**
 * An marker meant to cast its move so it can be used to move other views that are e.g to small to
 * be dragged easily.
 *
 * @author peterLaurence on 10/04/17.
 */
public class MarkerGrab extends AppCompatImageView {

    public MarkerGrab(Context context) {
        super(context);

        setImageDrawable(context.getDrawable(R.drawable.vd_marker_circle_grab));
    }
}
