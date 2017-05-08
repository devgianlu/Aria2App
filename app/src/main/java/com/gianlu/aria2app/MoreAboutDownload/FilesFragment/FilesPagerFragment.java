package com.gianlu.aria2app.MoreAboutDownload.FilesFragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.gianlu.aria2app.Main.IThread;
import com.gianlu.aria2app.MoreAboutDownload.CommonFragment;
import com.gianlu.aria2app.NetIO.JTA2.AFile;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.CommonUtils;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.List;


public class FilesPagerFragment extends CommonFragment {
    private UpdateUI updateUI;
    private FilesAdapter adapter;
    private int gotoIndex = -1;

    public static FilesPagerFragment newInstance(String title, String gid) {
        FilesPagerFragment fragment = new FilesPagerFragment();

        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("gid", gid);

        fragment.setArguments(args);
        return fragment;
    }

    public void gotoIndex(int index) {
        this.gotoIndex = index;
        if (adapter != null) realGoto();
    }

    private void realGoto() {
        if (gotoIndex == -1) return;
        adapter.gotoIndex(gotoIndex);
    }

    @Override
    public void onViewCreated(final View mainView, @Nullable Bundle savedInstanceState) {
        UpdateUI.stop(updateUI);
        final String gid = getArguments().getString("gid");

        try {
            JTA2.newInstance(getActivity()).getFiles(gid, new JTA2.IFiles() {
                @Override
                public void onFiles(final List<AFile> files) {
                    FilesAdapter.setupAsync(getActivity(), gid, Tree.newTree().addElements(files), new FilesAdapter.IAsync() {
                        @Override
                        public void onSetup(final FilesAdapter adapter, final LinearLayout view) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    FilesPagerFragment.this.adapter = adapter;

                                    ScrollView scrollView = (ScrollView) mainView.findViewById(R.id.filesFragment_scrollView);
                                    scrollView.removeAllViews();
                                    scrollView.addView(view);

                                    updateUI = new UpdateUI(getActivity(), gid, adapter);
                                    new Thread(updateUI).start();

                                    realGoto();
                                }
                            });
                        }
                    });
                }

                @Override
                public void onException(Exception exception) {
                    CommonUtils.UIToast(getActivity(), Utils.ToastMessages.FAILED_GATHERING_INFORMATION, exception);
                }
            });
        } catch (IOException | NoSuchAlgorithmException | CertificateException | KeyManagementException | KeyStoreException ex) {
            CommonUtils.UIToast(getActivity(), Utils.ToastMessages.FAILED_GATHERING_INFORMATION, ex);
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

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                rootView.setRefreshing(false);
                            }
                        });
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
