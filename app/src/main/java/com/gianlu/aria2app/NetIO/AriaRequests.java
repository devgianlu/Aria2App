package com.gianlu.aria2app.NetIO;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gianlu.aria2app.Activities.AddDownload.AddDownloadBundle;
import com.gianlu.aria2app.Activities.AddDownload.AddMetalinkBundle;
import com.gianlu.aria2app.Activities.AddDownload.AddTorrentBundle;
import com.gianlu.aria2app.Activities.AddDownload.AddUriBundle;
import com.gianlu.aria2app.NetIO.Aria2.AriaFiles;
import com.gianlu.aria2app.NetIO.Aria2.DownloadWithUpdate;
import com.gianlu.aria2app.NetIO.Aria2.GlobalStats;
import com.gianlu.aria2app.NetIO.Aria2.Peers;
import com.gianlu.aria2app.NetIO.Aria2.SessionInfo;
import com.gianlu.aria2app.NetIO.Aria2.SparseServers;
import com.gianlu.aria2app.NetIO.Aria2.VersionInfo;
import com.gianlu.aria2app.Options.OptionsUtils;
import com.gianlu.commonutils.CommonUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class AriaRequests {
    private static final AbstractClient.Processor<List<DownloadWithUpdate>> DOWNLOADS_LIST_PROCESSOR = new AbstractClient.Processor<List<DownloadWithUpdate>>() {
        @NonNull
        @Override
        public List<DownloadWithUpdate> process(AbstractClient client, JSONObject obj) throws JSONException {
            List<DownloadWithUpdate> list = new ArrayList<>();
            JSONArray array = obj.getJSONArray("result");
            for (int i = 0; i < array.length(); i++)
                list.add(DownloadWithUpdate.create(client, array.getJSONObject(i), true));
            return list;
        }
    };
    private static final AbstractClient.Processor<String> STRING_PROCESSOR = new AbstractClient.Processor<String>() {
        @NonNull
        @Override
        public String process(AbstractClient client, JSONObject obj) throws JSONException {
            return obj.getString("result");
        }
    };
    private static final JSONArray SMALL_KEYS = CommonUtils.toJSONArray(new String[]{
            "totalLength", "gid", "status", "pieceLength", "numPieces", "dir", "bittorrent",
            "completedLength", "downloadSpeed", "uploadSpeed", "connections", "uploadLength",
            "numSeeders", "files", "errorCode", "errorMessage"
    });

    public static AbstractClient.AriaRequest changePosition(String gid, int pos, String mode) {
        return new AbstractClient.AriaRequest(AbstractClient.Method.CHANGE_POSITION, gid, pos, mode);
    }

    public static AbstractClient.AriaRequestWithResult<SparseServers> getServers(String gid) {
        return new AbstractClient.AriaRequestWithResult<>(AbstractClient.Method.GET_SERVERS, new AbstractClient.Processor<SparseServers>() {
            @NonNull
            @Override
            public SparseServers process(AbstractClient client, JSONObject obj) throws JSONException {
                return new SparseServers(obj.getJSONArray("result"));
            }
        }, gid);
    }

    public static AbstractClient.AriaRequestWithResult<Peers> getPeers(String gid) {
        return new AbstractClient.AriaRequestWithResult<>(AbstractClient.Method.GET_PEERS, new AbstractClient.Processor<Peers>() {
            @NonNull
            @Override
            public Peers process(AbstractClient client, JSONObject obj) throws JSONException {
                return new Peers(obj.getJSONArray("result"));
            }
        }, gid);
    }

    public static AbstractClient.AriaRequestWithResult<AriaFiles> getFiles(String gid) {
        return new AbstractClient.AriaRequestWithResult<>(AbstractClient.Method.GET_FILES, new AbstractClient.Processor<AriaFiles>() {
            @NonNull
            @Override
            public AriaFiles process(AbstractClient client, JSONObject obj) throws JSONException {
                return new AriaFiles(obj.getJSONArray("result"));
            }
        }, gid);
    }

    public static AbstractClient.AriaRequestWithResult<Integer[]> getFileIndexes(final String gid) {
        return new AbstractClient.AriaRequestWithResult<>(AbstractClient.Method.GET_FILES, new AbstractClient.Processor<Integer[]>() {
            @NonNull
            @Override
            public Integer[] process(AbstractClient client, JSONObject obj) throws JSONException {
                JSONArray array = obj.getJSONArray("result");
                Integer[] indexes = new Integer[array.length()];
                for (int i = 0; i < array.length(); i++)
                    indexes[i] = Integer.parseInt(array.getJSONObject(i).getString("index"));
                return indexes;
            }
        }, gid);
    }

    public static AbstractClient.AriaRequestWithResult<String> addDownload(@NonNull AddDownloadBundle bundle) throws JSONException {
        if (bundle instanceof AddUriBundle)
            return addDownload((AddUriBundle) bundle);
        else if (bundle instanceof AddTorrentBundle)
            return addDownload((AddTorrentBundle) bundle);
        else if (bundle instanceof AddMetalinkBundle)
            return addDownload((AddMetalinkBundle) bundle);
        else
            throw new IllegalArgumentException("Unknown bundle: " + bundle);
    }

    private static AbstractClient.AriaRequestWithResult<String> addDownload(@NonNull AddTorrentBundle bundle) throws JSONException {
        return addTorrent(bundle.base64, bundle.uris, bundle.position, bundle.options);
    }

    private static AbstractClient.AriaRequestWithResult<String> addDownload(@NonNull AddUriBundle bundle) throws JSONException {
        return addUri(bundle.uris, bundle.position, bundle.options);
    }

    private static AbstractClient.AriaRequestWithResult<String> addDownload(@NonNull AddMetalinkBundle bundle) throws JSONException {
        return addMetalink(bundle.base64, bundle.position, bundle.options);
    }

    public static AbstractClient.AriaRequestWithResult<String> addUri(@NonNull Collection<String> uris, @Nullable Integer pos, @Nullable Map<String, String> options) throws JSONException {
        Object[] params = new Object[3];
        params[0] = CommonUtils.toJSONArray(uris, true);
        if (options != null) params[1] = OptionsUtils.toJson(options);
        else params[1] = new JSONObject();
        if (pos != null) params[2] = pos;
        else params[2] = Integer.MAX_VALUE;
        return new AbstractClient.AriaRequestWithResult<>(AbstractClient.Method.ADD_URI, STRING_PROCESSOR, params);
    }

    public static AbstractClient.AriaRequestWithResult<String> addTorrent(@NonNull String base64, @Nullable Collection<String> uris, @Nullable Integer pos, @Nullable Map<String, String> options) throws JSONException {
        Object[] params = new Object[4];
        params[0] = base64;
        if (uris != null) params[1] = CommonUtils.toJSONArray(uris, true);
        else params[1] = new JSONArray();
        if (options != null) params[2] = OptionsUtils.toJson(options);
        else params[2] = new JSONObject();
        if (pos != null) params[3] = pos;
        else params[3] = Integer.MAX_VALUE;
        return new AbstractClient.AriaRequestWithResult<>(AbstractClient.Method.ADD_TORRENT, STRING_PROCESSOR, params);
    }

    public static AbstractClient.AriaRequestWithResult<String> addMetalink(@NonNull String base64, @Nullable Integer pos, @Nullable Map<String, String> options) throws JSONException {
        Object[] params = new Object[3];
        params[0] = base64;
        if (options != null) params[1] = OptionsUtils.toJson(options);
        else params[1] = new JSONObject();
        if (pos != null) params[2] = pos;
        else params[2] = Integer.MAX_VALUE;
        return new AbstractClient.AriaRequestWithResult<>(AbstractClient.Method.ADD_METALINK, STRING_PROCESSOR, params);
    }

    public static AbstractClient.AriaRequest changeDownloadOptions(String gid, Map<String, String> options) throws JSONException {
        return new AbstractClient.AriaRequest(AbstractClient.Method.CHANGE_DOWNLOAD_OPTIONS, gid, OptionsUtils.toJson(options));
    }

    public static AbstractClient.AriaRequest changeGlobalOptions(Map<String, String> options) throws JSONException {
        return new AbstractClient.AriaRequest(AbstractClient.Method.CHANGE_GLOBAL_OPTIONS, OptionsUtils.toJson(options));
    }

    public static AbstractClient.AriaRequestWithResult<Map<String, String>> getGlobalOptions() {
        return new AbstractClient.AriaRequestWithResult<>(AbstractClient.Method.GET_GLOBAL_OPTIONS, new AbstractClient.Processor<Map<String, String>>() {
            @NonNull
            @Override
            public Map<String, String> process(AbstractClient client, JSONObject obj) throws JSONException {
                return CommonUtils.toMap(obj.getJSONObject("result"), String.class);
            }
        });
    }

    public static AbstractClient.AriaRequestWithResult<Map<String, String>> getDownloadOptions(@NonNull String gid) {
        return new AbstractClient.AriaRequestWithResult<>(AbstractClient.Method.GET_DOWNLOAD_OPTIONS, new AbstractClient.Processor<Map<String, String>>() {
            @NonNull
            @Override
            public Map<String, String> process(AbstractClient client, JSONObject obj) throws JSONException {
                return CommonUtils.toMap(obj.getJSONObject("result"), String.class);
            }
        }, gid);
    }

    public static AbstractClient.AriaRequestWithResult<GlobalStats> getGlobalStats() {
        return new AbstractClient.AriaRequestWithResult<>(AbstractClient.Method.GET_GLOBAL_STATS, new AbstractClient.Processor<GlobalStats>() {
            @NonNull
            @Override
            public GlobalStats process(AbstractClient client, JSONObject obj) throws JSONException {
                return new GlobalStats(obj.getJSONObject("result"));
            }
        });
    }

    public static AbstractClient.AriaRequestWithResult<List<String>> listMethods() {
        return new AbstractClient.AriaRequestWithResult<>(AbstractClient.Method.LIST_METHODS, new AbstractClient.Processor<List<String>>() {
            @NonNull
            @Override
            public List<String> process(AbstractClient client, JSONObject obj) throws JSONException {
                return CommonUtils.toStringsList(obj.getJSONArray("result"), false);
            }
        });
    }

    public static AbstractClient.AriaRequestWithResult<SessionInfo> getSessionInfo() {
        return new AbstractClient.AriaRequestWithResult<>(AbstractClient.Method.GET_SESSION_INFO, new AbstractClient.Processor<SessionInfo>() {
            @NonNull
            @Override
            public SessionInfo process(AbstractClient client, JSONObject obj) throws JSONException {
                return new SessionInfo(obj.getJSONObject("result"));
            }
        });
    }

    public static AbstractClient.AriaRequest saveSession() {
        return new AbstractClient.AriaRequest(AbstractClient.Method.SAVE_SESSION);
    }

    public static AbstractClient.AriaRequest purgeDownloadResults() {
        return new AbstractClient.AriaRequest(AbstractClient.Method.PURGE_DOWNLOAD_RESULTS);
    }

    public static AbstractClient.AriaRequest pauseAll() {
        return new AbstractClient.AriaRequest(AbstractClient.Method.PAUSE_ALL);
    }

    public static AbstractClient.AriaRequest unpauseAll() {
        return new AbstractClient.AriaRequest(AbstractClient.Method.UNPAUSE_ALL);
    }

    public static AbstractClient.AriaRequest forcePauseAll() {
        return new AbstractClient.AriaRequest(AbstractClient.Method.FORCE_PAUSE_ALL);
    }

    public static AbstractClient.AriaRequest forcePause(String gid) {
        return new AbstractClient.AriaRequest(AbstractClient.Method.FORCE_PAUSE, gid);
    }

    public static AbstractClient.AriaRequest forceRemove(String gid) {
        return new AbstractClient.AriaRequest(AbstractClient.Method.FORCE_REMOVE, gid);
    }

    public static AbstractClient.AriaRequest pause(String gid) {
        return new AbstractClient.AriaRequest(AbstractClient.Method.PAUSE, gid);
    }

    public static AbstractClient.AriaRequest unpause(String gid) {
        return new AbstractClient.AriaRequest(AbstractClient.Method.UNPAUSE, gid);
    }

    public static AbstractClient.AriaRequest remove(String gid) {
        return new AbstractClient.AriaRequest(AbstractClient.Method.REMOVE, gid);
    }

    public static AbstractClient.AriaRequest removeDownloadResult(String gid) {
        return new AbstractClient.AriaRequest(AbstractClient.Method.REMOVE_RESULT, gid);
    }

    public static AbstractClient.AriaRequestWithResult<DownloadWithUpdate> tellStatus(String gid) {
        return new AbstractClient.AriaRequestWithResult<>(AbstractClient.Method.TELL_STATUS, new AbstractClient.Processor<DownloadWithUpdate>() {
            @NonNull
            @Override
            public DownloadWithUpdate process(AbstractClient client, JSONObject obj) throws JSONException {
                return DownloadWithUpdate.create(client, obj.getJSONObject("result"), false);
            }
        }, gid);
    }

    public static AbstractClient.AriaRequestWithResult<DownloadWithUpdate> tellStatus(@NonNull final DownloadWithUpdate download) {
        return new AbstractClient.AriaRequestWithResult<>(AbstractClient.Method.TELL_STATUS, new AbstractClient.Processor<DownloadWithUpdate>() {
            @NonNull
            @Override
            public DownloadWithUpdate process(AbstractClient client, JSONObject obj) throws JSONException {
                return download.update(obj.getJSONObject("result"), false);
            }
        }, download.gid);
    }

    public static AbstractClient.AriaRequestWithResult<List<DownloadWithUpdate>> tellActiveSmall() {
        return new AbstractClient.AriaRequestWithResult<>(AbstractClient.Method.TELL_ACTIVE, DOWNLOADS_LIST_PROCESSOR, SMALL_KEYS);
    }

    public static AbstractClient.AriaRequestWithResult<List<DownloadWithUpdate>> tellWaitingSmall(int offset, int count) {
        return new AbstractClient.AriaRequestWithResult<>(AbstractClient.Method.TELL_WAITING, DOWNLOADS_LIST_PROCESSOR, offset, count, SMALL_KEYS);
    }

    public static AbstractClient.AriaRequestWithResult<List<DownloadWithUpdate>> tellStoppedSmall(int offset, int count) {
        return new AbstractClient.AriaRequestWithResult<>(AbstractClient.Method.TELL_STOPPED, DOWNLOADS_LIST_PROCESSOR, offset, count, SMALL_KEYS);
    }

    public static AbstractClient.AriaRequestWithResult<VersionInfo> getVersion() {
        return new AbstractClient.AriaRequestWithResult<>(AbstractClient.Method.GET_VERSION, new AbstractClient.Processor<VersionInfo>() {
            @NonNull
            @Override
            public VersionInfo process(AbstractClient client, JSONObject obj) throws JSONException {
                return new VersionInfo(obj.getJSONObject("result"));
            }
        });
    }
}