package com.gianlu.aria2app.Activities.MoreAboutDownload.Servers;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.gianlu.aria2app.Activities.MoreAboutDownload.PeersServersFragment;
import com.gianlu.aria2app.Adapters.ServersAdapter;
import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;
import com.gianlu.aria2app.NetIO.Aria2.AriaException;
import com.gianlu.aria2app.NetIO.Aria2.Server;
import com.gianlu.aria2app.NetIO.Aria2.SparseServersWithFiles;
import com.gianlu.aria2app.NetIO.Updater.PayloadProvider;
import com.gianlu.aria2app.NetIO.Updater.Wants;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.TutorialManager;
import com.gianlu.commonutils.Logging;

public class ServersFragment extends PeersServersFragment<ServersAdapter, ServerSheet, SparseServersWithFiles> implements ServersAdapter.Listener {
    private boolean isShowingHint = false;

    public static ServersFragment getInstance(Context context, String gid) {
        ServersFragment fragment = new ServersFragment();
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.servers));
        args.putString("gid", gid);
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
    public boolean onUpdateException(@NonNull Exception ex) {
        if (ex instanceof AriaException && ((AriaException) ex).isNoServers()) {
            onItemCountUpdated(0);
            return true;
        }

        return false;
    }

    @Override
    public void onServerSelected(@NonNull Server server) {
        sheet = ServerSheet.get();
        sheet.show(getActivity(), server);
    }

    @Override
    public void onItemCountUpdated(int count) {
        if (count == 0) {
            recyclerViewLayout.showMessage(R.string.noServers, false);
            topDownloadCountries.clear();
            if (sheet != null) {
                sheet.dismiss();
                sheet = null;
                dismissDialog();
            }
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
    public void onUpdateUi(@NonNull final SparseServersWithFiles payload) {
        recyclerViewLayout.showList();

        if (payload.files != null)
            topDownloadCountries.setServers(payload.servers, payload.files);

        if (adapter != null && payload.files != null)
            adapter.notifyItemsChanged(payload.servers, payload.files);

        if (sheet != null) sheet.update(payload.servers);
    }

    @Override
    public void onCouldntLoadChecked(@NonNull Exception ex) {
        recyclerViewLayout.showMessage(getString(R.string.failedLoading_reason, ex.getMessage()), true);
        Logging.log(ex);
    }

    @NonNull
    @Override
    protected Wants<SparseServersWithFiles> wants(@NonNull Bundle args) {
        return Wants.serversAndFiles(args.getString("gid"));
    }

    @NonNull
    @Override
    protected PayloadProvider<SparseServersWithFiles> requireProvider(@NonNull Context context, @NonNull Bundle args) throws Aria2Helper.InitializingException {
        return new ServersProvider(context, args.getString("gid"));
    }

    @Override
    public void onLoadUi(@NonNull SparseServersWithFiles payload) {
    }
}

