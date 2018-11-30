package com.peterlaurence.trekme.ui.mapcalibration;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.peterlaurence.trekme.R;

/**
 * Layout for {@link MapCalibrationFragment}.
 *
 * @author peterLaurence on 22/05/16.
 */
public class MapCalibrationLayout extends LinearLayout implements MapCalibrationFragment.MapCalibrationView {
    private EditText mEditTextLat;
    private EditText mEditTextLng;
    private TextView mLatLabel;
    private TextView mLonLabel;
    private String mLatLabelTxt;
    private String mLonLabelTxt;
    private String mProjXLabelTxt;
    private String mProjYLabelTxt;

    /* WGS84 switch */
    private Switch mWgs84Switch;
    private TextView mWgs84SwitchLabel;

    /* CalibrationPointSelector */
    private ImageButton mFirstCalibrationPointButton;
    private ImageButton mSecondCalibrationPointButton;
    private ImageButton mThirdCalibrationPointButton;
    private ImageButton mFourthCalibrationPointButton;
    CalibrationModel mCalibrationModel;

    /* Save button */
    private ImageButton mSaveButton;

    public MapCalibrationLayout(Context context) {
        this(context, null);
    }

    public MapCalibrationLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MapCalibrationLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public MapCalibrationLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        inflate(context, R.layout.fragment_map_calibration, this);
        setOrientation(VERTICAL);

        mEditTextLat = findViewById(R.id.editTextLat);
        mEditTextLng = findViewById(R.id.editTextLng);

        mLatLabel = findViewById(R.id.calibration_y_label);
        mLonLabel = findViewById(R.id.calibration_x_label);

        mLatLabelTxt = context.getString(R.string.latitude_short);
        mLonLabelTxt = context.getString(R.string.longitude_short);

        mProjXLabelTxt = context.getString(R.string.projected_x_short);
        mProjYLabelTxt = context.getString(R.string.projected_y_short);

        mWgs84Switch = findViewById(R.id.wgs84_switch);
        mWgs84SwitchLabel = findViewById(R.id.wgs84_switch_label);

        mFirstCalibrationPointButton = findViewById(R.id.firstCalibPointButton);
        mSecondCalibrationPointButton = findViewById(R.id.secondCalibPointButton);
        mThirdCalibrationPointButton = findViewById(R.id.thirdCalibPointButton);
        mFourthCalibrationPointButton = findViewById(R.id.fourthCalibPointButton);

        mSaveButton = findViewById(R.id.calibration_save);
    }

    @Override
    public void updateCoordinateFields(double x, double y) {
        mEditTextLat.setText(String.valueOf(y));
        mEditTextLng.setText(String.valueOf(x));
    }

    @Override
    public void setCalibrationModel(CalibrationModel l) {
        mCalibrationModel = l;
    }

    @Override
    public void setup() {
        setupCalibrationPointSelector();
        setupSaveButton();
        setupSwitch();
    }

    @Override
    public void noProjectionDefined() {
        mWgs84Switch.setVisibility(View.GONE);
        mWgs84SwitchLabel.setEnabled(false);
        mWgs84SwitchLabel.setVisibility(View.GONE);
    }

    @Override
    public void projectionDefined() {
        mWgs84Switch.setVisibility(View.VISIBLE);
        mWgs84Switch.setEnabled(true);
        mWgs84SwitchLabel.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean isWgs84() {
        return mWgs84Switch.isChecked();
    }

    @Override
    public double getXValue() {
        try {
            return Double.valueOf(mEditTextLng.getText().toString());
        } catch (NumberFormatException e) {
            // alert the user
            return Double.MAX_VALUE;
        }
    }

    @Override
    public double getYValue() {
        try {
            return Double.valueOf(mEditTextLat.getText().toString());
        } catch (NumberFormatException e) {
            // alert the user
            return Double.MAX_VALUE;
        }
    }

    /* By default, select the first calibration point */
    @Override
    public void setDefault() {
        mFirstCalibrationPointButton.callOnClick();
    }

    private void setupCalibrationPointSelector() {
        /* The first two buttons are always enabled */
        mFirstCalibrationPointButton.setOnClickListener(v -> {
            mFirstCalibrationPointButton.getDrawable().setTint(getContext().getColor(R.color.colorAccent));
            mSecondCalibrationPointButton.getDrawable().setTint(Color.BLACK);
            mThirdCalibrationPointButton.getDrawable().setTint(Color.BLACK);
            mFourthCalibrationPointButton.getDrawable().setTint(Color.BLACK);
            mCalibrationModel.onFirstCalibrationPointSelected();
        });

        mSecondCalibrationPointButton.setOnClickListener(v -> {
            mSecondCalibrationPointButton.getDrawable().setTint(getContext().getColor(R.color.colorAccent));
            mFirstCalibrationPointButton.getDrawable().setTint(Color.BLACK);
            mThirdCalibrationPointButton.getDrawable().setTint(Color.BLACK);
            mFourthCalibrationPointButton.getDrawable().setTint(Color.BLACK);
            mCalibrationModel.onSecondCalibrationPointSelected();
        });

        /* Disable/enable other buttons if necessary */
        if (mCalibrationModel.getCalibrationPointNumber() > 2) {
            mThirdCalibrationPointButton.setEnabled(true);
            mThirdCalibrationPointButton.setAlpha(1f);
            mThirdCalibrationPointButton.setOnClickListener(v -> {
                mThirdCalibrationPointButton.getDrawable().setTint(getContext().getColor(R.color.colorAccent));
                mFirstCalibrationPointButton.getDrawable().setTint(Color.BLACK);
                mSecondCalibrationPointButton.getDrawable().setTint(Color.BLACK);
                mFourthCalibrationPointButton.getDrawable().setTint(Color.BLACK);
                mCalibrationModel.onThirdCalibrationPointSelected();
            });
        } else {
            mThirdCalibrationPointButton.setEnabled(false);
            mThirdCalibrationPointButton.setAlpha(0.4f);
        }
        if (mCalibrationModel.getCalibrationPointNumber() > 3) {
            mFourthCalibrationPointButton.setEnabled(true);
            mFourthCalibrationPointButton.setAlpha(1f);
            mFourthCalibrationPointButton.setOnClickListener(v -> {
                mFourthCalibrationPointButton.getDrawable().setTint(getContext().getColor(R.color.colorAccent));
                mFirstCalibrationPointButton.getDrawable().setTint(Color.BLACK);
                mSecondCalibrationPointButton.getDrawable().setTint(Color.BLACK);
                mThirdCalibrationPointButton.getDrawable().setTint(Color.BLACK);
                mCalibrationModel.onFourthCalibrationPointSelected();
            });
        } else {
            mFourthCalibrationPointButton.setEnabled(false);
            mFourthCalibrationPointButton.setAlpha(0.4f);
        }
    }

    private void setupSaveButton() {
        mSaveButton.setOnClickListener(v -> mCalibrationModel.onSave());
    }

    private void setupSwitch() {
        mWgs84Switch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mLatLabel.setText(isChecked ? mLatLabelTxt : mProjYLabelTxt);
            mLonLabel.setText(isChecked ? mLonLabelTxt : mProjXLabelTxt);
            mCalibrationModel.onWgs84modeChanged(isChecked);
        });
    }
}
