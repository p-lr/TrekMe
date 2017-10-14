package com.peterlaurence.trekadvisor.core.download;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

import com.peterlaurence.trekadvisor.menu.maplist.dialogs.MapDownloadDialog;

/**
 * This object is given to the {@link DownloadService}, to process back the progression of the
 * download.
 *
 * @author peterLaurence on 08/10/17.
 */
public class DownloadReceiver extends ResultReceiver {
    private MapDownloadDialog mMapDownloadDialog;

    public DownloadReceiver(Handler handler, MapDownloadDialog mapDownloadDialog) {
        super(handler);

        mMapDownloadDialog = mapDownloadDialog;
    }

    /**
     * If the activity is re-created while this receiver gets progression from the service, the
     * {@link MapDownloadDialog} is destroyed and another instance is created. <br>
     * This method is then used to pass the new instance of {@link MapDownloadDialog}.
     */
    public void setMapDownloadDialog(MapDownloadDialog mapDownloadDialog) {
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
                    e.printStackTrace();
                }
            }
        }
    }
}
