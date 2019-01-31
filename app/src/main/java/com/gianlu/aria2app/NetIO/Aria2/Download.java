package com.gianlu.aria2app.NetIO.Aria2;

import android.content.Context;

import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.NetIO.AriaRequests;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;

import org.json.JSONException;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

public class Download {
    public final String gid;
    protected final AbstractClient client;

    public Download(String gid, @NonNull AbstractClient client) {
        this.gid = gid;
        this.client = client;
    }

    @WorkerThread
    @NonNull
    private static ChangeSelectionResult performSelectIndexesOperation(AbstractClient client, String gid, Integer[] currIndexes, Integer[] selIndexes, boolean select) throws Exception {
        Collection<Integer> newIndexes = new HashSet<>(Arrays.asList(currIndexes));
        if (select) {
            newIndexes.addAll(Arrays.asList(selIndexes)); // Does not allow duplicates
        } else {
            newIndexes.removeAll(Arrays.asList(selIndexes));
            if (newIndexes.isEmpty()) return ChangeSelectionResult.EMPTY;
        }

        OptionsMap map = new OptionsMap();
        map.put("select-file", CommonUtils.join(newIndexes, ","));
        client.sendSync(AriaRequests.changeDownloadOptions(gid, map));
        if (select) return ChangeSelectionResult.SELECTED;
        else return ChangeSelectionResult.DESELECTED;
    }

    public void restart(final AbstractClient.OnSuccess listener) {
        restart(new AbstractClient.OnResult<String>() {
            @Override
            public void onResult(@NonNull String result) {
                listener.onSuccess();
            }

            @Override
            public void onException(@NonNull Exception ex) {
                listener.onException(ex);
            }
        });
    }

    public void moveUp(AbstractClient.OnSuccess listener) {
        moveRelative(-1, listener);
    }

    public void moveDown(AbstractClient.OnSuccess listener) {
        moveRelative(1, listener);
    }

    public final void options(AbstractClient.OnResult<OptionsMap> listener) {
        client.send(AriaRequests.getDownloadOptions(gid), listener);
    }

    public final void servers(AbstractClient.OnResult<SparseServers> listener) {
        client.send(AriaRequests.getServers(gid), listener);
    }

    public final void files(AbstractClient.OnResult<AriaFiles> listener) {
        client.send(AriaRequests.getFiles(gid), listener);
    }

    public final void peers(AbstractClient.OnResult<Peers> listener) {
        client.send(AriaRequests.getPeers(gid), listener);
    }

    public final void changePosition(int pos, String mode, AbstractClient.OnSuccess listener) {
        client.send(AriaRequests.changePosition(gid, pos, mode), listener);
    }

    public final void changeOptions(OptionsMap options, AbstractClient.OnSuccess listener) throws JSONException {
        client.send(AriaRequests.changeDownloadOptions(gid, options), listener);
    }

    public final void pause(final AbstractClient.OnSuccess listener) {
        client.send(AriaRequests.pause(gid), listener);
    }

    public final void unpause(AbstractClient.OnSuccess listener) {
        client.send(AriaRequests.unpause(gid), listener);
    }

    public final void moveRelative(int relative, AbstractClient.OnSuccess listener) {
        client.send(AriaRequests.changePosition(gid, relative, "POS_CUR"), listener);
    }

    public final void restart(AbstractClient.OnResult<String> listener) {
        client.batch(client -> {
            DownloadWithUpdate old = client.sendSync(AriaRequests.tellStatus(gid));
            OptionsMap oldOptions = client.sendSync(AriaRequests.getDownloadOptions(gid));

            Set<String> newUrls = new HashSet<>();
            for (AriaFile file : old.update().files)
                newUrls.addAll(file.uris.findByStatus(AriaFile.Status.USED));

            String newGid = client.sendSync(AriaRequests.addUri(newUrls, null, oldOptions));
            client.sendSync(AriaRequests.removeDownloadResult(gid));
            return newGid;
        }, listener);
    }

    public final void remove(final boolean removeMetadata, AbstractClient.OnResult<RemoveResult> listener) {
        client.batch(client -> {
            DownloadWithUpdate.SmallUpdate last = client.sendSync(AriaRequests.tellStatus(gid)).update();
            if (last.status == Status.COMPLETE || last.status == Status.ERROR || last.status == Status.REMOVED) {
                client.sendSync(AriaRequests.removeDownloadResult(gid));
                if (removeMetadata) {
                    client.sendSync(AriaRequests.removeDownloadResult(last.following));
                    return RemoveResult.REMOVED_RESULT_AND_METADATA;
                } else {
                    return RemoveResult.REMOVED_RESULT;
                }
            } else {
                client.sendSync(AriaRequests.remove(gid));
                return RemoveResult.REMOVED;
            }
        }, listener);
    }

    public final void changeSelection(final Integer[] selIndexes, final boolean select, AbstractClient.OnResult<ChangeSelectionResult> listener) {
        client.batch(client -> {
            OptionsMap options = client.sendSync(AriaRequests.getDownloadOptions(gid));
            OptionsMap.OptionValue currIndexes = options.get("select-file");
            if (currIndexes == null)
                return performSelectIndexesOperation(client, gid, client.sendSync(AriaRequests.getFileIndexes(gid)), selIndexes, select);
            else
                return performSelectIndexesOperation(client, gid, CommonUtils.toIntsList(currIndexes.string(), ","), selIndexes, select);
        }, listener);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Download download = (Download) o;
        return Objects.equals(gid, download.gid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gid);
    }

    public enum ChangeSelectionResult {
        EMPTY,
        SELECTED,
        DESELECTED
    }

    public enum RemoveResult {
        REMOVED,
        REMOVED_RESULT,
        REMOVED_RESULT_AND_METADATA
    }


    public enum Status {
        ACTIVE, PAUSED, WAITING, ERROR, REMOVED, COMPLETE;

        @NonNull
        public static Status parse(@NonNull String val) throws ParseException {
            switch (val.toLowerCase()) {
                case "active":
                    return Status.ACTIVE;
                case "paused":
                    return Status.PAUSED;
                case "waiting":
                    return Status.WAITING;
                case "complete":
                    return Status.COMPLETE;
                case "error":
                    return Status.ERROR;
                case "removed":
                    return Status.REMOVED;
                default:
                    throw new ParseException(val, 0);
            }
        }

        @NonNull
        public static List<String> stringValues() {
            List<String> values = new ArrayList<>();
            for (Status value : values()) values.add(value.name());
            return values;
        }

        @NonNull
        public String getFormal(Context context, boolean firstCapital) {
            String val;
            switch (this) {
                case ACTIVE:
                    val = context.getString(R.string.downloadStatus_active);
                    break;
                case PAUSED:
                    val = context.getString(R.string.downloadStatus_paused);
                    break;
                case REMOVED:
                    val = context.getString(R.string.downloadStatus_removed);
                    break;
                case WAITING:
                    val = context.getString(R.string.downloadStatus_waiting);
                    break;
                case ERROR:
                    val = context.getString(R.string.downloadStatus_error);
                    break;
                case COMPLETE:
                    val = context.getString(R.string.downloadStatus_complete);
                    break;
                default:
                    val = context.getString(R.string.downloadStatus_unknown);
                    break;
            }

            if (firstCapital) return val;
            else return Character.toLowerCase(val.charAt(0)) + val.substring(1);
        }
    }
}
