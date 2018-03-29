package com.gianlu.aria2app.NetIO.Aria2;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.View;

import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.AriaRequests;
import com.gianlu.aria2app.NetIO.HttpClient;
import com.gianlu.aria2app.NetIO.WebSocketClient;
import com.gianlu.aria2app.PKeys;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.Preferences.Prefs;
import com.gianlu.commonutils.Toaster;

import java.util.ArrayList;
import java.util.List;

public class Aria2Helper {
    private final AbstractClient client;

    public Aria2Helper(Context context, @NonNull AbstractClient client) {
        this.client = client;
        Prefs.getBoolean(context, PKeys.A2_FORCE_ACTION, true); // FIXME
    }

    @NonNull
    public static AbstractClient getClient(Context context) throws AbstractClient.InitializationException, ProfilesManager.NoCurrentProfileException {
        MultiProfile.UserProfile profile = ProfilesManager.get(context).getCurrentSpecific();
        if (profile.connectionMethod == MultiProfile.ConnectionMethod.WEBSOCKET)
            return WebSocketClient.instantiate(context);
        else
            return HttpClient.instantiate(context);
    }

    @NonNull
    public static Aria2Helper instantiate(Context context) throws InitializingException {
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
        client.batch(new AbstractClient.BatchSandbox<VersionAndSession>() {
            @Override
            public VersionAndSession sandbox(AbstractClient client) throws Exception {
                return new VersionAndSession(client.sendSync(AriaRequests.getVersion()), client.sendSync(AriaRequests.getSessionInfo()));
            }
        }, listener);
    }

    public void tellAllAndGlobalStats(final boolean ignoreMetadata, final AbstractClient.OnResult<DownloadsAndGlobalStats> listener) {
        client.batch(new AbstractClient.BatchSandbox<DownloadsAndGlobalStats>() {

            @Override
            public DownloadsAndGlobalStats sandbox(AbstractClient client) throws Exception {
                List<Download> all = new ArrayList<>();
                all.addAll(client.sendSync(AriaRequests.tellActive()));
                all.addAll(client.sendSync(AriaRequests.tellWaiting(0, Integer.MAX_VALUE)));
                all.addAll(client.sendSync(AriaRequests.tellStopped(0, Integer.MAX_VALUE)));
                return new DownloadsAndGlobalStats(all, ignoreMetadata, client.sendSync(AriaRequests.getGlobalStats()));
            }
        }, listener);
    }

    @NonNull
    public AbstractClient getClient() {
        return client;
    }

    /*
    private void handleSuccessRequestt(JSONObject request, final ISuccess listener) {
        if (response.has("error")) // FIXME
            listener.onException(new AriaException(response.getJSONObject("error")));
        else if (Objects.equals(response.optString("result"), "OK"))
            listener.onSuccess();
        else
            listener.onException(new AriaException(response.toString(), -1));
    }
    */

    public enum WhatAction {
        REMOVE, RESTART, RESUME, PAUSE, MOVE_UP, STOP, MOVE_DOWN
    }

    public static class DownloadActionClick implements View.OnClickListener, AbstractClient.OnSuccess, AbstractClient.OnResult<DownloadWithHelper.RemoveResult> {
        private final DownloadWithHelper download;
        private final WhatAction what;
        private final Listener listener;

        public DownloadActionClick(DownloadWithHelper download, WhatAction what, Listener listener) {
            this.download = download;
            this.what = what;
            this.listener = listener;
        }

        private void remove(Context context) {
            if (download.get().following != null) {
                listener.showDialog(new AlertDialog.Builder(context)
                        .setTitle(context.getString(R.string.removeMetadataName, download.get().getName()))
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
            Toaster.Message msg;
            switch (what) {
                case RESTART:
                    msg = Utils.Messages.RESTARTED;
                    break;
                case RESUME:
                    msg = Utils.Messages.RESUMED;
                    break;
                case PAUSE:
                    msg = Utils.Messages.PAUSED;
                    break;
                case MOVE_DOWN:
                case MOVE_UP:
                    msg = Utils.Messages.MOVED;
                    break;
                case STOP: // Not called here
                case REMOVE: // Not called here
                default:
                    msg = Utils.Messages.FAILED_PERFORMING_ACTION;
                    break;
            }

            listener.showToast(msg, download.gid());
        }

        @Override
        public void onResult(DownloadWithHelper.RemoveResult result) {
            Toaster.Message msg;
            switch (result) {
                case REMOVED:
                    msg = Utils.Messages.REMOVED;
                    break;
                case REMOVED_RESULT:
                case REMOVED_RESULT_AND_METADATA:
                    msg = Utils.Messages.RESULT_REMOVED;
                    break;
                default:
                    msg = Utils.Messages.FAILED_PERFORMING_ACTION;
                    break;
            }

            listener.showToast(msg, download.gid());
        }

        @Override
        public void onException(Exception ex) {
            listener.showToast(Utils.Messages.FAILED_PERFORMING_ACTION, ex);
        }

        public interface Listener {
            void showDialog(AlertDialog.Builder builder);

            void showToast(Toaster.Message msg, Exception ex);

            void showToast(Toaster.Message msg, String extra);
        }
    }

    public static class InitializingException extends Exception {
        public InitializingException(Throwable cause) {
            super(cause);
        }
    }
}