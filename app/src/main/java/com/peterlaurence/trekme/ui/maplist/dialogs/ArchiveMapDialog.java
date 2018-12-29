package com.peterlaurence.trekme.ui.maplist.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;

import com.peterlaurence.trekme.R;
import com.peterlaurence.trekme.core.map.Map;

import org.greenrobot.eventbus.EventBus;

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
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mMapId = getArguments().getInt(MAP_ID);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(mTitle)
                .setMessage(R.string.archive_dialog_description)
                .setPositiveButton(R.string.ok_dialog,
                        (dialog, whichButton) -> EventBus.getDefault().post(new SaveMapEvent(mMapId)))
                .setNegativeButton(R.string.cancel_dialog_string,
                        (dialog, whichButton) -> dismiss()
                );

        return builder.create();
    }

    /**
     * The event that will be emitted when the user requests a {@link Map} to be saved. <br>
     * This is processed inside the {@link com.peterlaurence.trekme.ui.maplist.MapListFragment}.
     */
    public static class SaveMapEvent {
        public int mapId;

        public SaveMapEvent(int mapId) {
            this.mapId = mapId;
        }
    }
}
