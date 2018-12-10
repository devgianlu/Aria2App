package com.gianlu.aria2app.Activities.MoreAboutDownload.Files;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.gianlu.aria2app.Activities.DirectDownloadActivity;
import com.gianlu.aria2app.Activities.MoreAboutDownload.BigUpdateProvider;
import com.gianlu.aria2app.Activities.MoreAboutDownload.OnBackPressed;
import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.Aria2.Aria2Helper;
import com.gianlu.aria2app.NetIO.Aria2.AriaDirectory;
import com.gianlu.aria2app.NetIO.Aria2.AriaFile;
import com.gianlu.aria2app.NetIO.Aria2.AriaFiles;
import com.gianlu.aria2app.NetIO.Aria2.Download;
import com.gianlu.aria2app.NetIO.Aria2.DownloadWithUpdate;
import com.gianlu.aria2app.NetIO.Downloader.FetchHelper;
import com.gianlu.aria2app.NetIO.Updater.PayloadProvider;
import com.gianlu.aria2app.NetIO.Updater.UpdaterFragment;
import com.gianlu.aria2app.NetIO.Updater.Wants;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Tutorial.Discovery;
import com.gianlu.aria2app.Tutorial.FilesTutorial;
import com.gianlu.aria2app.Tutorial.FoldersTutorial;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.Analytics.AnalyticsApplication;
import com.gianlu.commonutils.BreadcrumbsView;
import com.gianlu.commonutils.Dialogs.DialogUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.RecyclerViewLayout;
import com.gianlu.commonutils.SuppressingLinearLayoutManager;
import com.gianlu.commonutils.Toaster;
import com.gianlu.commonutils.Tutorial.BaseTutorial;
import com.gianlu.commonutils.Tutorial.TutorialManager;
import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

public class FilesFragment extends UpdaterFragment<DownloadWithUpdate.BigUpdate> implements TutorialManager.Listener, FilesAdapter.Listener, OnBackPressed, FileSheet.Listener, DirectorySheet.Listener, BreadcrumbsView.Listener {
    private FilesAdapter adapter;
    private FileSheet fileSheet;
    private DirectorySheet dirSheet;
    private BreadcrumbsView breadcrumbs;
    private RecyclerViewLayout recyclerViewLayout;
    private ActionMode actionMode = null;
    private DownloadWithUpdate download;
    private TutorialManager tutorialManager;
    private FetchHelper helper;

