package com.gianlu.aria2app.Activities.MoreAboutDownload;

import android.app.Activity;
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
import com.gianlu.aria2app.Activities.MoreAboutDownload.Files.UpdateUI;
import com.gianlu.aria2app.Adapters.BreadcrumbSegment;
import com.gianlu.aria2app.Adapters.FilesAdapter;
import com.gianlu.aria2app.Downloader.DownloadStartConfig;
import com.gianlu.aria2app.Downloader.DownloaderUtils;
import com.gianlu.aria2app.ExternalPlayers;
import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;
import com.gianlu.aria2app.NetIO.Aria2.AriaDirectory;
import com.gianlu.aria2app.NetIO.Aria2.AriaFile;
import com.gianlu.aria2app.NetIO.Aria2.Download;
import com.gianlu.aria2app.NetIO.Aria2.DownloadStatic;
import com.gianlu.aria2app.NetIO.Aria2.DownloadWithHelper;
import com.gianlu.aria2app.NetIO.Aria2.TreeNode;
import com.gianlu.aria2app.NetIO.AriaRequests;
import com.gianlu.aria2app.NetIO.OnRefresh;
import com.gianlu.aria2app.NetIO.Updater.BaseUpdater;
import com.gianlu.aria2app.NetIO.Updater.DownloadUpdaterFragment;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.TutorialManager;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.Analytics.AnalyticsApplication;
import com.gianlu.commonutils.Dialogs.DialogUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.RecyclerViewLayout;
import com.gianlu.commonutils.Toaster;

import java.util.List;

import okhttp3.HttpUrl;

public class FilesFragment extends DownloadUpdaterFragment implements FilesAdapter.IAdapter, BreadcrumbSegment.IBreadcrumb, ServiceConnection, FileBottomSheet.ISheet, DirBottomSheet.ISheet, OnBackPressed, BaseUpdater.UpdaterListener<List<AriaFile>> {
    private FilesAdapter adapter;
    private FileBottomSheet fileSheet;
    private DirBottomSheet dirSheet;
    private LinearLayout breadcrumbsContainer;
    private HorizontalScrollView breadcrumbs;
    private boolean isShowingHint;
    private RecyclerViewLayout recyclerViewLayout;
    private Messenger downloaderMessenger = null;
    private IWaitBinder boundWaiter;
    private DownloadWithHelper download;

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

        if (fileSheet.isExpanded()) { // We don't need to do this for dirSheet too, it would be redundant
            fileSheet.collapse();
            return false;
        } else if (adapter != null && adapter.canGoUp()) {
            adapter.navigateUp();
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onBackPressed() {
        stopUpdater();
    }

    private void setupView() {
        if (getContext() == null) return;
        final int colorRes = download.get().isTorrent() ? R.color.colorTorrent : R.color.colorAccent;

        adapter = new FilesAdapter(getContext(), colorRes, FilesFragment.this);
        recyclerViewLayout.loadListData(adapter);
        recyclerViewLayout.startLoading();

        recyclerViewLayout.setRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
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

        DownloadStatic downloadStatic;
        Bundle args = getArguments();
        if (args == null || (downloadStatic = (DownloadStatic) args.getSerializable("download")) == null) {
            recyclerViewLayout.showMessage(R.string.failedLoading, true);
            return layout;
        }

        recyclerViewLayout.startLoading();
        try {
            Aria2Helper.instantiate(getContext()).request(AriaRequests.tellStatus(downloadStatic.gid), new AbstractClient.OnResult<DownloadWithHelper>() {
                @Override
                public void onResult(DownloadWithHelper result) {
                    download = result;

                    Activity activity = getActivity();
                    if (activity != null) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setupView();
                            }
                        });
                    }
                }

                @Override
                public void onException(final Exception ex) {
                    Logging.log(ex);

                    Activity activity = getActivity();
                    if (activity != null) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                recyclerViewLayout.showMessage(R.string.failedLoading_reason, true, ex.getMessage());
                            }
                        });
                    }
                }
            });
        } catch (Aria2Helper.InitializingException ex) {
            Logging.log(ex);
            recyclerViewLayout.showMessage(R.string.failedLoading_reason, true, ex.getMessage());
            return layout;
        }

        return layout;
    }

    @Override
    public void onFileSelected(AriaFile file) {
        fileSheet.expand(download, file);
    }

    @Override
    public void onDirectorySelected(TreeNode dir) {
        dirSheet.expand(download, new AriaDirectory(dir, download.get()));
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
            DownloaderUtils.startDownload(downloaderMessenger,
                    file == null ?
                            DownloadStartConfig.create(getContext(), download.get(), profile.getProfile(getContext()), dir) :
                            DownloadStartConfig.create(getContext(), download.get(), profile.getProfile(getContext()), file));
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
            final ExternalPlayers.Player player = ExternalPlayers.supportedBy(mime);
            if (player != null && getContext() != null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle(R.string.couldStreamVideo)
                        .setMessage(R.string.couldStreamVideo_message)
                        .setNeutralButton(android.R.string.cancel, null)
                        .setPositiveButton(R.string.stream, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                shouldStream(profile, file, player);
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

        shouldDownload(profile, file);
    }

    private void shouldStream(MultiProfile profile, AriaFile file, ExternalPlayers.Player player) {
        if (getContext() == null) return;

        MultiProfile.DirectDownload dd = profile.getProfile(getContext()).directDownload;
        if (dd == null) throw new IllegalStateException("WTF?!");

        HttpUrl base = dd.getUrl();
        if (base == null) {
            Toaster.show(getActivity(), Utils.Messages.FAILED_STREAM_VIDEO, new NullPointerException("DirectDownload url is null!"));
            return;
        }

        HttpUrl url = file.getDownloadUrl(base);
        // TODO: Stream video

        ExternalPlayers.play(getContext(), player);
        AnalyticsApplication.sendAnalytics(getContext(), Utils.ACTION_PLAY_VIDEO);
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
        Toaster.show(getActivity(), message);
        if (dirSheet != null)
            dirSheet.collapse(); // We don't need to do this for dirSheet too, it would be redundant
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

    @Nullable
    @Override
    protected Download getDownload(@NonNull Bundle args) {
        return (Download) args.getSerializable("download");
    }

    @Nullable
    @Override
    protected BaseUpdater createUpdater(@NonNull Download download) {
        try {
            return new UpdateUI(getContext(), download, FilesFragment.this);
        } catch (Aria2Helper.InitializingException ex) {
            recyclerViewLayout.showMessage(R.string.failedLoading, true);
            Logging.log(ex);
            return null;
        }
    }

    @Override
    public void onUpdateUi(List<AriaFile> files) {
        if (files.isEmpty() || files.get(0).path.isEmpty()) {
            recyclerViewLayout.showMessage(R.string.noFiles, false);
        } else {
            recyclerViewLayout.showList();
            if (adapter != null) adapter.update(download.get(), files);
            if (fileSheet != null) fileSheet.update(files);
            if (dirSheet != null) dirSheet.update(download, files);
            if (adapter != null) showTutorial(adapter.getCurrentNode());
        }
    }

    private interface IWaitBinder {
        void onBound();
    }
}
