package com.peterlaurence.trekadvisor.menu.mapview.components;

import android.content.Context;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.peterlaurence.trekadvisor.R;

/**
 * @author peterLaurence on 09/04/17.
 */
public class MarkerCallout extends RelativeLayout {
    private Button mMoveButton;

    public MarkerCallout(Context context) {
        super(context);

        inflate(context, R.layout.marker_callout, this);

        mMoveButton = (Button) findViewById(R.id.move_callout_btn);
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
}
