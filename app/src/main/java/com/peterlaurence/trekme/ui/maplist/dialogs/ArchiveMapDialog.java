package com.peterlaurence.trekme.ui.maplist.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.peterlaurence.trekme.R;
import com.peterlaurence.trekme.core.map.Map;
import com.peterlaurence.trekme.viewmodel.maplist.MapListViewModel;


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
    private MapListViewModel viewModel;

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
        viewModel = new ViewModelProvider(requireActivity()).get(MapListViewModel.class);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle(mTitle)
                .setMessage(R.string.archive_dialog_description)
                .setPositiveButton(R.string.ok_dialog,
                        (dialog, whichButton) -> {
                            if (viewModel != null) {
                                try {
                                    viewModel.startZipTask(mMapId);
                                } catch (Throwable t) {
                                    System.out.println("got u");
                                }

                            }
                        })
                .setNegativeButton(R.string.cancel_dialog_string,
                        (dialog, whichButton) -> dismiss()
                );

        return builder.create();
    }
}
