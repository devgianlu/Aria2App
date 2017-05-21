package com.gianlu.aria2app.Activities.MoreAboutDownload;

import android.content.Context;
import android.os.Bundle;
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
import android.widget.ProgressBar;

import com.gianlu.aria2app.Activities.MoreAboutDownload.Peers.PeerBottomSheet;
import com.gianlu.aria2app.Activities.MoreAboutDownload.Peers.UpdateUI;
import com.gianlu.aria2app.Adapters.PeersAdapter;
import com.gianlu.aria2app.NetIO.BaseUpdater;
import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.NetIO.JTA2.JTA2InitializingException;
import com.gianlu.aria2app.NetIO.JTA2.Peer;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.MessageLayout;

import java.util.ArrayList;
import java.util.List;

public class PeersFragment extends BackPressedFragment implements UpdateUI.IUI, PeersAdapter.IAdapter {
    private UpdateUI updater;
    private CoordinatorLayout layout;
    private PeersAdapter adapter;
    private RecyclerView list;
    private ProgressBar loading;
    private PeerBottomSheet sheet;

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
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        layout = (CoordinatorLayout) inflater.inflate(R.layout.peers_fragment, container, false);
        final SwipeRefreshLayout swipeRefresh = (SwipeRefreshLayout) layout.findViewById(R.id.peersFragment_swipeRefresh);
        swipeRefresh.setColorSchemeResources(R.color.colorAccent, R.color.colorMetalink, R.color.colorTorrent);
        loading = (ProgressBar) layout.findViewById(R.id.peersFragment_loading);
        list = (RecyclerView) layout.findViewById(R.id.peersFragment_list);
        list.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        list.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
        adapter = new PeersAdapter(getContext(), new ArrayList<Peer>(), this);
        list.setAdapter(adapter);

        sheet = new PeerBottomSheet(layout);

        final String gid = getArguments().getString("gid");
        if (gid == null) {
            MessageLayout.show(layout, R.string.failedLoading, R.drawable.ic_error_black_48dp);
            loading.setVisibility(View.GONE);
            return layout;
        }

        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updater.stopThread(new BaseUpdater.IThread() {
                    @Override
                    public void onStopped() {
                        try {
                            adapter = new PeersAdapter(getContext(), new ArrayList<Peer>(), PeersFragment.this);
                            list.setAdapter(adapter);

                            updater = new UpdateUI(getContext(), gid, PeersFragment.this);
                            updater.start();
                        } catch (JTA2InitializingException ex) {
                            CommonUtils.UIToast(getActivity(), Utils.ToastMessages.FAILED_REFRESHING, ex);
                        } finally {
                            if (isAdded()) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        swipeRefresh.setRefreshing(false);
                                    }
                                });
                            }
                        }
                    }
                });
            }
        });

        try {
            updater = new UpdateUI(getContext(), gid, this);
            updater.start();
        } catch (JTA2InitializingException ex) {
            MessageLayout.show(layout, R.string.failedLoading, R.drawable.ic_error_black_48dp);
            loading.setVisibility(View.GONE);
            Logging.logMe(getContext(), ex);
            return layout;
        }

        return layout;
    }

    @Override
    public boolean canGoBack() {
        if (sheet.shouldUpdate()) {
            sheet.collapse();
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onBackPressed() {
        if (updater != null) updater.stopThread(null);
    }

    @Override
    public void onUpdateAdapter(List<Peer> peers) {
        if (peers.isEmpty()) return;
        MessageLayout.hide(layout);
        loading.setVisibility(View.GONE);
        list.setVisibility(View.VISIBLE);
        if (adapter != null) adapter.notifyItemsChanged(peers);
        if (sheet != null && sheet.shouldUpdate()) sheet.update(peers);
    }

    @Override
    public void onNoPeers(String message) {
        MessageLayout.show(layout, message, R.drawable.ic_info_outline_black_48dp);
        loading.setVisibility(View.GONE);
        list.setVisibility(View.GONE);
        if (sheet != null) sheet.collapse();
    }

    @Override
    public void onPeerSelected(Peer peer) {
        sheet.expand(peer);
    }

    @Override
    public void onItemCountUpdated(int count) {
        if (count == 0) {
            MessageLayout.show(layout, R.string.noPeers, R.drawable.ic_info_outline_black_48dp);
            list.setVisibility(View.GONE);
            loading.setVisibility(View.GONE);
            if (sheet != null) sheet.collapse();
        } else {
            MessageLayout.hide(layout);
            list.setVisibility(View.VISIBLE);
        }
    }
}
