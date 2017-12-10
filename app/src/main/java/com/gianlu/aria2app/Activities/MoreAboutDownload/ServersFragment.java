package com.gianlu.aria2app.Activities.MoreAboutDownload;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.gianlu.aria2app.Activities.MoreAboutDownload.Servers.ServerBottomSheet;
import com.gianlu.aria2app.Activities.MoreAboutDownload.Servers.UpdateUI;
import com.gianlu.aria2app.Adapters.ServersAdapter;
import com.gianlu.aria2app.NetIO.BaseUpdater;
import com.gianlu.aria2app.NetIO.JTA2.AriaFile;
import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.NetIO.JTA2.Server;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.TutorialManager;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.RecyclerViewLayout;
import com.gianlu.commonutils.SuppressingLinearLayoutManager;
import com.gianlu.commonutils.Toaster;

import java.util.List;

public class ServersFragment extends BackPressedFragment implements UpdateUI.IUI, ServersAdapter.IAdapter {
    private UpdateUI updater;
    private RecyclerViewLayout recyclerViewLayout;
    private ServersAdapter adapter;
    private ServerBottomSheet sheet;
    private boolean isShowingHint;

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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        CoordinatorLayout layout = (CoordinatorLayout) inflater.inflate(R.layout.fragment_peers_and_servers, container, false);
        recyclerViewLayout = layout.findViewById(R.id.peersServersFragment_recyclerViewLayout);
        recyclerViewLayout.enableSwipeRefresh(R.color.colorAccent, R.color.colorMetalink, R.color.colorTorrent);
        recyclerViewLayout.setLayoutManager(new SuppressingLinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        adapter = new ServersAdapter(getContext(), this);
        recyclerViewLayout.loadListData(adapter);
        recyclerViewLayout.startLoading();

        sheet = new ServerBottomSheet(layout);

        final String gid = getArguments().getString("gid");
        if (gid == null) {
            recyclerViewLayout.showMessage(R.string.failedLoading, true);
            return layout;
        }

        recyclerViewLayout.setRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updater.stopThread(new BaseUpdater.IThread() {
                    @Override
                    public void onStopped() {
                        try {
                            adapter = new ServersAdapter(getContext(), ServersFragment.this);
                            recyclerViewLayout.loadListData(adapter);
                            recyclerViewLayout.startLoading();

                            updater = new UpdateUI(getContext(), gid, ServersFragment.this);
                            updater.start();
                        } catch (JTA2.InitializingException ex) {
                            Toaster.show(getActivity(), Utils.Messages.FAILED_REFRESHING, ex);
                        }
                    }
                });
            }
        });

        try {
            updater = new UpdateUI(getContext(), gid, this);
            updater.start();
        } catch (JTA2.InitializingException ex) {
            recyclerViewLayout.showMessage(R.string.failedLoading, true);
            Logging.logMe(ex);
            return layout;
        }

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
        if (updater != null) updater.stopThread(null);
    }

    @Override
    public void onServerSelected(Server server) {
        sheet.expand(server);
    }

    @Override
    public void onItemCountUpdated(int count) {
        if (count == 0) {
            recyclerViewLayout.showMessage(R.string.noServers, false);
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

                TapTargetView.showFor(getActivity(), TapTarget.forBounds(rect, getString(R.string.serverDetails), getString(R.string.serverDetails_desc))
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

    @Override
    public void onUpdateAdapter(SparseArray<List<Server>> servers, List<AriaFile> files) {
        if (servers.size() == 0) return;
        recyclerViewLayout.showList();
        if (adapter != null) adapter.notifyItemsChanged(servers, files);
        if (sheet != null && sheet.isExpanded()) sheet.update(servers);
    }

    @Override
    public void onNoServers(String reason) {
        recyclerViewLayout.showMessage(reason, false);
        if (sheet != null) sheet.collapse();
    }
}

