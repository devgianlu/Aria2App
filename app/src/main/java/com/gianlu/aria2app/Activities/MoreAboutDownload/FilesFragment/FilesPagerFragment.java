package com.gianlu.aria2app.Activities.MoreAboutDownload.FilesFragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.gianlu.aria2app.Activities.MoreAboutDownload.CommonFragment;
import com.gianlu.aria2app.NetIO.JTA2.AFile;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.NetIO.JTA2.JTA2InitializingException;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.CommonUtils;

import java.util.List;


public class FilesPagerFragment extends CommonFragment {
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
        final String gid = getArguments().getString("gid");

        try {
            JTA2.instantiate(getActivity()).getFiles(gid, new JTA2.IFiles() {
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

                                    /* TODO
                                    updateUI = new UpdateUI(getActivity(), gid, adapter);
                                    new Thread(updateUI).start();
                                    */

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
        } catch (JTA2InitializingException ex) {
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
                // TODO
            }
        });

        return rootView;
    }

    @Override
    public void stopUpdater() {
        // TODO
    }
}
