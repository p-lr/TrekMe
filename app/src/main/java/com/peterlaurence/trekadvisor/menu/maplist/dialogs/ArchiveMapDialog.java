package com.peterlaurence.trekadvisor.menu.maplist.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import com.peterlaurence.trekadvisor.MainActivity;
import com.peterlaurence.trekadvisor.R;
import com.peterlaurence.trekadvisor.core.map.Map;
import com.peterlaurence.trekadvisor.core.map.maploader.MapLoader;
import com.peterlaurence.trekadvisor.util.ZipTask;

/**
 * This dialog is shown when the user chose to save a {@link Map}. <br>
 * A message describes what this process does and asks for confirmation, before displaying a
 * {@link android.widget.ProgressBar}. <p>
 * When the saving is done, the location of the archive is shown.
 *
 * @author peterLaurence on 14/08/17.
 */
public class ArchiveMapDialog extends DialogFragment {
    private static final String MAP_ID = "map_id";

    private String mTitle;
    private int mMapId;
    private ArchiveMapListener mListener;

    public static ArchiveMapDialog newInstance(int mapId) {
        ArchiveMapDialog frag = new ArchiveMapDialog();
        Bundle args = new Bundle();
        args.putInt(MAP_ID, mapId);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mTitle = context.getString(R.string.archive_dialog_title);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
            mListener = ((MainActivity) getActivity()).getArchiveMapListener();
        } catch (ClassCastException e) {
            throw new ClassCastException(getContext()
                    + " must implement provide a ArchiveMapListener instance");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mMapId = getArguments().getInt(MAP_ID);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(mTitle)
                .setMessage(R.string.archive_dialog_description)
                .setPositiveButton(R.string.ok_dialog,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        mListener.doArchiveMap(mMapId);
                    }
                })
                .setNegativeButton(R.string.cancel_dialog_string,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dismiss();
                            }
                        }
                );

        return builder.create();
    }

    public interface ArchiveMapListener {
        void doArchiveMap(int mapId);
    }
}
