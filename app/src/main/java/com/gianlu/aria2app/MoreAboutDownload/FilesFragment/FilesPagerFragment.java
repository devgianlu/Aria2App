package com.gianlu.aria2app.MoreAboutDownload.FilesFragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.gianlu.aria2app.Main.IThread;
import com.gianlu.aria2app.MoreAboutDownload.CommonFragment;
import com.gianlu.aria2app.NetIO.JTA2.File;
import com.gianlu.aria2app.NetIO.JTA2.IFiles;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;


public class FilesPagerFragment extends CommonFragment {
    private UpdateUI updateUI;

    public static FilesPagerFragment newInstance(String title, String gid) {
        FilesPagerFragment fragment = new FilesPagerFragment();

        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("gid", gid);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        UpdateUI.stop(updateUI);

        try {
            JTA2.newInstance(getActivity()).getFiles(getArguments().getString("gid"), new IFiles() {
                @Override
                public void onFiles(final List<File> files) {
                    updateUI = new UpdateUI(getActivity(), getArguments().getString("gid"), new FilesAdapter(getActivity(), getArguments().getString("gid"), Tree.newTree().addElements(files), (LinearLayout) view.findViewById(R.id.filesFragment_tree)));
                    new Thread(updateUI).start();
                }

                @Override
                public void onException(Exception exception) {
                    Utils.UIToast(getActivity(), Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, exception);
                }
            });
        } catch (IOException | NoSuchAlgorithmException ex) {
            Utils.UIToast(getActivity(), Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, ex);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable final Bundle savedInstanceState) {
        final SwipeRefreshLayout rootView = (SwipeRefreshLayout) inflater.inflate(R.layout.files_fragment, container, false);

        rootView.setColorSchemeResources(R.color.colorAccent, R.color.colorMetalink, R.color.colorTorrent);
        rootView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                UpdateUI.stop(updateUI, new IThread() {
                    @Override
                    public void stopped() {
                        onViewCreated(rootView, savedInstanceState);
                        rootView.setRefreshing(false);
                    }
                });
            }
        });

        return rootView;
    }

    @Override
    public void stopUpdater() {
        UpdateUI.stop(updateUI);
    }
}
