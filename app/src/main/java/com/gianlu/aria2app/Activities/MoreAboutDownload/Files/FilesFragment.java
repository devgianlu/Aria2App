package com.gianlu.aria2app.Activities.MoreAboutDownload.Files;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
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
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.gianlu.aria2app.Activities.DirectDownloadActivity;
import com.gianlu.aria2app.Activities.MoreAboutDownload.BigUpdateProvider;
import com.gianlu.aria2app.Activities.MoreAboutDownload.OnBackPressed;
import com.gianlu.aria2app.Adapters.BreadcrumbSegment;
import com.gianlu.aria2app.Adapters.FilesAdapter;
import com.gianlu.aria2app.Downloader.DownloadStartConfig;
import com.gianlu.aria2app.Downloader.DownloaderUtils;
import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;
import com.gianlu.aria2app.NetIO.Aria2.AriaDirectory;
import com.gianlu.aria2app.NetIO.Aria2.AriaFile;
import com.gianlu.aria2app.NetIO.Aria2.AriaFiles;
import com.gianlu.aria2app.NetIO.Aria2.Download;
import com.gianlu.aria2app.NetIO.Aria2.DownloadWithUpdate;
import com.gianlu.aria2app.NetIO.Aria2.TreeNode;
import com.gianlu.aria2app.NetIO.OnRefresh;
import com.gianlu.aria2app.NetIO.Updater.PayloadProvider;
import com.gianlu.aria2app.NetIO.Updater.UpdaterFragment;
import com.gianlu.aria2app.NetIO.Updater.Wants;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.TutorialManager;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.Analytics.AnalyticsApplication;
import com.gianlu.commonutils.Dialogs.DialogUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.RecyclerViewLayout;
import com.gianlu.commonutils.Toaster;

import java.util.Collection;

public class FilesFragment extends UpdaterFragment<DownloadWithUpdate.BigUpdate> implements FilesAdapter.IAdapter, BreadcrumbSegment.IBreadcrumb, ServiceConnection, FileBottomSheet.ISheet, DirBottomSheet.ISheet, OnBackPressed {
    private FilesAdapter adapter;
    private FileBottomSheet fileSheet;
    private DirBottomSheet dirSheet;
    private LinearLayout breadcrumbsContainer;
    private HorizontalScrollView breadcrumbs;
    private boolean isShowingHint;
    private RecyclerViewLayout recyclerViewLayout;
    private Messenger downloaderMessenger = null;
    private IWaitBinder boundWaiter;
    private ActionMode actionMode = null;
    private DownloadWithUpdate download;

