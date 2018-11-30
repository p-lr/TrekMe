package com.peterlaurence.trekme.ui.mapimport;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.peterlaurence.trekme.R;
import com.peterlaurence.trekme.core.events.MapArchiveListUpdateEvent;
import com.peterlaurence.trekme.core.map.maploader.MapLoader;
import com.peterlaurence.trekme.ui.events.DrawerClosedEvent;
import com.peterlaurence.trekme.ui.events.MapImportedEvent;
import com.peterlaurence.trekme.ui.events.RequestImportMapEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * A {@link Fragment} subclass that displays the list of maps archives available for import.
 *
 * @author peterLaurence on 08/06/16.
 */
public class MapImportFragment extends Fragment {

    private static String CREATE_FROM_SCREEN_ROTATE = "create";
    private RecyclerView mRecyclerView;
    private MapArchiveAdapter mMapArchiveAdapter;
    private OnMapArchiveFragmentInteractionListener mListener;
    private View mView;
    private boolean mCreateAfterScreenRotation = false;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof OnMapArchiveFragmentInteractionListener) {
            mListener = (OnMapArchiveFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnMapArchiveFragmentInteractionListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_map_import, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mView = view;

        /* When this fragment is created from a screen rotating, don't wait the drawer layout to
         * close to re-generate the map list.
         */
        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean(CREATE_FROM_SCREEN_ROTATE)) {
                mCreateAfterScreenRotation = true;
            }
        }

        mRecyclerView = view.findViewById(R.id.recyclerview_map_import);
        mRecyclerView.setHasFixedSize(false);

        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(llm);

        mMapArchiveAdapter = new MapArchiveAdapter();
        mRecyclerView.setAdapter(mMapArchiveAdapter);

        /* Item decoration : divider */
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(view.getContext(),
                DividerItemDecoration.VERTICAL);
        Drawable divider = view.getContext().getDrawable(R.drawable.divider);
        if (divider != null) {
            dividerItemDecoration.setDrawable(divider);
        }
        mRecyclerView.addItemDecoration(dividerItemDecoration);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        if (mMapArchiveAdapter != null) {
            mMapArchiveAdapter.subscribeEventBus();
        }
        /* Request archive generation after EventBus registration to be 100% sure to get notified
         * if this fragment is still registered when the job finishes.
         */
        if (mCreateAfterScreenRotation) {
            generateMapList();
        }
    }

    private void generateMapList() {
        MapLoader.getInstance().generateMapArchives();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMapImported(MapImportedEvent event) {
        Snackbar snackbar = Snackbar.make(mView, R.string.snack_msg_show_map_list, Snackbar.LENGTH_LONG);
        snackbar.setAction(R.string.ok_dialog, v -> mListener.onMapArchiveFragmentInteraction());
        snackbar.show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRequestImportMapEvent(RequestImportMapEvent event) {
        String confirmImport = getContext().getString(R.string.confirm_import);
        Snackbar snackbar = Snackbar.make(getView(), confirmImport, Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    /**
     * When this fragment is created for the first time, we wait the {@link android.support.v4.widget.DrawerLayout}
     * to close before generating the map list (to avoid a stutter). <br>
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDrawerClosed(DrawerClosedEvent event) {
        generateMapList();
    }

    /**
     * A {@link MapArchiveListUpdateEvent} is emitted from the {@link MapLoader} when the list of
     * map archives is updated.
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMapArchiveListUpdate(MapArchiveListUpdateEvent event) {
        hideProgressBar();
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        if (mMapArchiveAdapter != null) {
            mMapArchiveAdapter.unSubscribeEventBus();
        }
        super.onStop();

        if (mRecyclerView == null) return;
        for (int i = 0; i < mRecyclerView.getAdapter().getItemCount(); i++) {
            MapArchiveViewHolder holder = (MapArchiveViewHolder) mRecyclerView.findViewHolderForAdapterPosition(i);
            if (holder != null) {
                holder.unSubscribe();
            }
        }
    }

    private void hideProgressBar() {
        mView.findViewById(R.id.progress_import_frgmt).setVisibility(View.GONE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(CREATE_FROM_SCREEN_ROTATE, true);
    }

    public interface OnMapArchiveFragmentInteractionListener {
        void onMapArchiveFragmentInteraction();
    }
}
