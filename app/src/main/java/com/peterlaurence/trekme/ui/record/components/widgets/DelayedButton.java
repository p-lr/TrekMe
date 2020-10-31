package com.peterlaurence.trekme.ui.record.components.widgets;

import android.content.Context;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import androidx.appcompat.widget.AppCompatImageButton;
import android.util.AttributeSet;

import com.peterlaurence.trekme.R;

/**
 * A button which has two modes : Play and Stop. <br>
 * There is a cooldown between each mode switch, to avoid a quick change of state. <br>
 * This is implemented using Animated Vector Drawables, so the cooldown is defined in the animations.
 *
 * A request to change of state while the button is transitioning will be taken into account only
 * when the animation finishes.
 *
 * @author P.Laurence on 26/12/17.
 */
public class DelayedButton extends AppCompatImageButton {
    private AnimatedVectorDrawable mStopToPLayDrawable;
    private AnimatedVectorDrawable mPlayToStopDrawable;
    private State mState;
    private State mRequestedState;
    private PlayStopListener mListener;
    private Boolean isTransitioning = false;

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
            public void onAnimationStart(Drawable drawable) {
                super.onAnimationStart(drawable);
                isTransitioning = true;
            }

            @Override
            public void onAnimationEnd(Drawable drawable) {
                super.onAnimationEnd(drawable);

                isTransitioning = false;
                checkState();
                setEnabled(true);
            }
        });

        mPlayToStopDrawable.registerAnimationCallback(new Animatable2.AnimationCallback() {
            @Override
            public void onAnimationStart(Drawable drawable) {
                super.onAnimationStart(drawable);
                isTransitioning = true;
            }

            @Override
            public void onAnimationEnd(Drawable drawable) {
                super.onAnimationEnd(drawable);

                isTransitioning = false;
                checkState();
                setEnabled(true);
            }
        });

        setOnClickListener(v -> toggle());
    }

    public void setListener(PlayStopListener listener) {
        mListener = listener;
    }

    /**
     * Request a state change. But it might not happen immediately, in the case the button is
     * transitioning between its two possible states. In this case, the state given as parameter
     * is saved as {@link #mRequestedState} and will be taken into account when the animation
     * finishes.
     */
    public void setMode(State state) {
        post(() -> _setMode(state));
    }

    private void _setMode(State state) {
        if (isTransitioning) {
            mRequestedState = state;
            return;
        }
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
        post(this::_toggle);
    }

    private void _toggle() {
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

    /**
     * If a state change request happened while a transition was running, the change was postponed.
     * Apply it now.
     */
    private void checkState() {
        if (mRequestedState != null && mState != mRequestedState) {
            setMode(mRequestedState);
            mRequestedState = null;
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
