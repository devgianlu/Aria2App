package com.gianlu.aria2app.Activities.MoreAboutDownload;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.gianlu.aria2app.Activities.MoreAboutDownload.Peers.PeerBottomSheet;
import com.gianlu.aria2app.Activities.MoreAboutDownload.Peers.UpdateUI;
import com.gianlu.aria2app.Adapters.PeersAdapter;
import com.gianlu.aria2app.NetIO.BaseUpdater;
import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.NetIO.JTA2.Peer;
import com.gianlu.aria2app.NetIO.OnRefresh;
import com.gianlu.aria2app.NetIO.UpdaterFragment;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.TutorialManager;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.RecyclerViewLayout;

import java.util.ArrayList;
import java.util.List;

public class PeersFragment extends UpdaterFragment implements UpdateUI.IUI, PeersAdapter.IAdapter, OnBackPressed {
    private PeersAdapter adapter;
    private PeerBottomSheet sheet;
    private boolean isShowingHint = false;
    private RecyclerViewLayout recyclerViewLayout;

    public static PeersFragment getInstance(Context context, Download download) {
        PeersFragment fragment = new PeersFragment();
        fragment.setHasOptionsMenu(true);
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.peers));
        args.putString("gid", download.gid);
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        CoordinatorLayout layout = (CoordinatorLayout) inflater.inflate(R.layout.fragment_peers_and_servers, container, false);
        if (getContext() == null) return layout;
        recyclerViewLayout = layout.findViewById(R.id.peersServersFragment_recyclerViewLayout);
        recyclerViewLayout.enableSwipeRefresh(R.color.colorAccent, R.color.colorMetalink, R.color.colorTorrent);
        recyclerViewLayout.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        recyclerViewLayout.getList().addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
        adapter = new PeersAdapter(getContext(), new ArrayList<Peer>(), this);
        recyclerViewLayout.loadListData(adapter);
        recyclerViewLayout.startLoading();

        sheet = new PeerBottomSheet(layout);
        recyclerViewLayout.setRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh(new OnRefresh() {
                    @Override
                    public void refreshed() {
                        adapter = new PeersAdapter(getContext(), new ArrayList<Peer>(), PeersFragment.this);
                        recyclerViewLayout.loadListData(adapter);
                        recyclerViewLayout.startLoading();
                    }
                });
            }
        });

        return layout;
    }

    @Override
    public boolean canGoBack(int code) {
        if (code == CODE_CLOSE_SHEET) {
            if (sheet != null) sheet.collapse();
            return true;
        }

        if (sheet != null && sheet.isExpanded()) {
            sheet.collapse();
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onBackPressed() {
        stopUpdater();
    }

    @Override
    public void onUpdateAdapter(List<Peer> peers) {
        recyclerViewLayout.showList();
        if (adapter != null) adapter.notifyItemsChanged(peers);
        if (sheet != null && sheet.isExpanded()) sheet.update(peers);
    }

    @Override
    public void onNoPeers(String reason) {
        recyclerViewLayout.showMessage(reason, false);
        if (sheet != null) sheet.collapse();
    }

    @Override
    public void onPeerSelected(Peer peer) {
        sheet.expand(peer);
    }

    @Override
    public void onItemCountUpdated(int count) {
        if (count == 0) {
            recyclerViewLayout.showMessage(R.string.noPeers, false);
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

    @Nullable
    @Override
    protected BaseUpdater createUpdater(@NonNull Bundle args) {
        String gid = args.getString("gid");

        try {
            return new UpdateUI(getContext(), gid, this);
        } catch (JTA2.InitializingException ex) {
            recyclerViewLayout.showMessage(R.string.failedLoading, true);
            Logging.logMe(ex);
            return null;
        }
    }
}
