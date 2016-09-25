package com.gianlu.aria2app.NetIO.JTA2;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

@SuppressWarnings("ConstantConditions")
public class Peer {
    public String peerId;
    public boolean amChoking;
    public boolean peerChoking;
    public int downloadSpeed;
    public int uploadSpeed;
    public boolean seeder;
    private String ip;
    private int port;
    private String bitfield;

    private Peer() {
    }

    @Nullable
    private static Integer parseInt(String val) {
        try {
            return Integer.parseInt(val);
        } catch (Exception ex) {
            return null;
        }
    }

    @NonNull
    private static Boolean parseBoolean(String val) {
        try {
            return Boolean.parseBoolean(val);
        } catch (Exception ex) {
            return false;
        }
    }

    public static Peer fromJSON(JSONObject jResult) {
        if (jResult == null) return null;

        Peer peer = new Peer();

        peer.peerId = jResult.optString("peerId");
        peer.ip = jResult.optString("ip");
        peer.port = parseInt(jResult.optString("port"));
        peer.bitfield = jResult.optString("bitfield");
        peer.amChoking = parseBoolean(jResult.optString("amChoking"));
        peer.peerChoking = parseBoolean(jResult.optString("peerChoking"));
        peer.downloadSpeed = parseInt(jResult.optString("downloadSpeed"));
        peer.uploadSpeed = parseInt(jResult.optString("uploadSpeed"));
        peer.seeder = parseBoolean(jResult.optString("seeder"));

        return peer;
    }

    public String getPeerId() {
        try {
            return URLDecoder.decode(peerId, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "Unknown";
        }
    }

    public String getFullAddress() {
        return ip + ":" + port;
    }
}
