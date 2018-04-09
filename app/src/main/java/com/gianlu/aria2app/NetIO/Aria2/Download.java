package com.gianlu.aria2app.NetIO.Aria2;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gianlu.aria2app.NetIO.AbstractClient;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.Adapters.Filterable;
import com.gianlu.commonutils.Logging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class Download implements Serializable, Filterable<Download.Status> {
    public final String dir;
    public final String gid;
    public final int numPieces;
    public final long pieceLength;
    public final long length;
    public final BitTorrent torrent;

    private Download(JSONObject obj) throws JSONException {
        gid = obj.getString("gid");
        length = obj.getLong("totalLength");
        pieceLength = obj.getLong("pieceLength");
        numPieces = obj.getInt("numPieces");
        dir = obj.getString("dir");
        torrent = BitTorrent.create(obj);
    }

    public static Download create(JSONObject obj, boolean small) throws JSONException {
        Download download = new Download(obj);
        AbstractClient.update(download.gid, download.update(obj, small));
        return download;
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

    private SmallUpdate update(JSONObject obj, boolean small) throws JSONException {
        if (small) return new SmallUpdate(obj);
        else return new BigUpdate(obj);
    }

    public final boolean isTorrent() {
        return torrent != null;
    }

    @NonNull
    public SmallUpdate last() {
        return AbstractClient.update(gid);
    }

    @Nullable
    public BigUpdate lastBig() {
        SmallUpdate update = AbstractClient.update(gid);
        if (update instanceof BigUpdate) return (BigUpdate) update;
        else return null;
    }

    public DownloadWithHelper wrap(@NonNull AbstractClient client) {
        return new DownloadWithHelper(this, client);
    }

    public DownloadWithHelper wrap(@NonNull Context context) throws ProfilesManager.NoCurrentProfileException, AbstractClient.InitializationException {
        return wrap(Aria2Helper.getClient(context));
    }

    @Override
    @NonNull
    public Status getFilterable() {
        return last().status;
    }

    public enum Status {
        ACTIVE, PAUSED, WAITING, ERROR, REMOVED, COMPLETE, UNKNOWN;

        @NonNull
        public static Status parse(@Nullable String val) {
            if (val == null) return Status.UNKNOWN;
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
                    return Status.UNKNOWN;
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
                case UNKNOWN:
                default:
                    val = context.getString(R.string.downloadStatus_unknown);
                    break;
            }

            if (firstCapital) return val;
            else return Character.toLowerCase(val.charAt(0)) + val.substring(1);
        }
    }

    private abstract static class UpdateComparator implements Comparator<Download> {

        @Override
        public final int compare(Download o1, Download o2) {
            return compare(o1.last(), o2.last());
        }

        protected abstract int compare(SmallUpdate o1, SmallUpdate o2);
    }

    public static class StatusComparator extends UpdateComparator {
        @Override
        public int compare(SmallUpdate o1, SmallUpdate o2) {
            if (o1.status == o2.status) return 0;
            else if (o1.status.ordinal() < o2.status.ordinal()) return -1;
            else return 1;
        }
    }

    public static class DownloadSpeedComparator extends UpdateComparator {
        @Override
        public int compare(SmallUpdate o1, SmallUpdate o2) {
            if (Objects.equals(o1.downloadSpeed, o2.downloadSpeed)) return 0;
            else if (o1.downloadSpeed > o2.downloadSpeed) return -1;
            else return 1;
        }
    }

    public static class UploadSpeedComparator extends UpdateComparator {
        @Override
        public int compare(SmallUpdate o1, SmallUpdate o2) {
            if (Objects.equals(o1.uploadSpeed, o2.uploadSpeed)) return 0;
            else if (o1.uploadSpeed > o2.uploadSpeed) return -1;
            else return 1;
        }
    }

    public static class LengthComparator implements Comparator<Download> {
        @Override
        public int compare(Download o1, Download o2) {
            if (Objects.equals(o1.length, o2.length)) return 0;
            else if (o1.length > o2.length) return -1;
            else return 1;
        }
    }

    public static class NameComparator extends UpdateComparator {
        @Override
        public int compare(SmallUpdate o1, SmallUpdate o2) {
            return o1.getName().compareToIgnoreCase(o2.getName());
        }
    }

    public static class CompletedLengthComparator extends UpdateComparator {
        @Override
        public int compare(SmallUpdate o1, SmallUpdate o2) {
            if (Objects.equals(o1.completedLength, o2.completedLength)) return 0;
            else if (o1.completedLength > o2.completedLength) return -1;
            else return 1;
        }
    }

    public static class ProgressComparator extends UpdateComparator {
        @Override
        public int compare(SmallUpdate o1, SmallUpdate o2) {
            return Integer.compare((int) o2.getProgress(), (int) o1.getProgress());
        }
    }

    public class BigUpdate extends SmallUpdate {
        public final String bitfield;
        public final long verifiedLength;
        public final boolean verifyIntegrityPending;

        // BitTorrent only
        public final boolean seeder;
        public final String infoHash;

        BigUpdate(JSONObject obj) throws JSONException {
            super(obj);

            // Optional
            bitfield = obj.optString("bitfield", null);
            verifiedLength = obj.optLong("verifiedLength", 0);
            verifyIntegrityPending = obj.optBoolean("verifyIntegrityPending", false);

            if (isTorrent()) {
                infoHash = obj.getString("infoHash");
                seeder = obj.optBoolean("seeder", false);
            } else {
                seeder = false;
                infoHash = null;
            }
        }
    }

    public class SmallUpdate {
        public final long completedLength;
        public final long uploadLength;
        public final int connections;
        public final Download.Status status;
        public final int downloadSpeed;
        public final int uploadSpeed;
        public final ArrayList<AriaFile> files;
        public final int errorCode;
        public final String errorMessage;
        public final String followedBy;
        // BitTorrent only
        public final int numSeeders;
        public final String following;
        public final String belongsTo;
        private String name = null;

        SmallUpdate(JSONObject obj) throws JSONException {
            status = Download.Status.parse(obj.getString("status"));
            completedLength = obj.getLong("completedLength");
            uploadLength = obj.getLong("uploadLength");
            downloadSpeed = obj.getInt("downloadSpeed");
            uploadSpeed = obj.getInt("uploadSpeed");
            connections = obj.getInt("connections");

            // Optional
            followedBy = obj.optString("followedBy", null);
            following = obj.optString("following", null);
            belongsTo = obj.optString("belongsTo", null);


            files = new ArrayList<>();
            JSONArray array = obj.getJSONArray("files");
            for (int i = 0; i < array.length(); i++)
                files.add(new AriaFile(Download.this, array.getJSONObject(i)));

            if (isTorrent()) {
                numSeeders = obj.getInt("numSeeders");
            } else {
                numSeeders = 0;
            }

            if (obj.has("errorCode")) {
                errorCode = obj.getInt("errorCode");
                errorMessage = obj.getString("errorMessage");
            } else {
                errorCode = -1;
                errorMessage = null;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Download download = (Download) o;
            return gid.equals(download.gid);
        }

        public float shareRatio() {
            if (completedLength == 0) return 0f;
            return ((float) uploadLength) / ((float) completedLength);
        }

        @NonNull
        public String getName() {
            if (name == null) name = getNameInternal();
            return name;
        }

        public boolean isMetadata() {
            return getName().startsWith("[METADATA]");
        }

        @NonNull
        private String getNameInternal() {
            try {
                if (torrent != null && torrent.name != null) return torrent.name;
                String[] splitted = files.get(0).path.split("/");
                if (splitted.length >= 1) return splitted[splitted.length - 1];
            } catch (Exception ex) {
                Logging.log(ex);
            }

            return "Unknown";
        }

        public float getProgress() {
            return ((float) completedLength) / ((float) length) * 100;
        }

        public long getMissingTime() {
            if (downloadSpeed == 0) return 0;
            return (length - completedLength) / downloadSpeed;
        }

        public boolean canDeselectFiles() {
            return isTorrent() && files.size() > 1 && status != Status.REMOVED && status != Status.ERROR && status != Status.UNKNOWN;
        }
    }
}
