package com.peterlaurence.trekadvisor.core.download;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

import com.peterlaurence.trekadvisor.menu.maplist.dialogs.MapDownloadDialog;

/**
 * @author peterLaurence on 08/10/17.
 */
public class DownloadReceiver extends ResultReceiver {
    private MapDownloadDialog mMapDownloadDialog;

    public DownloadReceiver(Handler handler, MapDownloadDialog mapDownloadDialog) {
        super(handler);

        mMapDownloadDialog = mapDownloadDialog;
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        super.onReceiveResult(resultCode, resultData);
        if (resultCode == DownloadService.UPDATE_PROGRESS) {
            int progress = resultData.getInt(DownloadService.PROGRESS_SIG);
            mMapDownloadDialog.setProgress(progress);
            if (progress == 100) {
                try {
                    mMapDownloadDialog.dismiss();
                } catch (NullPointerException e) {
                    //Bandaid before proper solution to handle screen orientation change
                }
            }
        }
    }
}
