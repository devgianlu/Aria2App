package com.gianlu.jtitan.Aria2Helper;

import android.support.annotation.Nullable;

import com.gianlu.jtitan.JTRequester.JTARequester;
import com.gianlu.jtitan.JTRequester.JTHandler;
import com.gianlu.jtitan.JTRequester.JTRequester;
import com.gianlu.jtitan.JTRequester.JTResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class JTA2 {
    static boolean authNeeded = false;
    static String authToken = null;
    private String url;

    public JTA2(String url) {
        this.url = url;
    }

    // Authentication
    public void setAuthentication(boolean set) {
        authNeeded = set;
    }

    public void setToken(String token) {
        if (!authNeeded) authNeeded = true;
        authToken = token;
    }

    private void outResponseAsync(String request, JTHandler handler) {
        new Thread(new JTARequester(url, request, handler)).start();
    }

    private JTResponse outResponse(String request) throws IOException {
        return new JTRequester(url).send(request);
    }

    // Caster
    private Map<String, String> castToOptions(JSONObject string) throws JSONException {
        JSONObject jResult = string.getJSONObject("result");
        Iterator<?> keys = jResult.keys();

        Map<String, String> options = new HashMap<>();

        while (keys.hasNext()) {
            String key = (String) keys.next();
            options.put(key, jResult.optString(key));
        }

        return options;
    }

    private List<String> castToMethods(JSONObject string) throws JSONException {
        JSONArray jResult = string.getJSONArray("result");
        List<String> methods = new ArrayList<>();

        for (int i = 0; i < jResult.length(); i++) {
            methods.add(jResult.getString(i));
        }

        return methods;
    }

    // Requests
    //aria2.addUri
    public void addUri(List<String> uris, @Nullable Integer position, @Nullable Map<String, String> options, final IGID handler) {
        final JSONObject request = new A2Request();
        try {
            request.put("method", "aria2.addUri");
            JSONArray params = new A2Params();

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

        outResponseAsync(request.toString(), new JTHandler() {
            @Override
            public void onReceive(JTResponse response) {
                try {
                    handler.onGID(response.toJSON().getString("result"));
                } catch (JSONException ex) {
                    handler.onException(ex);
                }
            }

            @Override
            public void onException(Exception exception) {
                handler.onException(exception);
            }
        });
    }

    //aria2.addTorrent
    public void addTorrent(String base64, @Nullable List<String> uris, @Nullable Map<String, String> options, @Nullable Integer position, final IGID handler) {
        final JSONObject request = new A2Request();
        try {
            request.put("method", "aria2.addTorrent");
            JSONArray params = new A2Params();
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

        outResponseAsync(request.toString(), new JTHandler() {
            @Override
            public void onReceive(JTResponse response) {
                JSONObject jResult = response.toJSON();
                handler.onGID(jResult.toString());
            }

            @Override
            public void onException(Exception exception) {
                handler.onException(exception);
            }
        });
    }

    //aria2.addTorrent
    public void addMetalink(String base64, @Nullable List<String> uris, @Nullable Map<String, String> options, @Nullable Integer position, final IGID handler) {
        final JSONObject request = new A2Request();
        try {
            request.put("method", "aria2.addMetalink");
            JSONArray params = new A2Params();
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

        outResponseAsync(request.toString(), new JTHandler() {
            @Override
            public void onReceive(JTResponse response) {
                JSONObject jResult = response.toJSON();
                handler.onGID(jResult.toString());
            }

            @Override
            public void onException(Exception exception) {
                handler.onException(exception);
            }
        });
    }

    //aria2.tellStatus
    public void tellStatus(String gid, final IDownload handler) {
        final JSONObject request = new A2Request();
        try {
            request.put("method", "aria2.tellStatus");
            JSONArray params = new A2Params();
            params.put(gid);
            request.put("params", params);
        } catch (JSONException ex) {
            handler.onException(ex);
            return;
        }

        outResponseAsync(request.toString(), new JTHandler() {
            @Override
            public void onReceive(JTResponse response) {
                try {
                    handler.onDownload(Download.fromString(response.toJSON().getJSONObject("result")));
                } catch (JSONException ex) {
                    handler.onException(ex);
                }
            }

            @Override
            public void onException(Exception exception) {
                handler.onException(exception);
                exception.printStackTrace();
            }
        });
    }

    public Download tellStatus(String gid) throws IOException {
        final JSONObject request = new A2Request();
        try {
            request.put("method", "aria2.tellStatus");
            JSONArray params = new A2Params();
            params.put(gid);
            request.put("params", params);

            return Download.fromString(outResponse(request.toString()).toJSON().getJSONObject("result"));
        } catch (JSONException ex) {
            return null;
        }
    }

    //aria2.getGlobalStat
    public void getGlobalStat(final IStats handler) {
        final JSONObject request = new A2Request();
        try {
            request.put("method", "aria2.getGlobalStat");
            request.put("params", new A2Params());
        } catch (JSONException ex) {
            handler.onException(ex);
            return;
        }

        outResponseAsync(request.toString(), new JTHandler() {
            @Override
            public void onReceive(JTResponse response) {
                try {
                    handler.onStats(GlobalStats.fromString(response.toJSON()));
                } catch (JSONException ex) {
                    handler.onException(ex);
                }
            }

            @Override
            public void onException(Exception exception) {
                handler.onException(exception);
            }
        });
    }

    public GlobalStats getGlobalStat() throws IOException {
        final JSONObject request = new A2Request();
        try {
            request.put("method", "aria2.getGlobalStat");
            request.put("params", new A2Params());

            return GlobalStats.fromString(outResponse(request.toString()).toJSON());
        } catch (JSONException ex) {
            return null;
        }
    }

    //aria2.tellActive
    public void tellActive(final IDownloadList handler) {
        final JSONObject request = new A2Request();
        try {
            request.put("method", "aria2.tellActive");
            request.put("params", new A2Params());
        } catch (JSONException ex) {
            return;
        }

        outResponseAsync(request.toString(), new JTHandler() {
            @Override
            public void onReceive(JTResponse response) {
                List<Download> downloads = new ArrayList<>();
                try {
                    JSONArray jResult = response.toJSON().getJSONArray("result");
                    for (int c = 0; c < jResult.length(); c++) {
                        downloads.add(Download.fromString(jResult.getJSONObject(c)));
                    }
                    handler.onDownloads(downloads);
                } catch (JSONException ex) {
                    handler.onException(ex);
                }
            }

            @Override
            public void onException(Exception exception) {
                handler.onException(exception);
            }
        });
    }

    //aria2.tellWaiting
    public void tellWaiting(final IDownloadList handler) {
        final JSONObject request = new A2Request();
        try {
            request.put("method", "aria2.tellWaiting");
            A2Params params = new A2Params();
            params.put(0);
            params.put(100);
            request.put("params", params);
        } catch (JSONException ex) {
            handler.onException(ex);
            return;
        }

        outResponseAsync(request.toString(), new JTHandler() {
            @Override
            public void onReceive(JTResponse response) {
                List<Download> downloads = new ArrayList<>();
                try {
                    JSONArray jResult = response.toJSON().getJSONArray("result");
                    for (int c = 0; c < jResult.length(); c++) {
                        downloads.add(Download.fromString(jResult.getJSONObject(c)));
                    }
                    handler.onDownloads(downloads);
                } catch (JSONException ex) {
                    handler.onException(ex);
                }
            }

            @Override
            public void onException(Exception exception) {
                handler.onException(exception);
            }
        });
    }

    //aria2.tellStopped
    public void tellStopped(final IDownloadList handler) {
        final JSONObject request = new A2Request();
        try {
            request.put("method", "aria2.tellStopped");
            A2Params params = new A2Params();
            params.put(0);
            params.put(100);
            request.put("params", params);
        } catch (JSONException ex) {
            handler.onException(ex);
            return;
        }

        outResponseAsync(request.toString(), new JTHandler() {
            @Override
            public void onReceive(JTResponse response) {
                List<Download> downloads = new ArrayList<>();
                try {
                    JSONArray jResult = response.toJSON().getJSONArray("result");
                    for (int c = 0; c < jResult.length(); c++) {
                        downloads.add(Download.fromString(jResult.getJSONObject(c)));
                    }
                    handler.onDownloads(downloads);
                } catch (JSONException ex) {
                    handler.onException(ex);
                }
            }

            @Override
            public void onException(Exception exception) {
                handler.onException(exception);
            }
        });
    }

    //aria2.pause
    public void pause(String gid, final IGID handler) {
        final JSONObject request = new A2Request();
        try {
            request.put("method", "aria2.pause");
            JSONArray params = new A2Params();
            params.put(gid);
            request.put("params", params);
        } catch (JSONException ex) {
            handler.onException(ex);
            return;
        }

        outResponseAsync(request.toString(), new JTHandler() {
            @Override
            public void onReceive(JTResponse response) {
                handler.onGID(response.toString());
            }

            @Override
            public void onException(Exception exception) {
                handler.onException(exception);
            }
        });
    }

    //aria2.unpause
    public void unpause(String gid, final IGID handler) {
        final JSONObject request = new A2Request();
        try {
            request.put("method", "aria2.unpause");
            JSONArray params = new A2Params();
            params.put(gid);
            request.put("params", params);
        } catch (JSONException ex) {
            handler.onException(ex);
            return;
        }

        outResponseAsync(request.toString(), new JTHandler() {
            @Override
            public void onReceive(JTResponse response) {
                handler.onGID(response.toString());
            }

            @Override
            public void onException(Exception exception) {
                handler.onException(exception);
            }
        });
    }

    //aria2.remove
    public void remove(final String gid, final IGID handler) {
        final JSONObject request = new A2Request();
        try {
            request.put("method", "aria2.remove");
            JSONArray params = new A2Params();
            params.put(gid);
            request.put("params", params);
        } catch (JSONException ex) {
            handler.onException(ex);
            return;
        }

        outResponseAsync(request.toString(), new JTHandler() {
            @Override
            public void onReceive(JTResponse response) {
                handler.onGID(response.toString());
            }

            @Override
            public void onException(Exception exception) {
                handler.onException(exception);
            }
        });
    }

    //aria2.removeDownloadResult
    public void removeDownloadResult(String gid, final IGID handler) {
        final JSONObject request = new A2Request();
        try {
            request.put("method", "aria2.removeDownloadResult");
            JSONArray params = new A2Params();
            params.put(gid);
            request.put("params", params);
        } catch (JSONException ex) {
            handler.onException(ex);
            return;
        }

        outResponseAsync(request.toString(), new JTHandler() {
            @Override
            public void onReceive(JTResponse response) {
                handler.onGID(response.toString());
            }

            @Override
            public void onException(Exception exception) {
                handler.onException(exception);
            }
        });
    }

    //aria2.forcePause
    public void forcePause(String gid, final IGID handler) {
        final JSONObject request = new A2Request();
        try {
            request.put("method", "aria2.forcePause");
            JSONArray params = new A2Params();
            params.put(gid);
            request.put("params", params);
        } catch (JSONException ex) {
            handler.onException(ex);
            return;
        }

        outResponseAsync(request.toString(), new JTHandler() {
            @Override
            public void onReceive(JTResponse response) {
                handler.onGID(response.toString());
            }

            @Override
            public void onException(Exception exception) {
                handler.onException(exception);
            }
        });
    }

    //aria2.forceRemove
    public void forceRemove(String gid, final IGID handler) {
        final JSONObject request = new A2Request();
        try {
            request.put("method", "aria2.forceRemove");
            JSONArray params = new A2Params();
            params.put(gid);
            request.put("params", params);
        } catch (JSONException ex) {
            handler.onException(ex);
            return;
        }

        outResponseAsync(request.toString(), new JTHandler() {
            @Override
            public void onReceive(JTResponse response) {
                handler.onGID(response.toJSON().toString());
            }

            @Override
            public void onException(Exception exception) {
                handler.onException(exception);
            }
        });
    }

    //aria2.getOption
    public void getOption(String gid, final IOption handler) {
        final JSONObject request = new A2Request();
        try {
            request.put("method", "aria2.getOption");
            JSONArray params = new A2Params();
            params.put(gid);
            request.put("params", params);
        } catch (JSONException ex) {
            handler.onException(ex);
            return;
        }

        outResponseAsync(request.toString(), new JTHandler() {
            @Override
            public void onReceive(JTResponse response) {
                try {
                    handler.onOptions(castToOptions(response.toJSON()));
                } catch (JSONException ex) {
                    handler.onException(ex);
                }
            }

            @Override
            public void onException(Exception exception) {
                handler.onException(exception);
            }
        });
    }

    //aria2.getGlobalOption
    public void getGlobalOption(final IOption handler) {
        final JSONObject request = new A2Request();
        try {
            request.put("method", "aria2.getGlobalOption");
            request.put("params", new A2Params());
        } catch (JSONException ex) {
            handler.onException(ex);
            return;
        }

        outResponseAsync(request.toString(), new JTHandler() {
            @Override
            public void onReceive(JTResponse response) {
                try {
                    handler.onOptions(castToOptions(response.toJSON()));
                } catch (JSONException ex) {
                    handler.onException(ex);
                }
            }

            @Override
            public void onException(Exception exception) {
                handler.onException(exception);
            }
        });
    }

    //aria2.changeOption
    public void changeOption(String gid, Map<String, String> options, final ISuccess handler) {
        final JSONObject request = new A2Request();
        try {
            request.put("method", "aria2.changeOption");
            JSONArray params = new A2Params();
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

        outResponseAsync(request.toString(), new JTHandler() {
            @Override
            public void onReceive(JTResponse response) {
                handler.onSuccess();
            }

            @Override
            public void onException(Exception exception) {
                handler.onException(exception);
            }
        });
    }

    //aria2.changeGlobalOption
    public void changeGlobalOption(Map<String, String> options, final ISuccess handler) {
        final JSONObject request = new A2Request();
        try {
            request.put("method", "aria2.changeGlobalOption");
            JSONArray params = new A2Params();
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

        outResponseAsync(request.toString(), new JTHandler() {
            @Override
            public void onReceive(JTResponse response) {
                handler.onSuccess();
            }

            @Override
            public void onException(Exception exception) {
                handler.onException(exception);
            }
        });
    }

    //system.listMethods
    public void listMethods(final IMethod handler) {
        final JSONObject request = new A2Request();
        try {
            request.put("method", "system.listMethods");
        } catch (JSONException ex) {
            handler.onException(ex);
            return;
        }

        outResponseAsync(request.toString(), new JTHandler() {
            @Override
            public void onReceive(JTResponse response) {
                try {
                    handler.onMethods(castToMethods(response.toJSON()));
                } catch (JSONException ex) {
                    handler.onException(ex);
                }
            }

            @Override
            public void onException(Exception exception) {
                handler.onException(exception);
            }
        });
    }

    private class A2Request extends JSONObject {
        private A2Request() {
            try {
                this.put("jsonrpc", "2.0");
                this.put("id", String.valueOf(new Random().nextInt(200)));
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
        }
    }
}