    public static FilesFragment getInstance(Context context, String gid) {
        FilesFragment fragment = new FilesFragment();
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.files));
        args.putString("gid", gid);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public boolean canGoBack(int code) {
        if (code == CODE_CLOSE_SHEET) {
            if (fileSheet != null) fileSheet.collapse();
            if (actionMode != null) actionMode.finish();
            return true;
        }

        if (actionMode != null) { // Unluckily ActionMode intercepts the event (useless condition)
            actionMode.finish();
            return false;
        } else if (fileSheet != null && fileSheet.isExpanded()) { // We don't need to do this for dirSheet too, it would be redundant
            fileSheet.collapse();
            return false;
        } else if (adapter != null && adapter.canGoUp()) {
            adapter.navigateUp();
            return false;
        } else {
            return true;
        }
    }

    private void setupView() {
        if (getContext() == null) return;
        final int colorRes = download.update().isTorrent() ? R.color.colorTorrent : R.color.colorAccent;

        adapter = new FilesAdapter(getContext(), colorRes, FilesFragment.this);
        recyclerViewLayout.loadListData(adapter);
        recyclerViewLayout.startLoading();

        recyclerViewLayout.setRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                canGoBack(CODE_CLOSE_SHEET);

                refresh(new OnRefresh() {
                    @Override
                    public void refreshed() {
                        adapter = new FilesAdapter(getContext(), colorRes, FilesFragment.this);
                        recyclerViewLayout.loadListData(adapter);
                        recyclerViewLayout.startLoading();
                    }
                });
            }
        });

        DownloaderUtils.bindService(getContext(), FilesFragment.this);
    }

    @NonNull
    @Override
    protected PayloadProvider<DownloadWithUpdate.BigUpdate> requireProvider(@NonNull Context context, @NonNull Bundle args) throws Aria2Helper.InitializingException {
        return new BigUpdateProvider(context, args.getString("gid"));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle savedInstanceState) {
        CoordinatorLayout layout = (CoordinatorLayout) inflater.inflate(R.layout.fragment_files, parent, false);
        if (getContext() == null) return layout;
        breadcrumbsContainer = layout.findViewById(R.id.filesFragment_breadcrumbsContainer);
        breadcrumbs = layout.findViewById(R.id.filesFragment_breadcrumbs);
        recyclerViewLayout = layout.findViewById(R.id.filesFragment_recyclerViewLayout);
        recyclerViewLayout.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        recyclerViewLayout.getList().addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
        recyclerViewLayout.enableSwipeRefresh(R.color.colorAccent, R.color.colorMetalink, R.color.colorTorrent);

        fileSheet = new FileBottomSheet(layout, this);
        dirSheet = new DirBottomSheet(layout, this);

        recyclerViewLayout.startLoading();

        return layout;
    }

    @Override
    public void onFileSelected(AriaFile file) {
        fileSheet.expand(download, file);
    }

    @Override
    public boolean onFileLongClick(AriaFile file) {
        if (getActivity() == null || download.update().files.size() == 1) return false;

        adapter.enteredActionMode(file);
        actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mode.getMenuInflater().inflate(R.menu.files_action_mode, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.filesActionMode_select:
                        changeSelectionForBatch(adapter.getSelectedFiles(), true);
                        return true;
                    case R.id.filesActionMode_deselect:
                        changeSelectionForBatch(adapter.getSelectedFiles(), false);
                        return true;
                    case R.id.filesActionMode_selectAll:
                        adapter.selectAllInDirectory();
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                actionMode = null;
                adapter.exitedActionMode();
            }
        });
        return true;
    }

    private void changeSelectionForBatch(Collection<AriaFile> files, boolean select) {
        download.changeSelection(AriaFile.allIndexes(files), select, new AbstractClient.OnResult<Download.ChangeSelectionResult>() {
            @Override
            public void onResult(@NonNull Download.ChangeSelectionResult result) {
                switch (result) {
                    case EMPTY:
                        Toaster.show(getActivity(), Utils.Messages.CANT_DESELECT_ALL_FILES);
                        break;
                    case SELECTED:
                        Toaster.show(getActivity(), Utils.Messages.FILES_SELECTED);
                        break;
                    case DESELECTED:
                        Toaster.show(getActivity(), Utils.Messages.FILES_DESELECTED);
                        break;
                }

                exitActionMode();
            }

            @Override
            public void onException(Exception ex, boolean shouldForce) {
                Toaster.show(getActivity(), Utils.Messages.FAILED_CHANGE_FILE_SELECTION, ex);
            }
        });
    }

    @Override
    public void exitActionMode() {
        if (actionMode != null) actionMode.finish();
    }

    @Override
    public void onDirectorySelected(TreeNode dir) {
        dirSheet.expand(download, new AriaDirectory(dir, download));
    }

    private void showTutorial(TreeNode dir) {
        if (isVisible() && !isShowingHint && dir.files != null && dir.dirs != null && dir.files.size() >= 1 && TutorialManager.shouldShowHintFor(getContext(), TutorialManager.Discovery.FILES)) {
            RecyclerView.ViewHolder holder = recyclerViewLayout.getList().findViewHolderForLayoutPosition(dir.dirs.size());
            if (holder != null && getActivity() != null) {
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

    private void startDownloadInternal(final MultiProfile profile, @Nullable final AriaFile file, @Nullable final AriaDirectory dir) {
        try {
            if (getContext() == null)
                throw new DownloadStartConfig.CannotCreateStartConfigException(new NullPointerException("Context is null!"));

            DownloaderUtils.startDownload(downloaderMessenger,
                    file == null ?
                            DownloadStartConfig.create(getContext(), download, profile.getProfile(getContext()), dir) :
                            DownloadStartConfig.create(getContext(), download, profile.getProfile(getContext()), file));
        } catch (DownloaderUtils.InvalidPathException | DownloadStartConfig.CannotCreateStartConfigException ex) {
            Toaster.show(getActivity(), Utils.Messages.FAILED_DOWNLOAD_DIR, ex);
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
    public void onServiceConnected(ComponentName name, IBinder service) {
        downloaderMessenger = new Messenger(service);
        if (boundWaiter != null) boundWaiter.onBound();
        boundWaiter = null;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        downloaderMessenger = null;
    }

    @Override
    public void onDownloadFile(final MultiProfile profile, final AriaFile file) {
        if (fileSheet != null) fileSheet.collapse();

        String mime = file.getMimeType();
        if (mime != null) {
            if (Utils.isStreamable(mime) && getContext() != null) {

                final Intent intent = Utils.getStreamIntent(download, profile.getProfile(getContext()), file);
                if (intent != null && Utils.canHandleIntent(getContext(), intent)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle(R.string.couldStreamVideo)
                            .setMessage(R.string.couldStreamVideo_message)
                            .setNeutralButton(android.R.string.cancel, null)
                            .setPositiveButton(R.string.stream, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    startActivity(intent);
                                    AnalyticsApplication.sendAnalytics(getContext(), Utils.ACTION_PLAY_VIDEO);
                                }
                            })
                            .setNegativeButton(R.string.download, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    shouldDownload(profile, file);
                                }
                            });

                    DialogUtils.showDialog(getActivity(), builder);
                    return;
                }
            }
        }

        shouldDownload(profile, file);
    }

    private void shouldDownload(final MultiProfile profile, final AriaFile file) {
        if (downloaderMessenger != null) {
            startDownloadInternal(profile, file, null);
        } else {
            boundWaiter = new IWaitBinder() {
                @Override
                public void onBound() {
                    startDownloadInternal(profile, file, null);
                }
            };
        }

        AnalyticsApplication.sendAnalytics(getContext(), Utils.ACTION_DOWNLOAD_FILE);
    }

    @Override
    public void showToast(Toaster.Message message) {
        Toaster.show(getActivity(), message, new Runnable() {
            @Override
            public void run() {
                if (dirSheet != null)
                    dirSheet.collapse(); // We don't need to do this for dirSheet too, it would be redundant
            }
        });
    }

    @Override
    public void onDownloadDirectory(final MultiProfile profile, final AriaDirectory dir) {
        if (dirSheet != null) dirSheet.collapse();

        if (downloaderMessenger != null) {
            startDownloadInternal(profile, null, dir);
        } else {
            boundWaiter = new IWaitBinder() {
                @Override
                public void onBound() {
                    startDownloadInternal(profile, null, dir);
                }
            };
        }

        AnalyticsApplication.sendAnalytics(getContext(), Utils.ACTION_DOWNLOAD_DIRECTORY);
    }

    @Override
    public void onUpdateUi(@NonNull DownloadWithUpdate.BigUpdate payload) {
        AriaFiles files = payload.files;
        if (files.isEmpty() || files.get(0).path.isEmpty()) {
            recyclerViewLayout.showMessage(R.string.noFiles, false);
        } else {
            recyclerViewLayout.showList();
            if (adapter != null) adapter.update(payload.download(), files);
            if (fileSheet != null) fileSheet.update(files);
            if (dirSheet != null) dirSheet.update(payload.download(), files);
            if (adapter != null) showTutorial(adapter.getCurrentNode());
        }
    }

    @Override
    public void onLoadUi(@NonNull DownloadWithUpdate.BigUpdate payload) {
        this.download = payload.download();
        setupView();
    }

    @Override
    public boolean onCouldntLoad(@NonNull Exception ex) {
        recyclerViewLayout.showMessage(R.string.failedLoading, true);
        Logging.log(ex);
        return false;
    }

    @NonNull
    @Override
    public Wants<DownloadWithUpdate.BigUpdate> wants(@NonNull Bundle args) {
        return Wants.bigUpdate(args.getString("gid"));
    }

    private interface IWaitBinder {
        void onBound();
    }
}
