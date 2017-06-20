package com.gianlu.aria2app.Activities.MoreAboutDownload;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.gianlu.aria2app.Activities.MoreAboutDownload.Files.DirBottomSheet;
import com.gianlu.aria2app.Activities.MoreAboutDownload.Files.FileBottomSheet;
import com.gianlu.aria2app.Activities.MoreAboutDownload.Files.TreeNode;
import com.gianlu.aria2app.Activities.MoreAboutDownload.Files.UpdateUI;
import com.gianlu.aria2app.Adapters.BreadcrumbSegment;
import com.gianlu.aria2app.Adapters.FilesAdapter;
import com.gianlu.aria2app.NetIO.BaseUpdater;
import com.gianlu.aria2app.NetIO.DownloadsManager.DownloadsManager;
import com.gianlu.aria2app.NetIO.DownloadsManager.DownloadsManagerException;
import com.gianlu.aria2app.NetIO.JTA2.ADir;
import com.gianlu.aria2app.NetIO.JTA2.AFile;
import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.NetIO.JTA2.JTA2InitializingException;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.ThisApplication;
import com.gianlu.aria2app.TutorialManager;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.MessageLayout;
import com.google.android.gms.analytics.HitBuilders;

import java.util.Collections;
import java.util.List;

// FIXME: No folders
public class FilesFragment extends BackPressedFragment implements UpdateUI.IUI, FilesAdapter.IAdapter, BreadcrumbSegment.IBreadcrumb, FileBottomSheet.ISheet, DirBottomSheet.ISheet {
    private UpdateUI updater;
    private CoordinatorLayout layout;
    private RecyclerView list;
    private ProgressBar loading;
    private FilesAdapter adapter;
    private FileBottomSheet fileSheet;
    private DirBottomSheet dirSheet;
    private LinearLayout container;
    private LinearLayout breadcrumbsContainer;
    private HorizontalScrollView breadcrumbs;
    private Download download;
    private boolean isShowingHint;

