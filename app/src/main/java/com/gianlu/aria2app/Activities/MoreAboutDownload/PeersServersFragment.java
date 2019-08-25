package com.gianlu.aria2app.Activities.MoreAboutDownload;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.aria2app.NetIO.Updater.UpdaterFragment;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Tutorial.Discovery;
import com.gianlu.aria2app.Tutorial.PeersServersTutorial;
import com.gianlu.commonutils.BottomSheet.BaseModalBottomSheet;
import com.gianlu.commonutils.CasualViews.RecyclerMessageView;
import com.gianlu.commonutils.SuppressingLinearLayoutManager;
import com.gianlu.commonutils.Tutorial.BaseTutorial;
import com.gianlu.commonutils.Tutorial.TutorialManager;

public abstract class PeersServersFragment<A extends RecyclerView.Adapter<?>, S extends BaseModalBottomSheet<?, ?>, P> extends UpdaterFragment<P> implements TutorialManager.Listener, OnBackPressed {
    protected TopCountriesView topDownloadCountries;
    protected TopCountriesView topUploadCountries;
    protected RecyclerMessageView rmv;
    protected TutorialManager tutorialManager;
    protected S sheet;
    protected A adapter;

    protected abstract boolean showUpload();

    protected abstract A getAdapter(@NonNull Context context);

    @Override
    public final boolean canGoBack(int code) {
        if (code == CODE_CLOSE_SHEET) {
            if (sheet != null) {
                sheet.dismiss();
                sheet = null;
                dismissDialog();
            }
            return true;
        }

        if (hasVisibleDialog()) {
            dismissDialog();
            sheet = null;
            return false;
        } else {
            return true;
        }
    }

    @Override
    public final boolean onCouldntLoad(@NonNull Exception ex) {
        boolean a = onUpdateException(ex);
        if (!a) onCouldntLoadChecked(ex);
        return a;
    }

    protected abstract void onCouldntLoadChecked(@NonNull Exception ex);

    @Nullable
    @Override
    public final View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getContext() == null) return null;

        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.fragment_peers_and_servers, container, false);
        topDownloadCountries = layout.findViewById(R.id.peersServersFragment_topDownloadCountries);
        topUploadCountries = layout.findViewById(R.id.peersServersFragment_topUploadCountries);
        rmv = layout.findViewById(R.id.peersServersFragment_rmv);
        rmv.setLayoutManager(new SuppressingLinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        adapter = getAdapter(getContext());
        rmv.loadListData(adapter);
        rmv.startLoading();

        rmv.enableSwipeRefresh(() -> refresh(() -> {
            adapter = getAdapter(getContext());
            rmv.loadListData(adapter);
            rmv.startLoading();
        }), R.color.colorAccent, R.color.colorMetalink, R.color.colorTorrent);

        layout.findViewById(R.id.peersServersFragment_topUploadCountriesContainer).setVisibility(showUpload() ? View.VISIBLE : View.GONE);

        tutorialManager = new TutorialManager(this, Discovery.PEERS_SERVERS);

        return layout;
    }

    @Override
    public final boolean canShow(@NonNull BaseTutorial tutorial) {
        return tutorial instanceof PeersServersTutorial && ((PeersServersTutorial) tutorial).canShow(this, adapter);
    }
}
