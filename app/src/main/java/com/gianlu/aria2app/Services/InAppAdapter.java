package com.gianlu.aria2app.Services;

import android.app.Activity;
import android.widget.ListView;

import com.gianlu.aria2app.DownloadsListing.ILoadDownloads;
import com.gianlu.aria2app.DownloadsListing.LoadDownloads;
import com.gianlu.aria2app.Main.IThread;
import com.gianlu.aria2app.Main.UpdateUI;
import com.gianlu.aria2app.Utils;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFrame;

import java.util.List;
import java.util.Map;

public class InAppAdapter extends WebSocketAdapter {
    private Activity context;
    private UpdateUI updater;
    private LoadDownloads loadDownloads;
    private ListView downloadsListView;
    private ILoadDownloads IloadDownloads;

    public InAppAdapter(Activity context, UpdateUI updater, LoadDownloads loadDownloads, ListView downloadsListView, ILoadDownloads IloadDownloads) {
        this.context = context;
        this.updater = updater;
        this.loadDownloads = loadDownloads;
        this.downloadsListView = downloadsListView;
        this.IloadDownloads = IloadDownloads;
    }

    @Override
    public void onFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
        if (updater != null) {
            updater.stop(new IThread() {
                @Override
                public void stopped() {
                    loadDownloads = new LoadDownloads(context, downloadsListView, IloadDownloads);
                    new Thread(loadDownloads).start();
                }
            });
        } else {
            loadDownloads = new LoadDownloads(context, downloadsListView, IloadDownloads);
            new Thread(loadDownloads).start();
        }
    }

    @Override
    public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
        Utils.UIToast(context, Utils.TOAST_MESSAGES.WS_OPENED);
    }

    @Override
    public void onConnectError(WebSocket websocket, WebSocketException exception) throws Exception {
        Utils.UIToast(context, Utils.TOAST_MESSAGES.WS_EXCEPTION, exception);
    }

    @Override
    public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
        Utils.UIToast(context, Utils.TOAST_MESSAGES.WS_CLOSED, cause);
    }

    @Override
    public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
        Utils.UIToast(context, Utils.TOAST_MESSAGES.WS_CLOSED, "Closed by server: " + closedByServer + "\nServer frame: " + serverCloseFrame + "\nClient frame: " + clientCloseFrame);
    }
}