    public static FilesFragment getInstance(Context context, Download download) {
        FilesFragment fragment = new FilesFragment();
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.files));
        args.putSerializable("download", download);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public boolean canGoBack(int code) {
        if (code == CODE_CLOSE_SHEET) {
            fileSheet.collapse();
            return true;
        }

        if (fileSheet.shouldUpdate()) { // We don't need to do this for dirSheet too, it would be redundant
            fileSheet.collapse();
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


        download = (Download) getArguments().getSerializable("download");
        if (download == null) {
            MessageLayout.show(layout, R.string.failedLoading, R.drawable.ic_error_black_48dp);
            loading.setVisibility(View.GONE);
            return layout;
        }

        final int colorRes = download.isTorrent() ? R.color.colorTorrent : R.color.colorAccent;

        adapter = new FilesAdapter(getContext(), colorRes, this);
        list.setAdapter(adapter);

        fileSheet = new FileBottomSheet(layout, download, this);
        dirSheet = new DirBottomSheet(layout, download, this);

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
        if (fileSheet != null) fileSheet.update(files);
        if (dirSheet != null) dirSheet.update(files);

        if (adapter != null) showTutorial(adapter.getCurrentNode());
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
        fileSheet.expand(file);
    }

    @Override
    public void onDirectorySelected(TreeNode dir) {
        dirSheet.expand(new ADir(dir, download));
    }

    private void showTutorial(TreeNode dir) {
        if (isVisible() && !isShowingHint && dir.files != null && dir.dirs != null && dir.files.size() >= 1 && TutorialManager.shouldShowHintFor(getContext(), TutorialManager.Discovery.FILES)) {
            RecyclerView.ViewHolder holder = list.findViewHolderForLayoutPosition(dir.dirs.size());
            if (holder != null) {
                isShowingHint = true;

                list.scrollToPosition(dir.dirs.size());

                Rect rect = new Rect();
                holder.itemView.getGlobalVisibleRect(rect);
                rect.offset((int) -(holder.itemView.getWidth() * 0.3), 0);

                TapTargetView.showFor(getActivity(), TapTarget.forBounds(rect, getString(R.string.fileDetails), getString(R.string.fileDetails_desc))
                                .tintTarget(false)
                                .transparentTarget(true),
                        new TapTargetView.Listener() {
                            @Override
                            public void onTargetDismissed(TapTargetView view, boolean userInitiated) {
                                TutorialManager.setHintShown(getContext(), TutorialManager.Discovery.FILES);
                                isShowingHint = false;
                            }
                        });
            }
        }

        if (isVisible() && !isShowingHint && dir.dirs != null && dir.dirs.size() >= 1 && TutorialManager.shouldShowHintFor(getContext(), TutorialManager.Discovery.FOLDERS)) {
            RecyclerView.ViewHolder holder = list.findViewHolderForLayoutPosition(0);
            if (holder != null) {
                isShowingHint = true;

                list.scrollToPosition(0);

                Rect rect = new Rect();
                holder.itemView.getGlobalVisibleRect(rect);
                rect.offset((int) -(holder.itemView.getWidth() * 0.3), 0);

                TapTargetView.showFor(getActivity(), TapTarget.forBounds(rect, getString(R.string.folderDetails), getString(R.string.folderDetails_desc))
                                .tintTarget(false)
                                .transparentTarget(true),
                        new TapTargetView.Listener() {
                            @Override
                            public void onTargetDismissed(TapTargetView view, boolean userInitiated) {
                                TutorialManager.setHintShown(getContext(), TutorialManager.Discovery.FOLDERS);
                                isShowingHint = false;
                            }
                        });
            }
        }
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

        showTutorial(dir);
    }

    private void addPathToBreadcrumbs(TreeNode dir) {
        breadcrumbsContainer.addView(new BreadcrumbSegment(getContext(), dir, this), 0);
    }

    @Override
    public void onDirSelected(TreeNode node) {
        if (adapter != null) adapter.rebaseTo(node);
    }

    @Override
    public void onSelectedFile(AFile file) {
        CommonUtils.UIToast(getActivity(), Utils.ToastMessages.FILE_SELECTED, file.getName());
        adapter.notifyItemChanged(file);
        if (fileSheet != null) fileSheet.collapse();
    }

    @Override
    public void onDeselectedFile(AFile file) {
        CommonUtils.UIToast(getActivity(), Utils.ToastMessages.FILE_DESELECTED, file.getName());
        adapter.notifyItemChanged(file);
        if (fileSheet != null) fileSheet.collapse();
    }

    @Override
    public void onSelectedDir(ADir dir) {
        CommonUtils.UIToast(getActivity(), Utils.ToastMessages.DIR_SELECTED, dir.name);
        adapter.notifyItemsChanged(dir, true);
        if (dirSheet != null) dirSheet.collapse();
    }

    @Override
    public void onDeselectedDir(ADir dir) {
        CommonUtils.UIToast(getActivity(), Utils.ToastMessages.DIR_DESELECTED, dir.name);
        adapter.notifyItemsChanged(dir, false);
        if (dirSheet != null) dirSheet.collapse();
    }

    @Override
    public void onExceptionChangingSelection(Exception ex) {
        CommonUtils.UIToast(getActivity(), Utils.ToastMessages.FAILED_CHANGE_FILE_SELECTION, ex);
        if (fileSheet != null) fileSheet.collapse();
    }

    @Override
    public void onWantsToDownload(final Download download, final ADir dir) {
        JTA2 jta2;
        try {
            jta2 = JTA2.instantiate(getContext());
        } catch (JTA2InitializingException ex) {
            CommonUtils.UIToast(getActivity(), Utils.ToastMessages.FAILED_DOWNLOAD_DIR, ex);
            return;
        }

        final ProgressDialog pd = CommonUtils.fastIndeterminateProgressDialog(getContext(), R.string.gathering_information);
        CommonUtils.showDialog(getActivity(), pd);

        jta2.getFiles(download.gid, new JTA2.IFiles() {
            @Override
            public void onFiles(List<AFile> files) {
                realStartDownload(ADir.find(dir, files), download.dir);
                pd.dismiss();
            }

            @Override
            public void onException(Exception ex) {
                CommonUtils.UIToast(getActivity(), Utils.ToastMessages.FAILED_DOWNLOAD_FILE, ex);
                pd.dismiss();
            }
        });

        ThisApplication.sendAnalytics(getContext(), new HitBuilders.EventBuilder()
                .setCategory(ThisApplication.CATEGORY_USER_INPUT)
                .setAction(ThisApplication.ACTION_DOWNLOAD_DIRECTORY)
                .build());
    }

    @Override
    public void onCantDeselectAll() {
        CommonUtils.UIToast(getActivity(), Utils.ToastMessages.CANT_DESELECT_ALL_FILES, download.gid);
        if (dirSheet != null) dirSheet.collapse();
    }

    @Override
    public void onWantsToDownload(final Download download, final AFile file) {
        JTA2 jta2;
        try {
            jta2 = JTA2.instantiate(getContext());
        } catch (JTA2InitializingException ex) {
            CommonUtils.UIToast(getActivity(), Utils.ToastMessages.FAILED_DOWNLOAD_FILE, ex);
            return;
        }

        final ProgressDialog pd = CommonUtils.fastIndeterminateProgressDialog(getContext(), R.string.gathering_information);
        CommonUtils.showDialog(getActivity(), pd);

        jta2.getFiles(download.gid, new JTA2.IFiles() {
            @Override
            public void onFiles(List<AFile> files) {
                startDownload(AFile.find(files, file), download.dir);
                pd.dismiss();
            }

            @Override
            public void onException(Exception ex) {
                CommonUtils.UIToast(getActivity(), Utils.ToastMessages.FAILED_DOWNLOAD_FILE, ex);
                pd.dismiss();
            }
        });

        ThisApplication.sendAnalytics(getContext(), new HitBuilders.EventBuilder()
                .setCategory(ThisApplication.CATEGORY_USER_INPUT)
                .setAction(ThisApplication.ACTION_DOWNLOAD_FILE)
                .build());
    }

    private void realStartDownload(List<AFile> files, String dir) {
        try {
            for (AFile file : files)
                DownloadsManager.get(getContext()).startDownload(getContext(), file, dir);
            CommonUtils.UIToast(getActivity(), Utils.ToastMessages.DOWNLOAD_STARTED);
        } catch (DownloadsManagerException ex) {
            CommonUtils.UIToast(getActivity(), Utils.ToastMessages.FAILED_DOWNLOAD_FILE, ex);
        }
    }

    private void startDownload(final AFile file, final String dir) {
        if (file.completed()) {
            realStartDownload(Collections.singletonList(file), dir);
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(R.string.downloadIncomplete)
                    .setMessage(R.string.downloadIncompleteMessage)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            realStartDownload(Collections.singletonList(file), dir);
                        }
                    })
                    .setNegativeButton(android.R.string.no, null);

            CommonUtils.showDialog(getActivity(), builder);
        }
    }
}