    @NonNull
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
            if (actionMode != null) actionMode.finish();
            if (fileSheet != null) {
                fileSheet.dismiss();
                fileSheet = null;
                dismissDialog();
            }
            return true;
        }

        if (actionMode != null) { // Unluckily ActionMode intercepts the event (useless condition)
            actionMode.finish();
            return false;
        } else if (hasVisibleDialog()) {
            dismissDialog();
            fileSheet = null;
            dirSheet = null;
            return false;
        } else if (adapter != null && adapter.canGoUp()) {
            adapter.navigateUp();
            return false;
        } else {
            return true;
        }
    }

    @NonNull
    @Override
    protected PayloadProvider<DownloadWithUpdate.BigUpdate> requireProvider(@NonNull Context context, @NonNull Bundle args) throws Aria2Helper.InitializingException {
        return new BigUpdateProvider(context, args.getString("gid"));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            helper = FetchHelper.get(context);
        } catch (FetchHelper.DirectDownloadNotEnabledException ignored) {
        } catch (FetchHelper.InitializationException ex) {
            Logging.log(ex);
            showToast(Toaster.build().message(R.string.failedLoading_reason, ex.getMessage()));
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle savedInstanceState) {
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.fragment_files, parent, false);
        breadcrumbs = layout.findViewById(R.id.filesFragment_breadcrumbs);
        breadcrumbs.setListener(this);
        recyclerViewLayout = layout.findViewById(R.id.filesFragment_recyclerViewLayout);
        recyclerViewLayout.setLayoutManager(new SuppressingLinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
        recyclerViewLayout.getList().addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));

        tutorialManager = new TutorialManager(this, Discovery.FILES, Discovery.FOLDERS);

        recyclerViewLayout.startLoading();

        return layout;
    }

    @Override
    public void onFileSelected(@NonNull AriaFile file) {
        fileSheet = FileSheet.get();
        fileSheet.show(getActivity(), download, file, this);
    }

    @Override
    public boolean onFileLongClick(@NonNull AriaFile file) {
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
                Toaster toaster = Toaster.build();
                toaster.extra(result);
                switch (result) {
                    case EMPTY:
                        toaster.message(R.string.cannotDeselectAllFiles);
                        break;
                    case SELECTED:
                        toaster.message(R.string.fileSelected);
                        break;
                    case DESELECTED:
                        toaster.message(R.string.fileDeselected);
                        break;
                    default:
                        toaster.message(R.string.failedAction);
                        break;
                }

                DialogUtils.showToast(getContext(), toaster);
                exitActionMode();
            }

            @Override
            public void onException(@NonNull @NotNull Exception ex) {
                showToast(Toaster.build().message(R.string.failedFileChangeSelection).ex(ex));
            }
        });
    }

    @Override
    public void exitActionMode() {
        if (actionMode != null) actionMode.finish();
    }

    @Override
    public boolean onDirectoryLongClick(@NonNull AriaDirectory dir) {
        dirSheet = DirectorySheet.get();
        dirSheet.show(getActivity(), download, dir, this);
        return true;
    }

    @Override
    public void onDirectoryChanged(@NonNull AriaDirectory dir) {
        breadcrumbs.clear();

        List<BreadcrumbsView.Item> items = new ArrayList<>();
        AriaDirectory current = dir;
        do {
            items.add(new BreadcrumbsView.Item(current.name, 0, current));
            current = current.parent;
        } while (current != null);

        Collections.reverse(items);
        breadcrumbs.addItems(items.toArray(new BreadcrumbsView.Item[0]));

        tutorialManager.tryShowingTutorials(getActivity());
    }

    private void startDownloadInternal(MultiProfile profile, @Nullable AriaFile file, @Nullable AriaDirectory dir) {
        if (helper == null) return;

        final boolean single = file != null;
        FetchHelper.StartListener listener = new FetchHelper.StartListener() {
            @Override
            public void onSuccess() {
                Snackbar.make(recyclerViewLayout, R.string.downloadAdded, Snackbar.LENGTH_LONG)
                        .setAction(R.string.show, v -> startActivity(new Intent(getContext(), DirectDownloadActivity.class)))
                        .show();
            }

            @Override
            public void onFailed(Throwable ex) {
                DialogUtils.showToast(getContext(),
                        Toaster.build()
                                .message(single ? R.string.failedAddingDownload : R.string.failedAddingDownloads)
                                .ex(ex));
            }
        };

        if (file != null) helper.start(profile, download, file, listener);
        else if (dir != null) helper.start(profile, download, dir, listener);
    }

    @Override
    public void onDownloadFile(@NonNull final MultiProfile profile, @NonNull final AriaFile file) {
        if (fileSheet != null) {
            fileSheet.dismiss();
            fileSheet = null;
            dismissDialog();
        }

        String mime = file.getMimeType();
        if (mime != null) {
            if (Utils.isStreamable(mime) && getContext() != null) {
                final Intent intent = Utils.getStreamIntent(profile.getProfile(getContext()), download, file);
                if (intent != null && Utils.canHandleIntent(getContext(), intent)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle(R.string.couldStreamVideo)
                            .setMessage(R.string.couldStreamVideo_message)
                            .setNeutralButton(android.R.string.cancel, null)
                            .setPositiveButton(R.string.stream, (dialog, which) -> {
                                startActivity(intent);
                                AnalyticsApplication.sendAnalytics(Utils.ACTION_PLAY_VIDEO);
                            })
                            .setNegativeButton(R.string.download, (dialog, which) -> shouldDownload(profile, file));

                    showDialog(builder);
                    return;
                }
            }
        }

        shouldDownload(profile, file);
    }

    private void shouldDownload(MultiProfile profile, AriaFile file) {
        startDownloadInternal(profile, file, null);
        AnalyticsApplication.sendAnalytics(Utils.ACTION_DOWNLOAD_FILE);
    }

    @Override
    public void onDownloadDirectory(@NonNull final MultiProfile profile, @NonNull final AriaDirectory dir) {
        if (dirSheet != null) {
            dirSheet.dismiss();
            dirSheet = null;
            dismissDialog();
        }

        startDownloadInternal(profile, null, dir);
        AnalyticsApplication.sendAnalytics(Utils.ACTION_DOWNLOAD_DIRECTORY);
    }

    @Override
    public void onUpdateUi(@NonNull DownloadWithUpdate.BigUpdate payload) {
        AriaFiles files = payload.files;
        if (files.isEmpty() || files.get(0).path.isEmpty()) {
            recyclerViewLayout.showInfo(R.string.noFiles);
            breadcrumbs.setVisibility(View.GONE);
        } else {
            recyclerViewLayout.showList();
            breadcrumbs.setVisibility(View.VISIBLE);
            if (adapter != null) adapter.update(payload.download(), files);
            if (fileSheet != null) fileSheet.update(files);
            if (dirSheet != null) dirSheet.update(payload.download(), files);

            tutorialManager.tryShowingTutorials(getActivity());
        }
    }

    @Override
    public void onLoadUi(@NonNull DownloadWithUpdate.BigUpdate payload) {
        this.download = payload.download();

        if (getContext() == null) return;

        adapter = new FilesAdapter(getContext(), FilesFragment.this);
        recyclerViewLayout.loadListData(adapter);
        recyclerViewLayout.startLoading();

        recyclerViewLayout.enableSwipeRefresh(() -> {
            canGoBack(CODE_CLOSE_SHEET);

            refresh(() -> {
                adapter = new FilesAdapter(getContext(), FilesFragment.this);
                recyclerViewLayout.loadListData(adapter);
                recyclerViewLayout.startLoading();
            });
        }, R.color.colorAccent, R.color.colorMetalink, R.color.colorTorrent);
    }

    @Override
    public boolean onCouldntLoad(@NonNull Exception ex) {
        recyclerViewLayout.showError(R.string.failedLoading);
        Logging.log(ex);
        return false;
    }

    @NonNull
    @Override
    public Wants<DownloadWithUpdate.BigUpdate> wants(@NonNull Bundle args) {
        return Wants.bigUpdate(args.getString("gid"));
    }

    @Override
    public void onSegmentSelected(@NonNull BreadcrumbsView.Item item) {
        if (adapter != null && item.data != null) adapter.changeDir((AriaDirectory) item.data);
    }

    @Override
    public boolean canShow(@NonNull BaseTutorial tutorial) {
        if (tutorial instanceof FilesTutorial)
            return ((FilesTutorial) tutorial).canShow(this, adapter);
        else if (tutorial instanceof FoldersTutorial)
            return ((FoldersTutorial) tutorial).canShow(this, adapter);

        return false;
    }

    @Override
    public boolean buildSequence(@NonNull BaseTutorial tutorial) {
        if (tutorial instanceof FilesTutorial)
            return ((FilesTutorial) tutorial).buildSequence(recyclerViewLayout.getList(), adapter != null ? adapter.getCurrentDir() : null);
        else if (tutorial instanceof FoldersTutorial)
            return ((FoldersTutorial) tutorial).buildSequence(recyclerViewLayout.getList());

        return true;
    }
}
