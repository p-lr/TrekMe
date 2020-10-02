package com.peterlaurence.trekme.core.download;

import android.util.SparseArray;

import java.io.File;


/**
 * A helper class to manage {@link DownloadTask}s, which can be cancelled. <br>
 * It is made so that :
 * <ul>
 * <li>A given url download can be cancelled at any time, with {@link #stopUrlDownload(String)}</li>
 * <li>When a {@link DownloadTask} finishes either normally or not, {@link #removeTask(int)} is called upon
 * {@link DownloadTask.UrlDownloadListener#onDownloadFinished(boolean)} callback. <br>
 * This guaranties to keep the internal dataset clean.</li>
 * </ul>
 *
 * @author P.Laurence on 16/10/17.
 */
public final class UrlDownloadTaskExecutor {
    private static SparseArray<DownloadTask> mDownloadTaskArray = new SparseArray<>();

    /**
     * Launch a {@link DownloadTask}. Two {@link DownloadTask.UrlDownloadListener} are chained so that
     * this component is able to react on {@link DownloadTask.UrlDownloadListener#onDownloadFinished(boolean)}
     * while still calling the original listener.
     */
    public static int startUrlDownload(final String url, File outputFile, final DownloadTask.UrlDownloadListener listener) {
        final int threadId = getThreadId(url);
        DownloadTask downloadTask = new DownloadTask(url, outputFile, new DownloadTask.UrlDownloadListener() {
            @Override
            public void onDownloadProgress(int percent) {
                listener.onDownloadProgress(percent);
            }

            @Override
            public void onDownloadFinished(boolean success) {
                removeTask(threadId);
                listener.onDownloadFinished(success);
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
