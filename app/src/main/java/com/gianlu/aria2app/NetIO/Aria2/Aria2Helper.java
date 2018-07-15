package com.gianlu.aria2app.NetIO.Aria2;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.View;

import com.gianlu.aria2app.Activities.AddDownload.AddDownloadBundle;
import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.AriaRequests;
import com.gianlu.aria2app.NetIO.HttpClient;
import com.gianlu.aria2app.NetIO.WebSocketClient;
import com.gianlu.aria2app.PK;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.Preferences.Prefs;
import com.gianlu.commonutils.Toaster;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Aria2Helper {
    private static final AbstractClient.BatchSandbox<VersionAndSession> VERSION_AND_SESSION_BATCH_SANDBOX = new AbstractClient.BatchSandbox<VersionAndSession>() {
        @Override
        public VersionAndSession sandbox(AbstractClient client, boolean shouldForce) throws Exception {
            return new VersionAndSession(client.sendSync(AriaRequests.getVersion()), client.sendSync(AriaRequests.getSessionInfo()));
        }
    };
    private final AbstractClient client;
    private final SharedPreferences preferences;
    private final AbstractClient.BatchSandbox<DownloadsAndGlobalStats> DOWNLOADS_AND_GLOBAL_STATS_BATCH_SANDBOX = new AbstractClient.BatchSandbox<DownloadsAndGlobalStats>() {
        @Override
        public DownloadsAndGlobalStats sandbox(AbstractClient client, boolean shouldForce) throws Exception {
            List<DownloadWithUpdate> all = new ArrayList<>();
            all.addAll(client.sendSync(AriaRequests.tellActiveSmall()));
            all.addAll(client.sendSync(AriaRequests.tellWaitingSmall(0, Integer.MAX_VALUE)));
            all.addAll(client.sendSync(AriaRequests.tellStoppedSmall(0, Integer.MAX_VALUE)));
            return new DownloadsAndGlobalStats(all, Prefs.getBoolean(preferences, PK.A2_HIDE_METADATA, false), client.sendSync(AriaRequests.getGlobalStats()));
        }
    };

    public Aria2Helper(@NonNull Context context, @NonNull AbstractClient client) {
        this.client = client;
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @NonNull
    private static AbstractClient getClient(@NonNull Context context) throws AbstractClient.InitializationException, ProfilesManager.NoCurrentProfileException {
        MultiProfile.UserProfile profile = ProfilesManager.get(context).getCurrentSpecific();
        if (profile.connectionMethod == MultiProfile.ConnectionMethod.WEBSOCKET)
            return WebSocketClient.instantiate(context);
        else
            return HttpClient.instantiate(context);
    }

    @NonNull
    public static Aria2Helper instantiate(@NonNull Context context) throws InitializingException {
        try {
            return new Aria2Helper(context, getClient(context));
        } catch (AbstractClient.InitializationException | ProfilesManager.NoCurrentProfileException ex) {
            throw new InitializingException(ex);
        }
    }

    public final <T> void request(final AbstractClient.AriaRequestWithResult<T> request, final AbstractClient.OnResult<T> listener) {
        client.send(request, listener);
    }

    public final void request(final AbstractClient.AriaRequest request, final AbstractClient.OnSuccess listener) {
        client.send(request, listener);
    }

    public void getVersionAndSession(AbstractClient.OnResult<VersionAndSession> listener) {
        client.batch(VERSION_AND_SESSION_BATCH_SANDBOX, listener);
    }

    public void tellAllAndGlobalStats(final AbstractClient.OnResult<DownloadsAndGlobalStats> listener) {
        client.batch(DOWNLOADS_AND_GLOBAL_STATS_BATCH_SANDBOX, listener);
    }

    public void getServersAndFiles(final String gid, AbstractClient.OnResult<SparseServersWithFiles> listener) {
        client.batch(new AbstractClient.BatchSandbox<SparseServersWithFiles>() {
            @Override
            public SparseServersWithFiles sandbox(AbstractClient client, boolean shouldForce) throws Exception {
                SparseServers servers = client.sendSync(AriaRequests.getServers(gid));
                AriaFiles files = client.sendSync(AriaRequests.getFiles(gid));
                return new SparseServersWithFiles(servers, files);
            }
        }, listener);
    }

    public void addDownloads(@NonNull final Collection<AddDownloadBundle> bundles, AbstractClient.OnResult<List<String>> listener) {
        client.batch(new AbstractClient.BatchSandbox<List<String>>() {
            @Override
            public List<String> sandbox(AbstractClient client, boolean shouldForce) throws Exception {
                List<String> results = new ArrayList<>(bundles.size());
                for (AddDownloadBundle bundle : bundles)
                    results.add(client.sendSync(AriaRequests.addDownload(bundle)));
                return results;
            }
        }, listener);
    }

    @NonNull
    public AbstractClient getClient() {
        return client;
    }

    public enum WhatAction {
        REMOVE, RESTART, RESUME, PAUSE, MOVE_UP, STOP, MOVE_DOWN
    }

    public static class DownloadActionClick implements View.OnClickListener, AbstractClient.OnSuccess, AbstractClient.OnResult<Download.RemoveResult> {
        private final DownloadWithUpdate download;
        private final WhatAction what;
        private final Listener listener;

        public DownloadActionClick(DownloadWithUpdate download, WhatAction what, Listener listener) {
            this.download = download;
            this.what = what;
            this.listener = listener;
        }

        private void remove(Context context) {
            DownloadWithUpdate.SmallUpdate update = download.update();
            if (update.following != null) {
                listener.showDialog(new AlertDialog.Builder(context)
                        .setTitle(context.getString(R.string.removeMetadataName, update.getName()))
                        .setMessage(R.string.removeDownload_removeMetadata)
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                download.remove(false, DownloadActionClick.this);
                            }
                        })
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                download.remove(true, DownloadActionClick.this);
                            }
                        }));
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
                    toaster.message(R.string.failedAction).error(true);
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
                    toaster.message(R.string.failedAction).error(true);
                    break;
            }

            listener.showToast(toaster);
        }

        @Override
        public void onException(Exception ex, boolean shouldForce) {
            listener.showToast(Toaster.build().message(R.string.failedAction).ex(ex));
        }

        public interface Listener {
            void showDialog(@NonNull AlertDialog.Builder builder);

            void showToast(@NonNull Toaster toaster);
        }
    }

    public static class InitializingException extends Exception {
        public InitializingException(Throwable cause) {
            super(cause);
        }
    }
}