package com.peterlaurence.trekme.ui.record.components;

import android.content.Context;
import android.util.AttributeSet;

import androidx.cardview.widget.CardView;

import com.peterlaurence.trekme.R;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * A set of controls (start & stop) over the GPX recording service.
 *
 * @author P.Laurence on 23/12/17.
 */
@AndroidEntryPoint
public class ActionsView extends CardView {

    public ActionsView(Context context) {
        this(context, null);
    }

    public ActionsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ActionsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        inflate(context, R.layout.record_actions_layout, this);
    }
}
