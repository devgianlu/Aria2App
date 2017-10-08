package com.gianlu.aria2app.Activities.MoreAboutDownload;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Messenger;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.gianlu.aria2app.Activities.DirectDownloadActivity;
import com.gianlu.aria2app.Activities.MoreAboutDownload.Files.DirBottomSheet;
import com.gianlu.aria2app.Activities.MoreAboutDownload.Files.FileBottomSheet;
import com.gianlu.aria2app.Activities.MoreAboutDownload.Files.TreeNode;
import com.gianlu.aria2app.Activities.MoreAboutDownload.Files.UpdateUI;
import com.gianlu.aria2app.Adapters.BreadcrumbSegment;
import com.gianlu.aria2app.Adapters.FilesAdapter;
import com.gianlu.aria2app.Downloader.DownloadStartConfig;
import com.gianlu.aria2app.Downloader.DownloaderUtils;
import com.gianlu.aria2app.NetIO.BaseUpdater;
import com.gianlu.aria2app.NetIO.JTA2.ADir;
import com.gianlu.aria2app.NetIO.JTA2.AFile;
import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;
import com.gianlu.aria2app.NetIO.JTA2.JTA2InitializingException;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.ThisApplication;
import com.gianlu.aria2app.TutorialManager;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.RecyclerViewLayout;
import com.gianlu.commonutils.Toaster;
import com.google.android.gms.analytics.HitBuilders;

import java.net.URISyntaxException;
import java.util.List;

public class FilesFragment extends BackPressedFragment implements UpdateUI.IUI, FilesAdapter.IAdapter, BreadcrumbSegment.IBreadcrumb, FileBottomSheet.ISheet, DirBottomSheet.ISheet, ServiceConnection {
    private UpdateUI updater;
    private FilesAdapter adapter;
    private FileBottomSheet fileSheet;
    private DirBottomSheet dirSheet;
    private LinearLayout breadcrumbsContainer;
    private HorizontalScrollView breadcrumbs;
    private Download download;
    private boolean isShowingHint;
    private RecyclerViewLayout recyclerViewLayout;
    private Messenger downloaderMessenger = null;
    private IWaitBinder boundWaiter;

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
        if (fileSheet == null) return true;

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
        CoordinatorLayout layout = (CoordinatorLayout) inflater.inflate(R.layout.files_fragment, parent, false);
        breadcrumbsContainer = layout.findViewById(R.id.filesFragment_breadcrumbsContainer);
        breadcrumbs = layout.findViewById(R.id.filesFragment_breadcrumbs);
        recyclerViewLayout = layout.findViewById(R.id.filesFragment_recyclerViewLayout);
        recyclerViewLayout.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        recyclerViewLayout.getList().addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
        recyclerViewLayout.enableSwipeRefresh(R.color.colorAccent, R.color.colorMetalink, R.color.colorTorrent);

        download = (Download) getArguments().getSerializable("download");
        if (download == null) {
            recyclerViewLayout.showMessage(R.string.failedLoading, true);
            return layout;
        }

        final int colorRes = download.isTorrent() ? R.color.colorTorrent : R.color.colorAccent;

        adapter = new FilesAdapter(getContext(), colorRes, this);
        recyclerViewLayout.loadListData(adapter);
        recyclerViewLayout.startLoading();

        fileSheet = new FileBottomSheet(layout, download, this);
        dirSheet = new DirBottomSheet(layout, download, this);

