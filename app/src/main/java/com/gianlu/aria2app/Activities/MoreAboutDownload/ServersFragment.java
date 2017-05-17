package com.gianlu.aria2app.Activities.MoreAboutDownload;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.gianlu.aria2app.Activities.MoreAboutDownload.Servers.UpdateUI;
import com.gianlu.aria2app.Adapters.ServersAdapter;
import com.gianlu.aria2app.NetIO.BaseUpdater;
import com.gianlu.aria2app.NetIO.JTA2.AFile;
import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.NetIO.JTA2.JTA2InitializingException;
import com.gianlu.aria2app.NetIO.JTA2.Server;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.MessageLayout;

import java.util.List;

public class ServersFragment extends BackPressedFragment implements UpdateUI.IUI, ServersAdapter.IAdapter {
    private UpdateUI updater;
    private CoordinatorLayout layout;
    private ServersAdapter adapter;
    private RecyclerView list;
    private ProgressBar loading;

    public static ServersFragment getInstance(Context context, Download download) {
        ServersFragment fragment = new ServersFragment();
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.servers));
        args.putString("gid", download.gid);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        layout = (CoordinatorLayout) inflater.inflate(R.layout.servers_fragment, container, false);
        final SwipeRefreshLayout swipeRefresh = (SwipeRefreshLayout) layout.findViewById(R.id.serversFragment_swipeRefresh);
        swipeRefresh.setColorSchemeResources(R.color.colorAccent, R.color.colorMetalink, R.color.colorTorrent);
        loading = (ProgressBar) layout.findViewById(R.id.serversFragment_loading);
        list = (RecyclerView) layout.findViewById(R.id.serversFragment_list);
        list.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        adapter = new ServersAdapter(getContext(), this);
        list.setAdapter(adapter);

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
                            adapter = new ServersAdapter(getContext(), ServersFragment.this);
                            list.setAdapter(adapter);

                            updater = new UpdateUI(getContext(), gid, ServersFragment.this);
                            updater.start();
                        } catch (JTA2InitializingException ex) {
                            CommonUtils.UIToast(getActivity(), Utils.ToastMessages.FAILED_REFRESHING, ex, new Runnable() {
                                @Override
                                public void run() {
                                    swipeRefresh.setRefreshing(false);
                                }
                            });
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
        return true;
    }

    @Override
    public void onBackPressed() {
        if (updater != null) updater.stopThread(null);
    }

    @Override
    public void onServerSelected(Server server) {
        // TODO: sheet.expand(server);
    }

    @Override
    public void onItemCountUpdated(int count) {
        if (count == 0) {
            MessageLayout.show(layout, R.string.noPeers, R.drawable.ic_info_outline_black_48dp);
            list.setVisibility(View.GONE);
            // TODO: if (sheet != null) sheet.collapse();
        } else {
            MessageLayout.hide(layout);
            list.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onUpdateAdapter(SparseArray<List<Server>> servers, List<AFile> files) {
        MessageLayout.hide(layout);
        loading.setVisibility(View.GONE);
        list.setVisibility(View.VISIBLE);
        if (adapter != null) adapter.notifyItemsChanged(servers, files);
        // TODO: if (sheet != null && sheet.shouldUpdate()) sheet.update(servers);
    }

    @Override
    public void onNoServers(String reason) {
        MessageLayout.show(layout, reason, R.drawable.ic_info_outline_black_48dp);
        loading.setVisibility(View.GONE);
        list.setVisibility(View.GONE);
        // TODO: if (sheet != null) sheet.collapse();
    }
}

