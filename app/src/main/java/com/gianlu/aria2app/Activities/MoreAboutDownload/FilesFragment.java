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
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.gianlu.aria2app.Activities.MoreAboutDownload.Files.FileBottomSheet;
import com.gianlu.aria2app.Activities.MoreAboutDownload.Files.TreeNode;
import com.gianlu.aria2app.Activities.MoreAboutDownload.Files.UpdateUI;
import com.gianlu.aria2app.Adapters.BreadcrumbSegment;
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

public class FilesFragment extends BackPressedFragment implements UpdateUI.IUI, FilesAdapter.IAdapter, BreadcrumbSegment.IBreadcrumb {
    private UpdateUI updater;
    private CoordinatorLayout layout;
    private RecyclerView list;
    private ProgressBar loading;
    private FilesAdapter adapter;
    private FileBottomSheet sheet;
    private LinearLayout container;
    private LinearLayout breadcrumbsContainer;
    private HorizontalScrollView breadcrumbs;

    public static FilesFragment getInstance(Context context, Download download) {
        FilesFragment fragment = new FilesFragment();
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.files));
        args.putSerializable("download", download);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public boolean canGoBack() {
        if (sheet.shouldUpdate()) {
            sheet.collapse();
            return false;
        } else if (adapter.canGoUp()) {
            adapter.navigateUp();
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
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle savedInstanceState) {
        layout = (CoordinatorLayout) inflater.inflate(R.layout.files_fragment, parent, false);
        final SwipeRefreshLayout swipeRefresh = (SwipeRefreshLayout) layout.findViewById(R.id.filesFragment_swipeRefresh);
        swipeRefresh.setColorSchemeResources(R.color.colorAccent, R.color.colorMetalink, R.color.colorTorrent);
        loading = (ProgressBar) layout.findViewById(R.id.filesFragment_loading);
        container = (LinearLayout) layout.findViewById(R.id.filesFragment_container);
        breadcrumbsContainer = (LinearLayout) layout.findViewById(R.id.filesFragment_breadcrumbsContainer);
        breadcrumbs = (HorizontalScrollView) layout.findViewById(R.id.filesFragment_breadcrumbs);
        list = (RecyclerView) layout.findViewById(R.id.filesFragment_list);
        list.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        list.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));

        final Download download = (Download) getArguments().getSerializable("download");
        if (download == null) {
            MessageLayout.show(layout, R.string.failedLoading, R.drawable.ic_error_black_48dp);
            loading.setVisibility(View.GONE);
            return layout;
        }

        final int colorRes = download.isTorrent() ? R.color.colorTorrent : R.color.colorAccent;

        adapter = new FilesAdapter(getContext(), colorRes, this);
        list.setAdapter(adapter);

        sheet = new FileBottomSheet(layout, download);

        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updater.stopThread(new BaseUpdater.IThread() {
                    @Override
                    public void onStopped() {
                        try {
                            adapter = new FilesAdapter(getContext(), colorRes, FilesFragment.this);
                            list.setAdapter(adapter);

                            updater = new UpdateUI(getContext(), download.gid, FilesFragment.this);
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
            updater = new UpdateUI(getContext(), download.gid, this);
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
        if (files.size() == 0) return;
        MessageLayout.hide(layout);
        loading.setVisibility(View.GONE);
        container.setVisibility(View.VISIBLE);
        if (adapter != null) adapter.update(files, commonRoot);
        if (sheet != null) sheet.update(files);
    }

    @Override
    public void onFatalException(Exception ex) {
        MessageLayout.show(layout, R.string.failedLoading, R.drawable.ic_error_black_48dp);
        loading.setVisibility(View.GONE);
        container.setVisibility(View.GONE);
        Logging.logMe(getContext(), ex);
    }

    @Override
    public void onFileSelected(AFile file) {
        sheet.expand(file);
    }

    @Override
    public void onDirectoryChanged(TreeNode dir) {
        breadcrumbsContainer.removeAllViews();

        TreeNode node = dir;
        do {
            addPathToBreadcrumbs(node);
            node = node.parent;
        } while (node != null);

        breadcrumbs.post(new Runnable() {
            @Override
            public void run() {
                breadcrumbs.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
            }
        });
    }

    private void addPathToBreadcrumbs(TreeNode dir) {
        breadcrumbsContainer.addView(new BreadcrumbSegment(getContext(), dir, this), 0);
    }

    @Override
    public void onDirSelected(TreeNode node) {
        if (adapter != null) adapter.rebaseTo(node);
    }
}
