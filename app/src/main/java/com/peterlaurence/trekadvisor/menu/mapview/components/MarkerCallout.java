package com.peterlaurence.trekadvisor.menu.mapview.components;

import android.content.Context;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.peterlaurence.trekadvisor.R;

import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * @author peterLaurence on 09/04/17.
 */
public class MarkerCallout extends RelativeLayout {
    private ImageButton mMoveButton;
    private ImageButton mEditButton;
    private ImageButton mDeleteButton;
    private TextView mTitle;
    private TextView mSubTitle;

    public MarkerCallout(Context context) {
        super(context);

        inflate(context, R.layout.marker_callout, this);

        mMoveButton = (ImageButton) findViewById(R.id.move_callout_btn);
        mEditButton = (ImageButton) findViewById(R.id.edit_callout_btn);
        mDeleteButton = (ImageButton) findViewById(R.id.delete_callout_btn);
        mTitle = (TextView) findViewById(R.id.callout_title);
        mSubTitle = (TextView) findViewById(R.id.callout_subtitle);

        mMoveButton.getDrawable().setTint(getContext().getColor(R.color.colorAccent));
        mEditButton.getDrawable().setTint(getContext().getColor(R.color.colorAccent));
        mDeleteButton.getDrawable().setTint(getContext().getColor(R.color.colorAccent));
    }

    public void transitionIn() {
        ScaleAnimation scaleAnimation = new ScaleAnimation(0, 1, 0, 1, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 1f);
        scaleAnimation.setInterpolator(new OvershootInterpolator(1.2f));
        scaleAnimation.setDuration(250);

        AlphaAnimation alphaAnimation = new AlphaAnimation(0, 1f);
        alphaAnimation.setDuration(200);

        AnimationSet animationSet = new AnimationSet(false);

        animationSet.addAnimation(scaleAnimation);
        animationSet.addAnimation(alphaAnimation);

        startAnimation(animationSet);
    }

    public void setMoveAction(final Runnable moveAction) {
        mMoveButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                moveAction.run();
            }
        });
    }

    public void setEditAction(final Runnable editAction) {
        mEditButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                editAction.run();
            }
        });
    }

    public void setDeleteAction(final Runnable deleteAction) {
        mDeleteButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteAction.run();
            }
        });
    }

    public void setTitle(String title) {
        mTitle.setText(title);
    }

    public void setSubTitle(String subtitle) {
        mSubTitle.setText(subtitle);
    }

    public void setSubTitle(double lat, double lon) {
        DecimalFormat df = new DecimalFormat("#.####");
        df.setRoundingMode(RoundingMode.CEILING);

        /* Note the the compiler uses StringBuilder under the hood */
        setSubTitle("lat : " + df.format(lat) + "  " + "lon : " + df.format(lon));
    }
}
