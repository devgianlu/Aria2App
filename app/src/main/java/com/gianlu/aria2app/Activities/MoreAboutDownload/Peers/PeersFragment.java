package com.gianlu.aria2app.Activities.MoreAboutDownload.Peers;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.gianlu.aria2app.Activities.MoreAboutDownload.PeersServersFragment;
import com.gianlu.aria2app.Adapters.PeersAdapter;
import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;
import com.gianlu.aria2app.NetIO.Aria2.AriaException;
import com.gianlu.aria2app.NetIO.Aria2.Peer;
import com.gianlu.aria2app.NetIO.Aria2.Peers;
import com.gianlu.aria2app.NetIO.Updater.PayloadProvider;
import com.gianlu.aria2app.NetIO.Updater.Wants;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.TutorialManager;
import com.gianlu.commonutils.Logging;

public class PeersFragment extends PeersServersFragment<PeersAdapter, PeerBottomSheet, Peers> implements PeersAdapter.IAdapter {
    private boolean isShowingHint = false;

    public static PeersFragment getInstance(Context context, String gid) {
        PeersFragment fragment = new PeersFragment();
        fragment.setHasOptionsMenu(true);
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.peers));
        args.putString("gid", gid);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.peers_fragment, menu);
        inflater.inflate(R.menu.peers_fragment_sorting, menu.findItem(R.id.peersFragment_sorting).getSubMenu());
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.peersFragment_sorting).getSubMenu().setGroupCheckable(0, true, true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (adapter == null) return false;

        item.setChecked(true);
        switch (item.getItemId()) {
            case R.id.peersFragmentSort_downloadSpeed:
                adapter.sort(PeersAdapter.SortBy.DOWNLOAD_SPEED);
                break;
            case R.id.peersFragmentSort_uploadSpeed:
                adapter.sort(PeersAdapter.SortBy.UPLOAD_SPEED);
                break;
        }

        return true;
    }

    @Override
    protected boolean showUpload() {
        return true;
    }

    @Override
    protected PeersAdapter getAdapter(@NonNull Context context) {
        return new PeersAdapter(getContext(), this);
    }

    @Override
    protected PeerBottomSheet getSheet(@NonNull CoordinatorLayout layout) {
        return new PeerBottomSheet(layout);
    }

    @Override
    public boolean onUpdateException(@NonNull Exception ex) {
        if (ex instanceof AriaException && ((AriaException) ex).isNoPeers()) {
            onItemCountUpdated(0);
            return true;
        }

        return false;
    }

    @Override
    public void onPeerSelected(Peer peer) {
        sheet.expand(peer);
    }

    @Override
    public void onItemCountUpdated(int count) {
        if (count == 0) {
            recyclerViewLayout.showMessage(R.string.noPeers, false);
            topDownloadCountries.clear();
            topUploadCountries.clear();
            if (sheet != null) sheet.collapse();
        } else {
            recyclerViewLayout.showList();
        }

        if (isVisible() && !isShowingHint && count >= 1 && TutorialManager.shouldShowHintFor(getContext(), TutorialManager.Discovery.PEERS_SERVERS)) {
            RecyclerView.ViewHolder holder = recyclerViewLayout.getList().findViewHolderForLayoutPosition(0);
            if (holder != null && getActivity() != null) {
                isShowingHint = true;

                recyclerViewLayout.getList().scrollToPosition(0);

                Rect rect = new Rect();
                holder.itemView.getGlobalVisibleRect(rect);
                rect.offset((int) -(holder.itemView.getWidth() * 0.3), 0);

                TapTargetView.showFor(getActivity(), TapTarget.forBounds(rect, getString(R.string.peerDetails), getString(R.string.peerDetails_desc))
                                .tintTarget(false)
                                .transparentTarget(true),
                        new TapTargetView.Listener() {
                            @Override
                            public void onTargetDismissed(TapTargetView view, boolean userInitiated) {
                                TutorialManager.setHintShown(getContext(), TutorialManager.Discovery.PEERS_SERVERS);
                                isShowingHint = false;
                            }
                        });
            }
        }
    }

    @Nullable
    @Override
    public RecyclerView getRecyclerView() {
        return recyclerViewLayout.getList();
    }

    @NonNull
    @Override
    protected Wants<Peers> wants(@NonNull Bundle args) {
        return Wants.peers(args.getString("gid"));
    }

    @NonNull
    @Override
    protected PayloadProvider<Peers> requireProvider(@NonNull Context context, @NonNull Bundle args) throws Aria2Helper.InitializingException {
        return new PeersProvider(context, args.getString("gid"));
    }

    @Override
    public void onUpdateUi(@NonNull Peers payload) {
        recyclerViewLayout.showList();
        topDownloadCountries.setPeers(payload, true);
        topUploadCountries.setPeers(payload, false);
        if (adapter != null) adapter.itemsChanged(payload);
        if (sheet != null && sheet.isExpanded()) sheet.update(payload);
    }

    @Override
    public void onCouldntLoadChecked(@NonNull Exception ex) {
        recyclerViewLayout.showMessage(getString(R.string.failedLoading_reason, ex.getMessage()), true);
        Logging.log(ex);
    }

    @Override
    protected void onLoadUi(@NonNull Peers payload) {
    }
}
