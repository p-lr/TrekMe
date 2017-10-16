package com.peterlaurence.trekadvisor.core.download;

import android.util.SparseArray;

import com.peterlaurence.trekadvisor.menu.maplist.dialogs.events.UrlDownloadEvent;
import com.peterlaurence.trekadvisor.menu.maplist.dialogs.events.UrlDownloadFinishedEvent;

import org.greenrobot.eventbus.EventBus;

import java.io.File;


/**
 * A helper class to manage {@link DownloadTask}s, which can be cancelled. <br>
 * It is made so that :
 * <ul>
 * <li>A given url download can be cancelled at any time, whith {@link #stopUrlDownload(String)}</li>
 * <li>When a {@link DownloadTask} finishes either normally or not, {@link #removeTask(int)} is called upon
 * {@link DownloadTask.UrlDownloadListener#onDownloadFinished(boolean)} callback. <br>
 * This guaranties to keep the internal dataset clean.</li>
 * </ul>
 *
 * @author peterLaurence on 16/10/17.
 */
public final class UrlDownloadTaskExecutor {
    private static SparseArray<DownloadTask> mDownloadTaskArray = new SparseArray<>();

    public static int startUrlDownload(final String url, File outputFile) {
        final int threadId = getThreadId(url);
        DownloadTask downloadTask = new DownloadTask(url, outputFile, new DownloadTask.UrlDownloadListener() {
            @Override
            public void onDownloadProgress(int percent) {
                EventBus.getDefault().post(new UrlDownloadEvent(percent));
            }

            @Override
            public void onDownloadFinished(boolean success) {
                removeTask(threadId);
                EventBus.getDefault().post(new UrlDownloadFinishedEvent(success));
            }
        });


        mDownloadTaskArray.append(threadId, downloadTask);
        downloadTask.start();

        return threadId;
    }


    public static void stopUrlDownload(String url) {
        int threadId = getThreadId(url);
        DownloadTask downloadTask = mDownloadTaskArray.get(threadId);
        if (downloadTask != null) {
            downloadTask.cancel();
            mDownloadTaskArray.remove(threadId);
        }
    }

    private static void removeTask(int threadId) {
        DownloadTask downloadTask = mDownloadTaskArray.get(threadId);
        if (downloadTask != null) {
            mDownloadTaskArray.remove(threadId);
        }
    }

    private static int getThreadId(String url) {
        return url.hashCode();
    }
}
