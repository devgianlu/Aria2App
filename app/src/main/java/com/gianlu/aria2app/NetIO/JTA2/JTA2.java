package com.gianlu.aria2app.NetIO.JTA2;

import android.app.Activity;
import android.support.annotation.Nullable;

import com.gianlu.aria2app.NetIO.WebSocketing;
import com.gianlu.aria2app.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class JTA2 {
    private WebSocketing webSocketing;

    public JTA2(WebSocketing webSocketing) {
        this.webSocketing = webSocketing;
    }

    public static JTA2 newInstance(Activity context) throws IOException, NoSuchAlgorithmException {
        return new JTA2(WebSocketing.newInstance(context));
    }

    // Caster
    private List<FEATURES> fromFeatures(JSONArray features) throws JSONException {
        if (features == null) return null;

        List<FEATURES> featuresList = new ArrayList<>();
        for (int i = 0; i < features.length(); i++) {
            String _feature = features.getString(i);

            switch (_feature.toLowerCase()) {
                case "bittorrent":
                    featuresList.add(FEATURES.BITTORRENT);
                    break;
                case "gzip":
                    featuresList.add(FEATURES.GZIP);
                    break;
                case "https":
                    featuresList.add(FEATURES.HTTPS);
                    break;
                case "message digest":
                    featuresList.add(FEATURES.MESSAGE_DIGEST);
                    break;
                case "metalink":
                    featuresList.add(FEATURES.METALINK);
                    break;
                case "xml-rpc":
                    featuresList.add(FEATURES.XML_RPC);
                    break;
            }
        }

        return featuresList;
    }
    private Map<String, String> fromOptions(JSONObject jResult) throws JSONException {
        if (jResult == null) return null;

        Iterator<?> keys = jResult.keys();

        Map<String, String> options = new HashMap<>();

        while (keys.hasNext()) {
            String key = (String) keys.next();
            options.put(key, jResult.optString(key));
        }

        return options;
    }
    private List<String> fromMethods(JSONArray jResult) throws JSONException {
        if (jResult == null) return null;

        List<String> methods = new ArrayList<>();

        for (int i = 0; i < jResult.length(); i++) {
            methods.add(jResult.getString(i));
        }

        return methods;
    }
    private List<Peer> fromPeers(JSONArray jResult) throws JSONException {
        if (jResult == null) return null;

        List<Peer> peers = new ArrayList<>();

        for (int i = 0; i < jResult.length(); i++) {
            peers.add(Peer.fromJSON(jResult.getJSONObject(i)));
        }

        return peers;
    }
    private List<File> fromFiles(JSONArray jResult) throws JSONException {
        if (jResult == null) return null;

        List<File> files = new ArrayList<>();

        for (int i = 0; i < jResult.length(); i++) {
            files.add(File.fromJSON(jResult.getJSONObject(i)));
        }

        return files;
    }
    private Map<Integer, List<Server>> fromServers(JSONArray jResult) throws JSONException {
        if (jResult == null) return null;

        Map<Integer, List<Server>> list = new HashMap<>();

        for (int i = 0; i < jResult.length(); i++) {
            JSONObject jServer = jResult.getJSONObject(i);

            int index = jServer.getInt("index");

            JSONArray _servers = jServer.getJSONArray("servers");

            List<Server> servers = new ArrayList<>();
            for (int ii = 0; ii < _servers.length(); ii++) {
                servers.add(Server.fromJSON(_servers.getJSONObject(i)));
            }
            list.put(index, servers);
        }


        return list;
    }

    // Requests
    //aria2.getVersion
    public void getVersion(final IVersion handler) {
        JSONObject request;
        try {
            request = Utils.readyRequest();
            request.put("method", "aria2.getVersion");
            JSONArray params = Utils.readyParams(webSocketing.getContext());
            request.put("params", params);
        } catch (JSONException ex) {
            handler.onException(ex);
            return;
        }

        webSocketing.send(request, new WebSocketing.IReceived() {
            @Override
            public void onResponse(JSONObject response) throws JSONException {
                handler.onVersion(fromFeatures(response.getJSONObject("result").optJSONArray("enabledFeatures")), response.getJSONObject("result").optString("version"));
            }

            @Override
            public void onException(boolean q, Exception ex) {
                handler.onException(ex);
            }

            @Override
            public void onException(int code, String reason) {
                handler.onException(new Aria2Exception(reason, code));
            }
        });
    }
    //aria2.addUri
    public void addUri(List<String> uris, @Nullable Integer position, @Nullable Map<String, String> options, final IGID handler) {
        JSONObject request;
        try {
            request = Utils.readyRequest();
            request.put("method", "aria2.addUri");
            JSONArray params = Utils.readyParams(webSocketing.getContext());

            JSONArray jUris = new JSONArray();
            for (String uri : uris) {
                jUris.put(uri);
            }
            params.put(jUris);

            JSONObject jOptions = new JSONObject();
            if (options != null) {
                for (String key : options.keySet()) {
                    jOptions.put(key, options.get(key));
                }
            }
            params.put(jOptions);

            if (position != null) params.put(position);
            request.put("params", params);
        } catch (JSONException ex) {
            handler.onException(ex);
            return;
        }

        webSocketing.send(request, new WebSocketing.IReceived() {
            @Override
            public void onResponse(JSONObject response) throws JSONException {
                handler.onGID(response.getString("result"));
            }

            @Override
            public void onException(boolean q, Exception ex) {
                handler.onException(ex);
            }

            @Override
            public void onException(int code, String reason) {
                handler.onException(new Aria2Exception(reason, code));
            }
        });
    }

    //aria2.addTorrent
    public void addTorrent(String base64, @Nullable List<String> uris, @Nullable Map<String, String> options, @Nullable Integer position, final IGID handler) {
        JSONObject request;
        try {
            request = Utils.readyRequest();
            request.put("method", "aria2.addTorrent");
            JSONArray params = Utils.readyParams(webSocketing.getContext());
            params.put(base64);

            JSONArray jUris = new JSONArray();
            if (uris != null) {
                for (String uri : uris) {
                    jUris.put(uri);
                }
            }
            params.put(jUris);

            JSONObject jOptions = new JSONObject();
            if (options != null) {
                for (String key : options.keySet()) {
                    jOptions.put(key, options.get(key));
                }
            }
            params.put(jOptions);

            if (position != null) params.put(position);
            request.put("params", params);
        } catch (JSONException ex) {
            handler.onException(ex);
            return;
        }

        webSocketing.send(request, new WebSocketing.IReceived() {
            @Override
            public void onResponse(JSONObject response) throws JSONException {
                handler.onGID(response.getString("result"));
            }

            @Override
            public void onException(boolean q, Exception ex) {
                handler.onException(ex);
            }

            @Override
            public void onException(int code, String reason) {
                handler.onException(new Aria2Exception(reason, code));
            }
        });
    }

    //aria2.addMetalink
    public void addMetalink(String base64, @Nullable List<String> uris, @Nullable Map<String, String> options, @Nullable Integer position, final IGID handler) {
        JSONObject request;
        try {
            request = Utils.readyRequest();
            request.put("method", "aria2.addMetalink");
            JSONArray params = Utils.readyParams(webSocketing.getContext());
            params.put(base64);

            JSONArray jUris = new JSONArray();
            if (uris != null) {
                for (String uri : uris) {
                    jUris.put(uri);
                }
            }
            params.put(jUris);

            JSONObject jOptions = new JSONObject();
            if (options != null) {
                for (String key : options.keySet()) {
                    jOptions.put(key, options.get(key));
                }
            }
            params.put(jOptions);

            if (position != null) params.put(position);
            request.put("params", params);
        } catch (JSONException ex) {
            handler.onException(ex);
            return;
        }

        webSocketing.send(request, new WebSocketing.IReceived() {
            @Override
            public void onResponse(JSONObject response) throws JSONException {
                handler.onGID(response.getString("result"));
            }

            @Override
            public void onException(boolean q, Exception ex) {
                handler.onException(ex);
            }

            @Override
            public void onException(int code, String reason) {
                handler.onException(new Aria2Exception(reason, code));
            }
        });
    }

    //aria2.tellStatus
    public void tellStatus(String gid, final IDownload handler) {
        JSONObject request;
        try {
            request = Utils.readyRequest();
            request.put("method", "aria2.tellStatus");
            JSONArray params = Utils.readyParams(webSocketing.getContext());
            params.put(gid);
            request.put("params", params);
        } catch (JSONException ex) {
            handler.onException(ex);
            return;
        }

        webSocketing.send(request, new WebSocketing.IReceived() {
            @Override
            public void onResponse(JSONObject response) throws JSONException {
                handler.onDownload(Download.fromJSON(response.getJSONObject("result")));
            }

            @Override
            public void onException(boolean q, Exception ex) {
                handler.onException(ex);
            }

            @Override
            public void onException(int code, String reason) {
                handler.onException(new Aria2Exception(reason, code));
            }
        });
    }

    //aria2.getGlobalStat
    public void getGlobalStat(final IStats handler) {
        JSONObject request;
        try {
            request = Utils.readyRequest();
            request.put("method", "aria2.getGlobalStat");
            request.put("params", Utils.readyParams(webSocketing.getContext()));
        } catch (JSONException ex) {
            handler.onException(ex);
            return;
        }

        webSocketing.send(request, new WebSocketing.IReceived() {
            @Override
            public void onResponse(JSONObject response) throws JSONException {
                handler.onStats(GlobalStats.fromJSON(response.getJSONObject("result")));
            }

            @Override
            public void onException(boolean q, Exception ex) {
                handler.onException(ex);
            }

            @Override
            public void onException(int code, String reason) {
                handler.onException(new Aria2Exception(reason, code));
            }
        });
    }

    //aria2.tellActive
    public void tellActive(final IDownloadList handler) {
        JSONObject request;
        try {
            request = Utils.readyRequest();
            request.put("method", "aria2.tellActive");
            request.put("params", Utils.readyParams(webSocketing.getContext()));
        } catch (JSONException ex) {
            handler.onException(false, ex);
            return;
        }

        webSocketing.send(request, new WebSocketing.IReceived() {
            @Override
            public void onResponse(JSONObject response) throws JSONException {
                List<Download> downloads = new ArrayList<>();
                JSONArray jResult = response.getJSONArray("result");

                for (int c = 0; c < jResult.length(); c++) {
                    downloads.add(Download.fromJSON(jResult.getJSONObject(c)));
                }

                handler.onDownloads(downloads);
            }

            @Override
            public void onException(boolean q, Exception ex) {
                handler.onException(q, ex);
            }

            @Override
            public void onException(int code, String reason) {
                handler.onException(false, new Aria2Exception(reason, code));
            }
        });
    }

    //aria2.tellWaiting
    public void tellWaiting(final IDownloadList handler) {
        JSONObject request;
        try {
            request = Utils.readyRequest();
            request.put("method", "aria2.tellWaiting");
            JSONArray params = Utils.readyParams(webSocketing.getContext());
            params.put(0);
            params.put(100);
            request.put("params", params);
        } catch (JSONException ex) {
            handler.onException(false, ex);
            return;
        }

        webSocketing.send(request, new WebSocketing.IReceived() {
            @Override
            public void onResponse(JSONObject response) throws JSONException {
                List<Download> downloads = new ArrayList<>();
                JSONArray jResult = response.getJSONArray("result");

                for (int c = 0; c < jResult.length(); c++) {
                    downloads.add(Download.fromJSON(jResult.getJSONObject(c)));
                }

                handler.onDownloads(downloads);
            }

            @Override
            public void onException(boolean q, Exception ex) {
                handler.onException(q, ex);
            }

            @Override
            public void onException(int code, String reason) {
                handler.onException(false, new Aria2Exception(reason, code));
            }
        });
    }

    //aria2.tellStopped
    public void tellStopped(final IDownloadList handler) {
        JSONObject request;
        try {
            request = Utils.readyRequest();
            request.put("method", "aria2.tellStopped");
            JSONArray params = Utils.readyParams(webSocketing.getContext());
            params.put(0);
            params.put(100);
            request.put("params", params);
        } catch (JSONException ex) {
            handler.onException(false, ex);
            return;
        }

        webSocketing.send(request, new WebSocketing.IReceived() {
            @Override
            public void onResponse(JSONObject response) throws JSONException {
                List<Download> downloads = new ArrayList<>();
                JSONArray jResult = response.getJSONArray("result");

                for (int c = 0; c < jResult.length(); c++) {
                    downloads.add(Download.fromJSON(jResult.getJSONObject(c)));
                }

                handler.onDownloads(downloads);
            }

            @Override
            public void onException(boolean q, Exception ex) {
                handler.onException(q, ex);
            }

            @Override
            public void onException(int code, String reason) {
                handler.onException(false, new Aria2Exception(reason, code));
            }
        });
    }

    //aria2.pause
    public void pause(String gid, final IGID handler) {
        JSONObject request;
        try {
            request = Utils.readyRequest();
            request.put("method", "aria2.pause");
            JSONArray params = Utils.readyParams(webSocketing.getContext());
            params.put(gid);
            request.put("params", params);
        } catch (JSONException ex) {
            handler.onException(ex);
            return;
        }

        webSocketing.send(request, new WebSocketing.IReceived() {
            @Override
            public void onResponse(JSONObject response) throws JSONException {
                handler.onGID(response.getString("result"));
            }

            @Override
            public void onException(boolean q, Exception ex) {
                handler.onException(ex);
            }

            @Override
            public void onException(int code, String reason) {
                handler.onException(new Aria2Exception(reason, code));
            }
        });
    }

    //aria2.unpause
    public void unpause(String gid, final IGID handler) {
        JSONObject request;
        try {
            request = Utils.readyRequest();
            request.put("method", "aria2.unpause");
            JSONArray params = Utils.readyParams(webSocketing.getContext());
            params.put(gid);
            request.put("params", params);
        } catch (JSONException ex) {
            handler.onException(ex);
            return;
        }

        webSocketing.send(request, new WebSocketing.IReceived() {
            @Override
            public void onResponse(JSONObject response) throws JSONException {
                handler.onGID(response.getString("result"));
            }

            @Override
            public void onException(boolean q, Exception ex) {
                handler.onException(ex);
            }

            @Override
            public void onException(int code, String reason) {
                handler.onException(new Aria2Exception(reason, code));
            }
        });
    }

    //aria2.remove
    public void remove(final String gid, final IGID handler) {
        JSONObject request;
        try {
            request = Utils.readyRequest();
            request.put("method", "aria2.remove");
            JSONArray params = Utils.readyParams(webSocketing.getContext());
            params.put(gid);
            request.put("params", params);
        } catch (JSONException ex) {
            handler.onException(ex);
            return;
        }

        webSocketing.send(request, new WebSocketing.IReceived() {
            @Override
            public void onResponse(JSONObject response) throws JSONException {
                handler.onGID(response.getString("result"));
            }

            @Override
            public void onException(boolean q, Exception ex) {
                handler.onException(ex);
            }

            @Override
            public void onException(int code, String reason) {
                handler.onException(new Aria2Exception(reason, code));
            }
        });
    }

    //aria2.removeDownloadResult
    public void removeDownloadResult(String gid, final IGID handler) {
        JSONObject request;
        try {
            request = Utils.readyRequest();
            request.put("method", "aria2.removeDownloadResult");
            JSONArray params = Utils.readyParams(webSocketing.getContext());
            params.put(gid);
            request.put("params", params);
        } catch (JSONException ex) {
            handler.onException(ex);
            return;
        }

        webSocketing.send(request, new WebSocketing.IReceived() {
            @Override
            public void onResponse(JSONObject response) throws JSONException {
                handler.onGID(response.getString("result"));
            }

            @Override
            public void onException(boolean q, Exception ex) {
                handler.onException(ex);
            }

            @Override
            public void onException(int code, String reason) {
                handler.onException(new Aria2Exception(reason, code));
            }
        });
    }

    //aria2.forcePause
    public void forcePause(String gid, final IGID handler) {
        JSONObject request;
        try {
            request = Utils.readyRequest();
            request.put("method", "aria2.forcePause");
            JSONArray params = Utils.readyParams(webSocketing.getContext());
            params.put(gid);
            request.put("params", params);
        } catch (JSONException ex) {
            handler.onException(ex);
            return;
        }

        webSocketing.send(request, new WebSocketing.IReceived() {
            @Override
            public void onResponse(JSONObject response) throws JSONException {
                handler.onGID(response.getString("result"));
            }

            @Override
            public void onException(boolean q, Exception ex) {
                handler.onException(ex);
            }

            @Override
            public void onException(int code, String reason) {
                handler.onException(new Aria2Exception(reason, code));
            }
        });
    }

    //aria2.forceRemove
    public void forceRemove(String gid, final IGID handler) {
        JSONObject request;
        try {
            request = Utils.readyRequest();
            request.put("method", "aria2.forceRemove");
            JSONArray params = Utils.readyParams(webSocketing.getContext());
            params.put(gid);
            request.put("params", params);
        } catch (JSONException ex) {
            handler.onException(ex);
            return;
        }

        webSocketing.send(request, new WebSocketing.IReceived() {
            @Override
            public void onResponse(JSONObject response) throws JSONException {
                handler.onGID(response.getString("result"));
            }

            @Override
            public void onException(boolean q, Exception ex) {
                handler.onException(ex);
            }

            @Override
            public void onException(int code, String reason) {
                handler.onException(new Aria2Exception(reason, code));
            }
        });
    }

    //aria2.getOption
    public void getOption(String gid, final IOption handler) {
        JSONObject request;
        try {
            request = Utils.readyRequest();
            request.put("method", "aria2.getOption");
            JSONArray params = Utils.readyParams(webSocketing.getContext());
            params.put(gid);
            request.put("params", params);
        } catch (JSONException ex) {
            handler.onException(ex);
            return;
        }

        webSocketing.send(request, new WebSocketing.IReceived() {
            @Override
            public void onResponse(JSONObject response) throws JSONException {
                handler.onOptions(fromOptions(response.getJSONObject("result")));
            }

            @Override
            public void onException(boolean q, Exception ex) {
                handler.onException(ex);
            }

            @Override
            public void onException(int code, String reason) {
                handler.onException(new Aria2Exception(reason, code));
            }
        });
    }

    //aria2.getGlobalOption
    public void getGlobalOption(final IOption handler) {
        JSONObject request;
        try {
            request = Utils.readyRequest();
            request.put("method", "aria2.getGlobalOption");
            request.put("params", Utils.readyParams(webSocketing.getContext()));
        } catch (JSONException ex) {
            handler.onException(ex);
            return;
        }

        webSocketing.send(request, new WebSocketing.IReceived() {
            @Override
            public void onResponse(JSONObject response) throws JSONException {
                handler.onOptions(fromOptions(response.getJSONObject("result")));
            }

            @Override
            public void onException(boolean q, Exception ex) {
                handler.onException(ex);
            }

            @Override
            public void onException(int code, String reason) {
                handler.onException(new Aria2Exception(reason, code));
            }
        });
    }

    //aria2.changeOption
    public void changeOption(String gid, Map<String, String> options, final ISuccess handler) {
        JSONObject request;
        try {
            request = Utils.readyRequest();
            request.put("method", "aria2.changeOption");
            JSONArray params = Utils.readyParams(webSocketing.getContext());
            params.put(gid);
            JSONObject jOptions = new JSONObject();
            for (Map.Entry<String, String> entry : options.entrySet()) {
                jOptions.put(entry.getKey(), entry.getValue());
            }
            params.put(jOptions);
            request.put("params", params);
        } catch (JSONException ex) {
            handler.onException(ex);
            return;
        }

        webSocketing.send(request, new WebSocketing.IReceived() {
            @Override
            public void onResponse(JSONObject response) throws JSONException {
                handler.onSuccess();
            }

            @Override
            public void onException(boolean q, Exception ex) {
                handler.onException(ex);
            }

            @Override
            public void onException(int code, String reason) {
                handler.onException(new Aria2Exception(reason, code));
            }
        });
    }

    //aria2.changePosition
    public void changePosition(String gid, int pos, POSITION_HOW how, final ISuccess handler) {
        JSONObject request;
        try {
            request = Utils.readyRequest();
            request.put("method", "aria2.changePosition");
            JSONArray params = Utils.readyParams(webSocketing.getContext());
            params.put(gid)
                    .put(pos)
                    .put(how.name());
            request.put("params", params);
        } catch (JSONException ex) {
            handler.onException(ex);
            return;
        }

        webSocketing.send(request, new WebSocketing.IReceived() {
            @Override
            public void onResponse(JSONObject response) throws JSONException {
                handler.onSuccess();
            }

            @Override
            public void onException(boolean q, Exception ex) {
                handler.onException(ex);
            }

            @Override
            public void onException(int code, String reason) {
                handler.onException(new Aria2Exception(reason, code));
            }
        });
    }

    //aria2.changeGlobalOption
    public void changeGlobalOption(Map<String, String> options, final ISuccess handler) {
        JSONObject request;
        try {
            request = Utils.readyRequest();
            request.put("method", "aria2.changeGlobalOption");
            JSONArray params = Utils.readyParams(webSocketing.getContext());
            JSONObject jOptions = new JSONObject();
            for (Map.Entry<String, String> entry : options.entrySet()) {
                jOptions.put(entry.getKey(), entry.getValue());
            }
            params.put(jOptions);
            request.put("params", params);
        } catch (JSONException ex) {
            handler.onException(ex);
            return;
        }

        webSocketing.send(request, new WebSocketing.IReceived() {
            @Override
            public void onResponse(JSONObject response) throws JSONException {
                handler.onSuccess();
            }

            @Override
            public void onException(boolean q, Exception ex) {
                handler.onException(ex);
            }

            @Override
            public void onException(int code, String reason) {
                handler.onException(new Aria2Exception(reason, code));
            }
        });
    }

    // aria2.getServers
    public void getServers(String gid, final IServers handler) {
        JSONObject request;
        try {
            request = Utils.readyRequest();
            request.put("method", "aria2.getServers");
            request.put("params", Utils.readyParams(webSocketing.getContext())
                    .put(gid));
        } catch (JSONException ex) {
            handler.onException(ex);
            return;
        }

        webSocketing.send(request, new WebSocketing.IReceived() {
            @Override
            public void onResponse(JSONObject response) throws JSONException {
                handler.onServers(fromServers(response.optJSONArray("result")));
            }

            @Override
            public void onException(boolean q, Exception ex) {
                handler.onException(ex);
            }

            @Override
            public void onException(int code, String reason) {
                if (code == 1 && reason.startsWith("No active download")) {
                    handler.onDownloadNotActive(new Aria2Exception(reason, code));
                } else {
                    handler.onException(new Aria2Exception(reason, code));
                }
            }
        });
    }


    // aria2.getPeers
    public void getPeers(String gid, final IPeers handler) {
        JSONObject request;
        try {
            request = Utils.readyRequest();
            request.put("method", "aria2.getPeers");
            request.put("params", Utils.readyParams(webSocketing.getContext())
                    .put(gid));
        } catch (JSONException ex) {
            handler.onException(ex);
            return;
        }

        webSocketing.send(request, new WebSocketing.IReceived() {
            @Override
            public void onResponse(JSONObject response) throws JSONException {
                handler.onPeers(fromPeers(response.optJSONArray("result")));
            }

            @Override
            public void onException(boolean q, Exception ex) {
                handler.onException(ex);
            }

            @Override
            public void onException(int code, String reason) {
                if (code == 1 && reason.startsWith("No peer data")) {
                    handler.onNoPeerData(new Aria2Exception(reason, code));
                } else {
                    handler.onException(new Aria2Exception(reason, code));
                }
            }
        });
    }

    // aria2.getFiles
    public void getFiles(String gid, final IFiles handler) {
        JSONObject request;
        try {
            request = Utils.readyRequest();
            request.put("method", "aria2.getFiles");
            request.put("params", Utils.readyParams(webSocketing.getContext())
                    .put(gid));
        } catch (JSONException ex) {
            handler.onException(ex);
            return;
        }

        webSocketing.send(request, new WebSocketing.IReceived() {
            @Override
            public void onResponse(JSONObject response) throws JSONException {
                handler.onFiles(fromFiles(response.optJSONArray("result")));
            }

            @Override
            public void onException(boolean q, Exception ex) {
                handler.onException(ex);
            }

            @Override
            public void onException(int code, String reason) {
                handler.onException(new Aria2Exception(reason, code));
            }
        });
    }

    // system.listMethods
    public void listMethods(final IMethod handler) {
        JSONObject request;
        try {
            request = Utils.readyRequest();
            request.put("method", "system.listMethods");
        } catch (JSONException ex) {
            handler.onException(ex);
            return;
        }

        webSocketing.send(request, new WebSocketing.IReceived() {
            @Override
            public void onResponse(JSONObject response) throws JSONException {
                handler.onMethods(fromMethods(response.getJSONArray("result")));
            }

            @Override
            public void onException(boolean q, Exception ex) {
                handler.onException(ex);
            }

            @Override
            public void onException(int code, String reason) {
                handler.onException(new Aria2Exception(reason, code));
            }
        });
    }

    public enum AUTH_METHOD {
        NONE,
        HTTP,
        TOKEN
    }

    public enum FEATURES {
        BITTORRENT,
        GZIP,
        HTTPS,
        MESSAGE_DIGEST,
        METALINK,
        XML_RPC
    }

    public enum POSITION_HOW {
        POS_CUR,
        POS_END,
        POS_SET
    }
}
