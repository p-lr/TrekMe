package com.peterlaurence.trekadvisor.core.download;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.annotation.Nullable;

import com.peterlaurence.trekadvisor.core.TrekAdvisorContext;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * This {@link IntentService} expects 3 parameters from the {@link Intent} :
 * <ul>
 * <li>The URL of the file to download</li>
 * <li>The name of the file to write</li>
 * <li>The {@link ResultReceiver} object</li>
 * </ul>
 * For instance, the file is written directly in {@link TrekAdvisorContext#DEFAULT_APP_DIR}. <br>
 * This typically used to download a map archive.
 *
 * @author peterLaurence on 07/10/17.
 */
public class DownloadService extends IntentService {
    public static volatile boolean isRunning;
    public static final String URL_PARAM = "url";
    public static final String RECEIVER_PARAM = "receiver";
    public static final String FILE_NAME = "file_name";
    public static final String PROGRESS_SIG = "progress";
    public static final int UPDATE_PROGRESS = 8344;


    public DownloadService() {
        super(DownloadService.class.getName());
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        isRunning = true;
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String urlToDownload = intent.getStringExtra(URL_PARAM);
        ResultReceiver receiver = intent.getParcelableExtra(RECEIVER_PARAM);
        String fileName = intent.getStringExtra(FILE_NAME);

        int percentProgress = 0;
        try {
            URL url = new URL(urlToDownload);
            URLConnection connection = url.openConnection();
            connection.connect();
            /* This will be useful so that you can show a typical 0-100% progress bar */
            int fileLength = connection.getContentLength();

            /* Download the file */
            InputStream input = new BufferedInputStream(connection.getInputStream());
            OutputStream output = new FileOutputStream(new File(TrekAdvisorContext.DEFAULT_APP_DIR, fileName));

            byte data[] = new byte[1024];
            long total = 0;
            int count;
            while ((count = input.read(data)) != -1) {
                total += count;
                /* Publishing the progress... */
                Bundle resultData = new Bundle();
                percentProgress = (int) (total * 100 / fileLength);
                resultData.putInt(PROGRESS_SIG, percentProgress);
                receiver.send(UPDATE_PROGRESS, resultData);
                output.write(data, 0, count);
            }

            output.flush();
            output.close();
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (percentProgress != 100) {
            //TODO : alert user, send an error signal to the receiver.
        }
        stopSelf();
        isRunning = false;
    }
}
