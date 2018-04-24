package com.gianlu.aria2app.Activities.MoreAboutDownload.Servers;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.gianlu.aria2app.Activities.MoreAboutDownload.PeersServersFragment;
import com.gianlu.aria2app.Adapters.ServersAdapter;
import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.Aria2.Download;
import com.gianlu.aria2app.NetIO.Aria2.DownloadWithHelper;
import com.gianlu.aria2app.NetIO.Aria2.Server;
import com.gianlu.aria2app.NetIO.Aria2.Servers;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.TutorialManager;

public class ServersFragment extends PeersServersFragment<ServersAdapter, ServerBottomSheet> implements ServersAdapter.IAdapter {
    private boolean isShowingHint = false;

    public static ServersFragment getInstance(Context context, Download download) {
        ServersFragment fragment = new ServersFragment();
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.servers));
        args.putSerializable("download", download);
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
    public void onUpdateUi(@NonNull final DownloadWithHelper download) {
        download.servers(new AbstractClient.OnResult<SparseArray<Servers>>() {
            @Override
            public void onResult(@NonNull SparseArray<Servers> servers) {
                recyclerViewLayout.showList();
                if (download.get().last().files != null)
                    topDownloadCountries.setServers(servers, download.get().last().files);
                if (adapter != null && download.get().last().files != null)
                    adapter.notifyItemsChanged(servers, download.get().last().files);
                if (sheet != null && sheet.isExpanded()) sheet.update(servers);
            }

            @Override
            public void onException(Exception ex, boolean shouldForce) {
                ex.printStackTrace(); // FIXME
            }
        });
    }

    @Override
    public void onLoad(@NonNull DownloadWithHelper download) {
        // TODO
    }
}

