package com.peterlaurence.trekadvisor.menu.mapcalibration;

/**
 * Contract of a calibration model when up to 4 calibration points can be defined.
 *
 * @author peterLaurence on 22/05/16.
 */
public interface CalibrationModel {
    void onFirstCalibrationPointSelected();

    void onSecondCalibrationPointSelected();

    void onThirdCalibrationPointSelected();

    void onFourthCalibrationPointSelected();

    void onWgs84modeChanged(boolean isWgs84);

    int getCalibrationPointNumber();

    void onSave();
}
