package com.peterlaurence.trekadvisor.menu.maplist.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.peterlaurence.trekadvisor.R;
import com.peterlaurence.trekadvisor.core.download.UrlDownloadTaskExecutor;
import com.peterlaurence.trekadvisor.menu.maplist.dialogs.events.UrlDownloadEvent;
import com.peterlaurence.trekadvisor.menu.maplist.dialogs.events.UrlDownloadFinishedEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * A {@link Dialog} that shows the progression of an URL download. <p>
 * It relies on the {@link EventBus} to get {@link UrlDownloadEvent} messages.
 *
 * @author peterLaurence on 07/10/17.
 */
public class UrlDownloadDialog extends DialogFragment {
    private static final String MAP_NAME = "map_name";
    private static final String URL = "url";
    private String mTitle;
    private String mUrl;
    private int mUrlHashCode;
    private ProgressBar mProgressBar;

    public static UrlDownloadDialog newInstance(String mapName, String url) {
        UrlDownloadDialog frag = new UrlDownloadDialog();
        Bundle args = new Bundle();
        args.putString(MAP_NAME, mapName);
        args.putString(URL, url);
        frag.setArguments(args);
        return frag;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onProgressEvent(UrlDownloadEvent event) {
        if (event.urlHash != mUrlHashCode) return;
        mProgressBar.setProgress(event.percentProgress);

        if (event.percentProgress == 100) {
            dismiss();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDownloadFinished(UrlDownloadFinishedEvent event) {
        if (event.urlHash != mUrlHashCode) return;
        if (!event.success) {
            //TODO : alert the user
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mTitle = context.getString(R.string.download_map_dialog_title);
        mUrl = getArguments().getString(URL);
        mUrlHashCode = mUrl.hashCode();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(mTitle);
        View view = getActivity().getLayoutInflater().inflate(R.layout.download_url_dialog, null);

        TextView msg = view.findViewById(R.id.download_url_dialog_msg);
        String mMapName = getArguments().getString(MAP_NAME);
        msg.setText(String.format(getString(R.string.download_map_dialog_msg), mMapName));
        mProgressBar = view.findViewById(R.id.download_url_dialog_progress);

        builder.setView(view);
        builder.setNegativeButton(getString(R.string.cancel_dialog_string), (dialog, which) -> {
            /* Download canceled by user */
            UrlDownloadTaskExecutor.stopUrlDownload(mUrl);
        });
        builder.setCancelable(false);

        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }
}
