package com.gianlu.aria2app.Activities.MoreAboutDownload;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;

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
import com.gianlu.commonutils.Logging;

import java.util.List;

public class ServersFragment extends PeersServersFragment<ServersAdapter, ServerBottomSheet> implements UpdateUI.IUI, ServersAdapter.IAdapter {
    private boolean isShowingHint = false;

    public static ServersFragment getInstance(Context context, Download download) {
        ServersFragment fragment = new ServersFragment();
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.servers));
        args.putString("gid", download.gid);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected boolean showUpload() {
        return false;
    }

    @Override
    protected ServersAdapter getAdapter(@NonNull Context context) {
        return new ServersAdapter(getContext(), this);
    }

    @Override
    protected ServerBottomSheet getSheet(@NonNull CoordinatorLayout layout) {
        return new ServerBottomSheet(layout);
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
        topDownloadCountries.setServers(servers, files);
        if (adapter != null) adapter.notifyItemsChanged(servers, files);
        if (sheet != null && sheet.isExpanded()) sheet.update(servers);
    }

    @Override
    public void onNoServers(String reason) {
        recyclerViewLayout.showMessage(reason, false);
        topDownloadCountries.clear();
        if (sheet != null) sheet.collapse();
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