        recyclerViewLayout.setRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updater.stopThread(new BaseUpdater.IThread() {
                    @Override
                    public void onStopped() {
                        try {
                            adapter = new FilesAdapter(getContext(), colorRes, FilesFragment.this);
                            recyclerViewLayout.loadListData(adapter);
                            recyclerViewLayout.startLoading();

                            updater = new UpdateUI(getContext(), download.gid, FilesFragment.this);
                            updater.start();
                        } catch (JTA2InitializingException ex) {
                            Toaster.show(getActivity(), Utils.Messages.FAILED_REFRESHING, ex);
                        }
                    }
                });
            }
        });

        try {
            updater = new UpdateUI(getContext(), download.gid, this);
            updater.start();
        } catch (JTA2InitializingException ex) {
            recyclerViewLayout.showMessage(R.string.failedLoading, true);
            Logging.logMe(getContext(), ex);
            return layout;
        }

        DownloaderUtils.bindService(getContext(), this);

        return layout;
    }

    @Override
    public void onUpdateHierarchy(List<AFile> files, String commonRoot) {
        if (files.size() == 0 || files.get(0).path.isEmpty()) {
            recyclerViewLayout.showMessage(R.string.noFiles, false);
        } else {
            recyclerViewLayout.showList();

            if (adapter != null) adapter.update(files, commonRoot);
            if (fileSheet != null) fileSheet.update(files);
            if (dirSheet != null) dirSheet.update(files);

            if (adapter != null) showTutorial(adapter.getCurrentNode());
        }
    }

    @Override
    public void onFatalException(Exception ex) {
        recyclerViewLayout.showMessage(R.string.failedLoading, true);
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
            RecyclerView.ViewHolder holder = recyclerViewLayout.getList().findViewHolderForLayoutPosition(dir.dirs.size());
            if (holder != null) {
                isShowingHint = true;

                recyclerViewLayout.getList().scrollToPosition(dir.dirs.size());

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
            RecyclerView.ViewHolder holder = recyclerViewLayout.getList().findViewHolderForLayoutPosition(0);
            if (holder != null) {
                isShowingHint = true;

                recyclerViewLayout.getList().scrollToPosition(0);

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
        Context context = getContext();
        if (context != null)
            breadcrumbsContainer.addView(new BreadcrumbSegment(context, dir, this), 0);
    }

    @Override
    public void onDirSelected(TreeNode node) {
        if (adapter != null) adapter.rebaseTo(node);
    }

    @Override
    public void onSelectedFile(AFile file) {
        Toaster.show(getActivity(), Utils.Messages.FILE_SELECTED, file.getName());
        adapter.notifyItemChanged(file);
        if (fileSheet != null) fileSheet.collapse();
    }

    @Override
    public void onDeselectedFile(AFile file) {
        Toaster.show(getActivity(), Utils.Messages.FILE_DESELECTED, file.getName());
        adapter.notifyItemChanged(file);
        if (fileSheet != null) fileSheet.collapse();
    }

    @Override
    public void onSelectedDir(ADir dir) {
        Toaster.show(getActivity(), Utils.Messages.DIR_SELECTED, dir.name);
        adapter.notifyItemsChanged(dir, true);
        if (dirSheet != null) dirSheet.collapse();
    }

    @Override
    public void onDeselectedDir(ADir dir) {
        Toaster.show(getActivity(), Utils.Messages.DIR_DESELECTED, dir.name);
        adapter.notifyItemsChanged(dir, false);
        if (dirSheet != null) dirSheet.collapse();
    }

    @Override
    public void onExceptionChangingSelection(Exception ex) {
        Toaster.show(getActivity(), Utils.Messages.FAILED_CHANGE_FILE_SELECTION, ex);
        if (fileSheet != null) fileSheet.collapse();
    }

    @Override
    public void onWantsToDownload(final MultiProfile profile, final String gid, @NonNull final ADir dir) {
        if (fileSheet != null) fileSheet.collapse();

        final ProgressDialog pd = CommonUtils.fastIndeterminateProgressDialog(getContext(), R.string.gathering_information);
        CommonUtils.showDialog(getActivity(), pd);

        if (downloaderMessenger != null) {
            startDownloadInternal(pd, gid, profile, null, dir);
        } else {
            boundWaiter = new IWaitBinder() {
                @Override
                public void onBound() {
                    startDownloadInternal(pd, gid, profile, null, dir);
                }
            };
        }

        ThisApplication.sendAnalytics(getContext(), new HitBuilders.EventBuilder()
                .setCategory(ThisApplication.CATEGORY_USER_INPUT)
                .setAction(ThisApplication.ACTION_DOWNLOAD_DIRECTORY)
                .build());
    }

    @Override
    public void onCantDeselectAll() {
        Toaster.show(getActivity(), Utils.Messages.CANT_DESELECT_ALL_FILES, download.gid);
        if (dirSheet != null) dirSheet.collapse();
    }

    private void startDownloadInternal(final ProgressDialog pd, String gid, final MultiProfile profile, @Nullable final AFile file, @Nullable final ADir dir) {
        JTA2 jta2;
        try {
            jta2 = JTA2.instantiate(getContext());
        } catch (JTA2InitializingException ex) {
            pd.dismiss();
            Toaster.show(getActivity(), Utils.Messages.FAILED_LOADING, ex);
            return;
        }

        jta2.tellStatus(gid, new String[]{"gid", "status", "dir"}, new JTA2.IDownload() {
            @Override
            public void onDownload(Download download) {
                pd.dismiss();

                try {
                    DownloaderUtils.startDownload(downloaderMessenger, file == null ? DownloadStartConfig.create(getContext(), download, profile.getProfile(getContext()), dir) : DownloadStartConfig.create(getContext(), download, profile.getProfile(getContext()), file));
                } catch (DownloaderUtils.InvalidPathException | URISyntaxException ex) {
                    onException(ex);
                    return;
                }

                Snackbar.make(recyclerViewLayout, R.string.downloadAdded, Snackbar.LENGTH_LONG)
                        .setAction(R.string.show, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                startActivity(new Intent(getContext(), DirectDownloadActivity.class));
                            }
                        }).show();
            }

            @Override
            public void onException(Exception ex) {
                pd.dismiss();

                if (file == null)
                    Toaster.show(getActivity(), Utils.Messages.FAILED_DOWNLOAD_DIR, ex);
                else
                    Toaster.show(getActivity(), Utils.Messages.FAILED_DOWNLOAD_FILE, ex);
            }
        });
    }

    @Override
    public void onWantsToDownload(final MultiProfile profile, final String gid, @NonNull final AFile file) {
        if (fileSheet != null) fileSheet.collapse();

        final ProgressDialog pd = CommonUtils.fastIndeterminateProgressDialog(getContext(), R.string.gathering_information);
        CommonUtils.showDialog(getActivity(), pd);

        if (downloaderMessenger != null) {
            startDownloadInternal(pd, gid, profile, file, null);
        } else {
            boundWaiter = new IWaitBinder() {
                @Override
                public void onBound() {
                    startDownloadInternal(pd, gid, profile, file, null);
                }
            };
        }

        ThisApplication.sendAnalytics(getContext(), new HitBuilders.EventBuilder()
                .setCategory(ThisApplication.CATEGORY_USER_INPUT)
                .setAction(ThisApplication.ACTION_DOWNLOAD_FILE)
                .build());
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        downloaderMessenger = new Messenger(service);
        if (boundWaiter != null) boundWaiter.onBound();
        boundWaiter = null;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        downloaderMessenger = null;
    }

    private interface IWaitBinder {
        void onBound();
    }
}
