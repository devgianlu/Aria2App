package com.gianlu.aria2app.NetIO;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;

import com.gianlu.aria2app.NetIO.Aria2.AriaFile;
import com.gianlu.aria2app.NetIO.Aria2.Download;
import com.gianlu.aria2app.NetIO.Aria2.DownloadStatic;
import com.gianlu.aria2app.NetIO.Aria2.DownloadWithHelper;
import com.gianlu.aria2app.NetIO.Aria2.GlobalStats;
import com.gianlu.aria2app.NetIO.Aria2.Peers;
import com.gianlu.aria2app.NetIO.Aria2.Servers;
import com.gianlu.aria2app.NetIO.Aria2.SessionInfo;
import com.gianlu.aria2app.NetIO.Aria2.VersionInfo;
import com.gianlu.aria2app.Options.OptionsUtils;
import com.gianlu.commonutils.CommonUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class AriaRequests {
    private static final AbstractClient.Processor<List<Download>> DOWNLOADS_LIST_PROCESSOR = new AbstractClient.Processor<List<Download>>() {
        @NonNull
        @Override
        public List<Download> process(AbstractClient client, JSONObject obj) throws JSONException {
            List<Download> list = new ArrayList<>();
            JSONArray array = obj.getJSONArray("result");
            for (int i = 0; i < array.length(); i++)
                list.add(new Download(array.getJSONObject(i)));
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

    public static AbstractClient.AriaRequest changePosition(String gid, int pos, String mode) {
        return new AbstractClient.AriaRequest(AbstractClient.Method.CHANGE_POSITION, gid, pos, mode);
    }

    public static AbstractClient.AriaRequestWithResult<SparseArray<Servers>> getServers(final DownloadStatic download) {
        return new AbstractClient.AriaRequestWithResult<>(AbstractClient.Method.GET_SERVERS, new AbstractClient.Processor<SparseArray<Servers>>() {
            @NonNull
            @Override
            public SparseArray<Servers> process(AbstractClient client, JSONObject obj) throws JSONException {
                JSONArray array = obj.getJSONArray("result");
                SparseArray<Servers> list = new SparseArray<>();
                for (int i = 0; i < array.length(); i++) {
                    JSONObject server = array.getJSONObject(i);
                    list.put(server.getInt("index"), new Servers(download, server.getJSONArray("servers")));
                }
                return list;
            }
        }, download.gid);
    }

    public static AbstractClient.AriaRequestWithResult<Peers> getPeers(final DownloadStatic download) {
        return new AbstractClient.AriaRequestWithResult<>(AbstractClient.Method.GET_PEERS, new AbstractClient.Processor<Peers>() {
            @NonNull
            @Override
            public Peers process(AbstractClient client, JSONObject obj) throws JSONException {
                return new Peers(download, obj.getJSONArray("result"));
            }
        }, download.gid);
    }

    public static AbstractClient.AriaRequestWithResult<List<AriaFile>> getFiles(final DownloadStatic download) {
        return new AbstractClient.AriaRequestWithResult<>(AbstractClient.Method.GET_FILES, new AbstractClient.Processor<List<AriaFile>>() {
            @NonNull
            @Override
            public List<AriaFile> process(AbstractClient client, JSONObject obj) throws JSONException {
                List<AriaFile> list = new ArrayList<>();
                JSONArray array = obj.getJSONArray("result");
                for (int i = 0; i < array.length(); i++)
                    list.add(new AriaFile(download, array.getJSONObject(i)));
                return list;
            }
        }, download.gid);
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

    public static AbstractClient.AriaRequestWithResult<Map<String, String>> getOptions(String gid) {
        return new AbstractClient.AriaRequestWithResult<>(AbstractClient.Method.GET_OPTIONS, new AbstractClient.Processor<Map<String, String>>() {
            @NonNull
            @Override
            public Map<String, String> process(AbstractClient client, JSONObject obj) throws JSONException {
                return CommonUtils.toMap(obj.getJSONObject("result"), String.class);
            }
        }, gid);
    }

    public static AbstractClient.AriaRequestWithResult<String> addUri(@NonNull List<String> uris, @Nullable Integer pos, @Nullable Map<String, String> options) throws JSONException {
        Object[] params = new Object[3];
        params[0] = CommonUtils.toJSONArray(uris, true);
        if (options != null) params[1] = OptionsUtils.toJson(options);
        else params[1] = new JSONObject();
        if (pos != null) params[2] = pos;
        else params[2] = Integer.MAX_VALUE;
        return new AbstractClient.AriaRequestWithResult<>(AbstractClient.Method.ADD_URI, STRING_PROCESSOR, params);
    }

    public static AbstractClient.AriaRequestWithResult<String> addTorrent(@NonNull String base64, @Nullable List<String> uris, @Nullable Integer pos, @Nullable Map<String, String> options) throws JSONException {
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

    public static AbstractClient.AriaRequest changeOptions(String gid, Map<String, String> options) throws JSONException {
        return new AbstractClient.AriaRequest(AbstractClient.Method.CHANGE_OPTIONS, gid, OptionsUtils.toJson(options));
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

    public static AbstractClient.AriaRequestWithResult<DownloadWithHelper> tellStatus(String gid) {
        return new AbstractClient.AriaRequestWithResult<>(AbstractClient.Method.TELL_STATUS, new AbstractClient.Processor<DownloadWithHelper>() {
            @NonNull
            @Override
            public DownloadWithHelper process(AbstractClient client, JSONObject obj) throws JSONException {
                return new Download(obj.getJSONObject("result")).wrap(client);
            }
        }, gid);
    }

    public static AbstractClient.AriaRequestWithResult<List<Download>> tellActive() {
        return new AbstractClient.AriaRequestWithResult<>(AbstractClient.Method.TELL_ACTIVE, DOWNLOADS_LIST_PROCESSOR);
    }

    public static AbstractClient.AriaRequestWithResult<List<Download>> tellWaiting(int offset, int count) {
        return new AbstractClient.AriaRequestWithResult<>(AbstractClient.Method.TELL_WAITING, DOWNLOADS_LIST_PROCESSOR, offset, count);
    }

    public static AbstractClient.AriaRequestWithResult<List<Download>> tellStopped(int offset, int count) {
        return new AbstractClient.AriaRequestWithResult<>(AbstractClient.Method.TELL_STOPPED, DOWNLOADS_LIST_PROCESSOR, offset, count);
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