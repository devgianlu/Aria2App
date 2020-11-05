package com.gianlu.aria2app.api.aria2;


import android.content.Context;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;

import com.gianlu.aria2app.PK;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.activities.adddownload.AddDownloadBundle;
import com.gianlu.aria2app.api.AbstractClient;
import com.gianlu.aria2app.api.AriaRequests;
import com.gianlu.aria2app.api.ClientInterface;
import com.gianlu.aria2app.api.NetInstanceHolder;
import com.gianlu.aria2app.profiles.ProfilesManager;
import com.gianlu.aria2lib.Aria2PK;
import com.gianlu.commonutils.dialogs.DialogUtils;
import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.commonutils.preferences.json.JsonStoring;
import com.gianlu.commonutils.ui.Toaster;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class Aria2Helper {
    private static final AbstractClient.BatchSandbox<VersionAndSession> VERSION_AND_SESSION_BATCH_SANDBOX = client -> new VersionAndSession(client.sendSync(AriaRequests.getVersion()), client.sendSync(AriaRequests.getSessionInfo()));
    private static final String TAG = Aria2Helper.class.getSimpleName();
    private final ClientInterface client;
    private final AbstractClient.BatchSandbox<DownloadsAndGlobalStats> DOWNLOADS_AND_GLOBAL_STATS_BATCH_SANDBOX = client -> {
        List<DownloadWithUpdate> all = new ArrayList<>();
        all.addAll(client.sendSync(AriaRequests.tellActiveSmall()));
        all.addAll(client.sendSync(AriaRequests.tellWaitingSmall(0, Integer.MAX_VALUE)));
        all.addAll(client.sendSync(AriaRequests.tellStoppedSmall(0, Integer.MAX_VALUE)));
        return new DownloadsAndGlobalStats(all, Prefs.getBoolean(PK.A2_HIDE_METADATA), client.sendSync(AriaRequests.getGlobalStats()));
    };

    public Aria2Helper(@NonNull ClientInterface client) {
        this.client = client;
    }

    @NonNull
    private static ClientInterface getClient(@NonNull Context context) throws AbstractClient.InitializationException, ProfilesManager.NoCurrentProfileException {
        return NetInstanceHolder.instantiate(ProfilesManager.get(context).getCurrentSpecific());
    }

    @NonNull
    public static Aria2Helper instantiate(@NonNull Context context) throws InitializingException {
        try {
            return new Aria2Helper(getClient(context));
        } catch (AbstractClient.InitializationException | ProfilesManager.NoCurrentProfileException ex) {
            throw new InitializingException(ex);
        }
    }

    private void processGlobalOptionsUpdate(@NonNull JSONObject newOptions) {
        if (!client.isInAppDownloader()) return;

        try {
            JSONObject options = JsonStoring.intoPrefs().getJsonObject(Aria2PK.CUSTOM_OPTIONS);
            if (options == null) options = new JSONObject();

            Iterator<String> iter = newOptions.keys();
            while (iter.hasNext()) {
                String key = iter.next();
                options.put(key, newOptions.get(key));
            }

            JsonStoring.intoPrefs().putJsonObject(Aria2PK.CUSTOM_OPTIONS, options);
        } catch (JSONException ex) {
            Log.e(TAG, "Failed saving In-App downloader options.", ex);
        }
    }

    public final <T> void request(AbstractClient.AriaRequestWithResult<T> request, AbstractClient.OnResult<T> listener) {
        client.send(request, listener);

        if (request.method == AbstractClient.Method.CHANGE_GLOBAL_OPTIONS)
            processGlobalOptionsUpdate((JSONObject) request.params[0]);
    }

    public final void request(AbstractClient.AriaRequest request, AbstractClient.OnSuccess listener) {
        client.send(request, listener);

        if (request.method == AbstractClient.Method.CHANGE_GLOBAL_OPTIONS)
            processGlobalOptionsUpdate((JSONObject) request.params[0]);
    }

    public void getVersionAndSession(AbstractClient.OnResult<VersionAndSession> listener) {
        client.batch(VERSION_AND_SESSION_BATCH_SANDBOX, listener);
    }

    public void tellAllAndGlobalStats(AbstractClient.OnResult<DownloadsAndGlobalStats> listener) {
        client.batch(DOWNLOADS_AND_GLOBAL_STATS_BATCH_SANDBOX, listener);
    }

    public void getServersAndFiles(final String gid, AbstractClient.OnResult<SparseServersWithFiles> listener) {
        client.batch(client -> {
            SparseServers servers = client.sendSync(AriaRequests.getServers(gid));
            AriaFiles files = client.sendSync(AriaRequests.getFiles(gid));
            return new SparseServersWithFiles(servers, files);
        }, listener);
    }

    public void addDownloads(@NonNull final Collection<AddDownloadBundle> bundles, AbstractClient.OnResult<List<String>> listener) {
        client.batch(client -> {
            List<String> results = new ArrayList<>(bundles.size());
            for (AddDownloadBundle bundle : bundles)
                results.add(client.sendSync(AriaRequests.addDownload(bundle)));
            return results;
        }, listener);
    }

    public enum WhatAction {
        REMOVE, RESTART, RESUME, PAUSE, MOVE_UP, STOP, MOVE_DOWN
    }

    public static class DownloadActionClick implements View.OnClickListener, AbstractClient.OnSuccess, AbstractClient.OnResult<Download.RemoveResult> {
        private final DownloadWithUpdate download;
        private final WhatAction what;
        private final DialogUtils.ShowStuffInterface listener;

        public DownloadActionClick(DownloadWithUpdate download, WhatAction what, DialogUtils.ShowStuffInterface listener) {
            this.download = download;
            this.what = what;
            this.listener = listener;
        }

        private void remove(Context context) {
            DownloadWithUpdate.SmallUpdate update = download.update();
            if (update.following != null) {
                listener.showDialog(new MaterialAlertDialogBuilder(context)
                        .setTitle(context.getString(R.string.removeMetadataName, update.getName()))
                        .setMessage(R.string.removeDownload_removeMetadata)
                        .setNegativeButton(android.R.string.no, (dialog, which) -> download.remove(false, DownloadActionClick.this))
                        .setPositiveButton(android.R.string.yes, (dialog, which) -> download.remove(true, DownloadActionClick.this)));
            } else {
                download.remove(false, this);
            }
        }

        @Override
        public void onClick(View v) {
            switch (what) {
                case REMOVE:
                    remove(v.getContext());
                    break;
                case STOP:
                    download.remove(false, this);
                    break;
                case RESTART:
                    download.restart(this);
                    break;
                case RESUME:
                    download.unpause(this);
                    break;
                case PAUSE:
                    download.pause(this);
                    break;
                case MOVE_UP:
                    download.moveUp(this);
                    break;
                case MOVE_DOWN:
                    download.moveDown(this);
                    break;
            }
        }

        @Override
        public void onSuccess() {
            Toaster toaster = Toaster.build();
            toaster.extra(download.gid);
            switch (what) {
                case RESTART:
                    toaster.message(R.string.downloadRestarted);
                    break;
                case RESUME:
                    toaster.message(R.string.downloadResumed);
                    break;
                case PAUSE:
                    toaster.message(R.string.downloadPaused);
                    break;
                case MOVE_DOWN:
                case MOVE_UP:
                    toaster.message(R.string.downloadMoved);
                    break;
                case STOP: // Not called here
                case REMOVE: // Not called here
                default:
                    toaster.message(R.string.failedAction);
                    break;
            }

            listener.showToast(toaster);
        }

        @Override
        public void onResult(@NonNull Download.RemoveResult result) {
            Toaster toaster = Toaster.build();
            toaster.extra(download.gid);
            switch (result) {
                case REMOVED:
                    toaster.message(R.string.downloadRemoved);
                    break;
                case REMOVED_RESULT:
                case REMOVED_RESULT_AND_METADATA:
                    toaster.message(R.string.downloadResultRemoved);
                    break;
                default:
                    toaster.message(R.string.failedAction);
                    break;
            }

            listener.showToast(toaster);
        }

        @Override
        public void onException(@NonNull Exception ex) {
            Log.e(TAG, "Failed performing action.", ex);
            listener.showToast(Toaster.build().message(R.string.failedAction));
        }
    }

    public static class InitializingException extends Exception {
        public InitializingException(Throwable cause) {
            super(cause);
        }
    }
}