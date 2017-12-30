package com.peterlaurence.trekadvisor.menu.record.components.widgets;

import android.content.Context;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.AppCompatImageButton;
import android.util.AttributeSet;

import com.peterlaurence.trekadvisor.R;

/**
 * A button which has two modes : Play and Stop. <br>
 * There is a cooldown between each mode switch, to avoid a quick change of state. <br>
 * This is implemented using Animated Vector Drawables, so the cooldown is defined in the animations.
 *
 * @author peterLaurence on 26/12/17.
 */
public class DelayedButton extends AppCompatImageButton {
    private AnimatedVectorDrawable mStopToPLayDrawable;
    private AnimatedVectorDrawable mPlayToStopDrawable;
    private State mState;
    private PlayStopListener mListener;

    public DelayedButton(Context context) {
        this(context, null);
    }

    public DelayedButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DelayedButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mStopToPLayDrawable = (AnimatedVectorDrawable) context.getDrawable(R.drawable.avd_delayed_button_stop_to_play);
        mPlayToStopDrawable = (AnimatedVectorDrawable) context.getDrawable(R.drawable.avd_delayed_button_play_to_stop);
        setImageDrawable(mPlayToStopDrawable);

        mState = State.PLAY;

        mStopToPLayDrawable.registerAnimationCallback(new Animatable2.AnimationCallback() {
            @Override
            public void onAnimationEnd(Drawable drawable) {
                super.onAnimationEnd(drawable);

                setEnabled(true);
            }
        });

        mPlayToStopDrawable.registerAnimationCallback(new Animatable2.AnimationCallback() {
            @Override
            public void onAnimationEnd(Drawable drawable) {
                super.onAnimationEnd(drawable);

                setEnabled(true);
            }
        });

        setOnClickListener(v -> toggle());
    }

    public void setListener(PlayStopListener listener) {
        mListener = listener;
    }

    public void setMode(State state) {
        switch (state) {
            case STOP:
                setImageDrawable(mStopToPLayDrawable);
                mState = State.STOP;
                break;
            case PLAY:
                setImageDrawable(mPlayToStopDrawable);
                mState = State.PLAY;
                break;
            default:
                // don't care
        }
    }

    public void toggle() {
        switch (mState) {
            case STOP:
                setEnabled(false);
                setImageDrawable(mStopToPLayDrawable);
                mStopToPLayDrawable.start();
                mState = State.PLAY;

                if (mListener != null) {
                    mListener.onStop();
                }
                break;

            case PLAY:
                setEnabled(false);
                setImageDrawable(mPlayToStopDrawable);
                mPlayToStopDrawable.start();
                mState = State.STOP;

                if (mListener != null) {
                    mListener.onPlay();
                }
                break;

            default:
                // don't care
        }
    }

    public enum State {
        STOP, PLAY
    }

    public interface PlayStopListener {
        void onPlay();

        void onStop();
    }
}
