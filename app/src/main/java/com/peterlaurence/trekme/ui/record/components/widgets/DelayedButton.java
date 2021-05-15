package com.peterlaurence.trekme.ui.record.components.widgets;

import android.content.Context;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;

import com.peterlaurence.trekme.R;

/**
 * A button which has two modes : Play and Stop. <br>
 * There is a cooldown between each mode switch, to avoid a quick change of state. <br>
 * This is implemented using Animated Vector Drawables, so the cooldown is defined in the animations.
 *
 * @author P.Laurence on 26/12/17.
 */
public class DelayedButton extends AppCompatImageButton {
    private final AnimatedVectorDrawable mStopToPLayDrawable;
    private final AnimatedVectorDrawable mPlayToStopDrawable;
    private State mState;
    private PlayStopListener mListener;
    private Boolean isTransitioning = false;
    private static final String STARTED_KEY = "started";
    private static final String SUPER_STATE_KEY = "superState";

    public DelayedButton(Context context) {
        this(context, null);
    }

    public DelayedButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DelayedButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setContentDescription(context.getString(R.string.delayed_button_desc));

        mStopToPLayDrawable = (AnimatedVectorDrawable) ContextCompat.getDrawable(context, R.drawable.avd_delayed_button_stop_to_play);
        mPlayToStopDrawable = (AnimatedVectorDrawable) ContextCompat.getDrawable(context, R.drawable.avd_delayed_button_play_to_stop);

        /* It appears that internally, we get an AnimatedVectorDrawable from a pool, and we might
         * get an instance from a previous usage. So reset it.. */
        mPlayToStopDrawable.reset();

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
                setEnabled(true);
            }
        });

        setOnClickListener(v -> toggle());
    }

    public void setListener(PlayStopListener listener) {
        mListener = listener;
    }

    /**
     * Request a state change, which might be denied, in the case the button is transitioning
     * between its two possible states.
     */
    public void setMode(State state) {
        post(() -> _setMode(state));
    }

    private void _setMode(State state) {
        if (state == mState) return;

        switch (state) {
            case PLAY:
                setEnabled(false);
                mStopToPLayDrawable.reset();
                setImageDrawable(mStopToPLayDrawable);
                mStopToPLayDrawable.start();
                mState = State.PLAY;
                break;
            case STOP:
                setEnabled(false);
                mPlayToStopDrawable.reset();
                setImageDrawable(mPlayToStopDrawable);
                mPlayToStopDrawable.start();
                mState = State.STOP;
                break;
            default:
                // don't care
        }
    }

    public void toggle() {
        post(this::_toggle);
    }

    private void _toggle() {
        if (isTransitioning) return;
        switch (mState) {
            case STOP:
                if (mListener != null) {
                    mListener.onStop();
                }
                break;
            case PLAY:
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

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        Bundle b = new Bundle();
        b.putBoolean(STARTED_KEY, mState == State.STOP);
        b.putParcelable(SUPER_STATE_KEY, superState);
        return b;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Bundle b = (Bundle) state;
        boolean started = b.getBoolean(STARTED_KEY);
        Parcelable superState = b.getParcelable(SUPER_STATE_KEY);
        super.onRestoreInstanceState(superState);
        State st = started ? State.STOP : State.PLAY;
        switch (st) {
            case PLAY:
                mPlayToStopDrawable.reset();
                setImageDrawable(mPlayToStopDrawable);
                break;
            case STOP:
                mStopToPLayDrawable.reset();
                setImageDrawable(mStopToPLayDrawable);
                break;
            default:
                // don't care
        }
        mState = st;
        _setMode(st);
    }
}
