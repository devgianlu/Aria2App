package com.gianlu.aria2app.Activities.MoreAboutDownload.Servers;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.gianlu.aria2app.Activities.MoreAboutDownload.PeersServersFragment;
import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;
import com.gianlu.aria2app.NetIO.Aria2.AriaException;
import com.gianlu.aria2app.NetIO.Aria2.Server;
import com.gianlu.aria2app.NetIO.Aria2.SparseServersWithFiles;
import com.gianlu.aria2app.NetIO.Updater.PayloadProvider;
import com.gianlu.aria2app.NetIO.Updater.Wants;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Tutorial.PeersServersTutorial;
import com.gianlu.commonutils.logging.Logging;
import com.gianlu.commonutils.tutorial.BaseTutorial;

public class ServersFragment extends PeersServersFragment<ServersAdapter, ServerSheet, SparseServersWithFiles> implements ServersAdapter.Listener {

    @NonNull
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
            rmv.showInfo(R.string.noServers);
            topDownloadCountries.clear();
            if (sheet != null && !isDetached()) {
                sheet.dismiss();
                sheet = null;
                dismissDialog();
            }
        } else {
            rmv.showList();
            tutorialManager.tryShowingTutorials(getActivity());
        }
    }

    @Override
    public void onUpdateUi(@NonNull final SparseServersWithFiles payload) {
        rmv.showList();

        if (payload.files != null)
            topDownloadCountries.setServers(payload.servers, payload.files);

        if (adapter != null && payload.files != null)
            adapter.notifyItemsChanged(payload.servers, payload.files);

        if (sheet != null) sheet.update(payload.servers);
    }

    @Override
    public void onCouldntLoadChecked(@NonNull Exception ex) {
        rmv.showError(R.string.failedLoading_reason, ex.getMessage());
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

    @Override
    public boolean buildSequence(@NonNull BaseTutorial tutorial) {
        return tutorial instanceof PeersServersTutorial && ((PeersServersTutorial) tutorial).buildForServers(rmv.list());
    }
}

