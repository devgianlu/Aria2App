package com.gianlu.aria2app.activities.moreabout.files;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.aria2app.PK;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.aria2app.activities.DirectDownloadActivity;
import com.gianlu.aria2app.activities.moreabout.BigUpdateProvider;
import com.gianlu.aria2app.activities.moreabout.OnBackPressed;
import com.gianlu.aria2app.api.AbstractClient;
import com.gianlu.aria2app.api.AriaRequests;
import com.gianlu.aria2app.api.aria2.Aria2Helper;
import com.gianlu.aria2app.api.aria2.AriaDirectory;
import com.gianlu.aria2app.api.aria2.AriaFile;
import com.gianlu.aria2app.api.aria2.AriaFiles;
import com.gianlu.aria2app.api.aria2.Download;
import com.gianlu.aria2app.api.aria2.DownloadWithUpdate;
import com.gianlu.aria2app.api.aria2.OptionsMap;
import com.gianlu.aria2app.api.updater.PayloadProvider;
import com.gianlu.aria2app.api.updater.UpdaterFragment;
import com.gianlu.aria2app.api.updater.Wants;
import com.gianlu.aria2app.downloader.DirectDownloadHelper;
import com.gianlu.aria2app.tutorial.Discovery;
import com.gianlu.aria2app.tutorial.FilesTutorial;
import com.gianlu.aria2app.tutorial.FoldersTutorial;
import com.gianlu.commonutils.analytics.AnalyticsApplication;
import com.gianlu.commonutils.dialogs.DialogUtils;
import com.gianlu.commonutils.misc.BreadcrumbsView;
import com.gianlu.commonutils.misc.RecyclerMessageView;
import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.commonutils.tutorial.BaseTutorial;
import com.gianlu.commonutils.tutorial.TutorialManager;
import com.gianlu.commonutils.ui.Toaster;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class FilesFragment extends UpdaterFragment<DownloadWithUpdate.BigUpdate> implements TutorialManager.Listener, FilesAdapter.Listener, OnBackPressed, FileSheet.Listener, DirectorySheet.Listener, BreadcrumbsView.Listener {
    private static final String TAG = FilesFragment.class.getSimpleName();
    private FilesAdapter adapter;
    private FileSheet fileSheet;
    private DirectorySheet dirSheet;
    private BreadcrumbsView breadcrumbs;
    private RecyclerMessageView rmv;
    private ActionMode actionMode = null;
    private DownloadWithUpdate download;
    private TutorialManager tutorialManager;
    private DirectDownloadHelper helper;

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
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            helper = DirectDownloadHelper.get(context);
        } catch (DirectDownloadHelper.DirectDownloadNotEnabledException ignored) {
        } catch (DirectDownloadHelper.InitializationException ex) {
            Log.e(TAG, "Failed initializing DirectDownload helper.", ex);
            showToast(Toaster.build().message(R.string.failedLoading_reason, ex.getMessage()));
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle savedInstanceState) {
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.fragment_files, parent, false);
        breadcrumbs = layout.findViewById(R.id.filesFragment_breadcrumbs);
        breadcrumbs.setListener(this);
        rmv = layout.findViewById(R.id.filesFragment_rmv);
        rmv.linearLayoutManager(LinearLayoutManager.VERTICAL, false);
        rmv.dividerDecoration(RecyclerView.VERTICAL);

        tutorialManager = new TutorialManager(this, Discovery.FILES, Discovery.FOLDERS);

        rmv.startLoading();

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
                Log.e(TAG, "Failed changing selection", ex);
                showToast(Toaster.build().message(R.string.failedFileChangeSelection));
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

    private void startDownloadInternal(@Nullable AriaFile file, @Nullable AriaDirectory dir) {
        if (helper == null) return;

        boolean single = file != null;
        DirectDownloadHelper.StartListener listener = new DirectDownloadHelper.StartListener() {
            @Override
            public void onSuccess() {
                Snackbar.make(rmv, R.string.downloadAdded, Snackbar.LENGTH_LONG)
                        .setAction(R.string.show, v -> startActivity(new Intent(getContext(), DirectDownloadActivity.class)))
                        .show();
            }

            @Override
            public void onFailed(@NonNull Throwable ex) {
                Log.e(TAG, "Failed starting download.", ex);
                DialogUtils.showToast(getContext(), Toaster.build().message(single ? R.string.failedAddingDownload : R.string.failedAddingDownloads));
            }
        };

        if (file != null) helper.start(requireContext(), file, listener);
        else if (dir != null) helper.start(requireContext(), dir, listener);
    }

    @Override
    public void onDownloadFile(@NonNull AriaFile file, boolean share) {
        if (fileSheet != null) {
            fileSheet.dismiss();
            fileSheet = null;
            dismissDialog();
        }

        String mime = file.getMimeType();
        if (getHelper().isInAppDownloader()) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(file.getAbsolutePath()));
            if (mime != null) intent.setType(mime);
            startActivity(Intent.createChooser(intent, "Open the file..."));
            return;
        }

        showProgress(R.string.gathering_information);
        getHelper().request(AriaRequests.getGlobalOptions(), new AbstractClient.OnResult<OptionsMap>() {
            @Override
            public void onResult(@NonNull OptionsMap result) {
                dismissDialog();

                if (mime != null && getContext() != null && helper.canStreamHttp(mime)) {
                    Intent intent = helper.getStreamIntent(result, file);
                    if (intent != null && Utils.canHandleIntent(requireContext(), intent)) {
                        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(requireContext());
                        builder.setTitle(R.string.couldStreamVideo)
                                .setMessage(R.string.couldStreamVideo_message)
                                .setNeutralButton(android.R.string.cancel, null)
                                .setPositiveButton(R.string.stream, (dialog, which) -> {
                                    startActivity(intent);
                                    AnalyticsApplication.sendAnalytics(Utils.ACTION_PLAY_VIDEO);
                                })
                                .setNegativeButton(R.string.download, (dialog, which) -> shouldDownload(result, file, share));

                        showDialog(builder);
                        return;
                    }
                }

                shouldDownload(result, file, share);
            }

            @Override
            public void onException(@NonNull Exception ex) {
                dismissDialog();
                Log.e(TAG, "Failed getting global options.", ex);
                showToast(Toaster.build().message(R.string.failedDownloadingFile));
            }
        });
    }

    private void shouldDownload(@NonNull OptionsMap global, @NonNull AriaFile file, boolean share) {
        AnalyticsApplication.sendAnalytics(Utils.ACTION_DOWNLOAD_FILE);

        if (Prefs.getBoolean(PK.DD_USE_EXTERNAL) || share) {
            Intent intent = helper.getStreamIntent(global, file);
            if (intent != null) startActivity(intent);
        } else {
            startDownloadInternal(file, null);
        }
    }

    @Override
    public void onDownloadDirectory(@NonNull AriaDirectory dir) {
        if (dirSheet != null) {
            dirSheet.dismiss();
            dirSheet = null;
            dismissDialog();
        }

        AnalyticsApplication.sendAnalytics(Utils.ACTION_DOWNLOAD_DIRECTORY);

        if (Prefs.getBoolean(PK.DD_USE_EXTERNAL)) {
            AlertDialog.Builder builder = new MaterialAlertDialogBuilder(requireContext());
            builder.setTitle(R.string.cannotDownloadDirWithExternal)
                    .setMessage(R.string.cannotDownloadDirWithExternal_message)
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> startDownloadInternal(null, dir))
                    .setNegativeButton(android.R.string.no, null);

            showDialog(builder);
        } else {
            startDownloadInternal(null, dir);
        }
    }

    @Override
    public void onUpdateUi(@NonNull DownloadWithUpdate.BigUpdate payload) {
        AriaFiles files = payload.files;
        if (files.isEmpty() || files.get(0).path.isEmpty()) {
            rmv.showInfo(R.string.noFiles);
            breadcrumbs.setVisibility(View.GONE);
        } else {
            rmv.showList();
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

        adapter = new FilesAdapter(getContext(), this);
        rmv.loadListData(adapter);
        rmv.startLoading();

        rmv.enableSwipeRefresh(() -> {
            canGoBack(CODE_CLOSE_SHEET);

            refresh(() -> {
                if (!isAdded() || isDetached()) return;

                adapter = new FilesAdapter(getContext(), this);
                rmv.loadListData(adapter);
                rmv.startLoading();
            });
        }, R.color.colorAccent, R.color.colorMetalink, R.color.colorTorrent);
    }

    @Override
    public boolean onCouldntLoad(@NonNull Exception ex) {
        rmv.showError(R.string.failedLoading);
        Log.e(TAG, "Failed load info.", ex);
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
            return ((FilesTutorial) tutorial).buildSequence(rmv.list(), adapter != null ? adapter.getCurrentDir() : null);
        else if (tutorial instanceof FoldersTutorial)
            return ((FoldersTutorial) tutorial).buildSequence(rmv.list());
        return true;
    }
}
