package com.gianlu.aria2app.Activities.MoreAboutDownload;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.gianlu.aria2app.Activities.MoreAboutDownload.Files.FileBottomSheet;
import com.gianlu.aria2app.Activities.MoreAboutDownload.Files.UpdateUI;
import com.gianlu.aria2app.Adapters.FilesAdapter;
import com.gianlu.aria2app.NetIO.BaseUpdater;
import com.gianlu.aria2app.NetIO.JTA2.AFile;
import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.NetIO.JTA2.JTA2InitializingException;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.MessageLayout;

import java.util.List;

public class FilesFragment extends BackPressedFragment implements UpdateUI.IUI, FilesAdapter.IAdapter {
    private UpdateUI updater;
    private CoordinatorLayout layout;
    private RecyclerView list;
    private ProgressBar loading;
    private FilesAdapter adapter;
    private FileBottomSheet sheet;

    public static FilesFragment getInstance(Context context, Download download) {
        FilesFragment fragment = new FilesFragment();
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.files));
        args.putString("gid", download.gid);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public boolean canGoBack() {
        if (sheet.shouldUpdate()) {
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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        layout = (CoordinatorLayout) inflater.inflate(R.layout.files_fragment, container, false);
        final SwipeRefreshLayout swipeRefresh = (SwipeRefreshLayout) layout.findViewById(R.id.filesFragment_swipeRefresh);
        swipeRefresh.setColorSchemeResources(R.color.colorAccent, R.color.colorMetalink, R.color.colorTorrent);
        loading = (ProgressBar) layout.findViewById(R.id.filesFragment_loading);
        list = (RecyclerView) layout.findViewById(R.id.filesFragment_list);
        list.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        list.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
        adapter = new FilesAdapter(getContext());
        list.setAdapter(adapter);

        sheet = new FileBottomSheet(layout);

        final String gid = getArguments().getString("gid");
        if (gid == null) {
            MessageLayout.show(layout, R.string.failedLoading, R.drawable.ic_error_black_48dp);
            loading.setVisibility(View.GONE);
            return layout;
        }

        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updater.stopThread(new BaseUpdater.IThread() {
                    @Override
                    public void onStopped() {
                        try {
                            adapter = new FilesAdapter(getContext());
                            list.setAdapter(adapter);

                            updater = new UpdateUI(getContext(), gid, FilesFragment.this);
                            updater.start();
                        } catch (JTA2InitializingException ex) {
                            CommonUtils.UIToast(getActivity(), Utils.ToastMessages.FAILED_REFRESHING, ex);
                        } finally {
                            if (isAdded()) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        swipeRefresh.setRefreshing(false);
                                    }
                                });
                            }
                        }
                    }
                });
            }
        });

        try {
            updater = new UpdateUI(getContext(), gid, this);
            updater.start();
        } catch (JTA2InitializingException ex) {
            MessageLayout.show(layout, R.string.failedLoading, R.drawable.ic_error_black_48dp);
            loading.setVisibility(View.GONE);
            Logging.logMe(getContext(), ex);
            return layout;
        }

        return layout;
    }

    @Override
    public void onUpdateHierarchy(List<AFile> files, String commonRoot) {
        adapter.update(files, commonRoot);
    }

    @Override
    public void onFileSelected(AFile file) {
        sheet.expand(file);
    }

    @Override
    public void onDirectoryChanged(String dir) {

    }
}
