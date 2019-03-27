package com.gianlu.aria2app.Activities.MoreAboutDownload.Peers;

import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.gianlu.aria2app.Activities.MoreAboutDownload.PeersServersFragment;
import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;
import com.gianlu.aria2app.NetIO.Aria2.AriaException;
import com.gianlu.aria2app.NetIO.Aria2.DownloadWithUpdate;
import com.gianlu.aria2app.NetIO.Aria2.Peer;
import com.gianlu.aria2app.NetIO.Aria2.Peers;
import com.gianlu.aria2app.NetIO.AriaRequests;
import com.gianlu.aria2app.NetIO.Updater.PayloadProvider;
import com.gianlu.aria2app.NetIO.Updater.Wants;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Tutorial.PeersServersTutorial;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Tutorial.BaseTutorial;

import androidx.annotation.NonNull;

public class PeersFragment extends PeersServersFragment<PeersAdapter, PeerSheet, Peers> implements PeersAdapter.Listener {
    private int numPieces = -1;

    @NonNull
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
    public boolean onUpdateException(@NonNull Exception ex) {
        if (ex instanceof AriaException && ((AriaException) ex).isNoPeers()) {
            onItemCountUpdated(0);
            return true;
        }

        return false;
    }

    @Override
    public void onPeerSelected(@NonNull Peer peer) {
        if (numPieces != -1) {
            sheet = PeerSheet.get();
            sheet.show(getActivity(), new PeerWithPieces(peer, numPieces));
        }
    }

    @Override
    public void onItemCountUpdated(int count) {
        if (count == 0) {
            recyclerViewLayout.showInfo(R.string.noPeers);
            topDownloadCountries.clear();
            topUploadCountries.clear();
            if (sheet != null) {
                sheet.dismiss();
                sheet = null;
                dismissDialog();
            }
        } else {
            recyclerViewLayout.showList();
            tutorialManager.tryShowingTutorials(getActivity());
        }
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
        if (numPieces != -1) {
            recyclerViewLayout.showList();
            topDownloadCountries.setPeers(payload, true);
            topUploadCountries.setPeers(payload, false);
            if (adapter != null) adapter.itemsChanged(payload);
            if (sheet != null) sheet.update(payload);
        }
    }

    @Override
    public void onCouldntLoadChecked(@NonNull Exception ex) {
        recyclerViewLayout.showError(R.string.failedLoading_reason, ex.getMessage());
        Logging.log(ex);
    }

    @Override
    protected void onLoadUi(@NonNull Peers payload) {
        Bundle args = getArguments();
        if (args == null || !isAdded()) return;

        try {
            Aria2Helper.instantiate(requireContext()).request(AriaRequests.tellStatus(args.getString("gid")), new AbstractClient.OnResult<DownloadWithUpdate>() {
                @Override
                public void onResult(@NonNull DownloadWithUpdate result) {
                    numPieces = result.bigUpdate().numPieces;
                }

                @Override
                public void onException(@NonNull Exception ex) {
                    onCouldntLoadChecked(ex);
                }
            });
        } catch (Aria2Helper.InitializingException ex) {
            onCouldntLoadChecked(ex);
        }
    }

    @Override
    public boolean buildSequence(@NonNull BaseTutorial tutorial) {
        return tutorial instanceof PeersServersTutorial && ((PeersServersTutorial) tutorial).buildForPeers(recyclerViewLayout.getList());
    }
}
