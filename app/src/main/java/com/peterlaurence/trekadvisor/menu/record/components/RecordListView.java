package com.peterlaurence.trekadvisor.menu.record.components;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;

import com.peterlaurence.trekadvisor.R;

/**
 * @author peterLaurence on 23/12/17.
 */
public class RecordListView extends CardView {
    public RecordListView(Context context) {
        this(context, null);
    }

    public RecordListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecordListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        inflate(context, R.layout.record_list_card, this);
    }
}
