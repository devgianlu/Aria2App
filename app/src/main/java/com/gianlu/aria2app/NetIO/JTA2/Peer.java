package com.gianlu.aria2app.NetIO.JTA2;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

@SuppressWarnings("ConstantConditions")
public class Peer {
    public final String peerId;
    public final boolean amChoking;
    public final boolean peerChoking;
    public final int downloadSpeed;
    public final int uploadSpeed;
    public final boolean seeder;
    private final String ip;
    private final int port;
    private final String bitfield;

    public Peer(JSONObject obj) {
        peerId = obj.optString("peerId");
        ip = obj.optString("ip");
        port = Integer.parseInt(obj.optString("port", "-1"));
        bitfield = obj.optString("bitfield");
        amChoking = Boolean.parseBoolean(obj.optString("amChoking", "false"));
        peerChoking = Boolean.parseBoolean(obj.optString("peerChoking", "false"));
        downloadSpeed = Integer.parseInt(obj.optString("downloadSpeed", "0"));
        uploadSpeed = Integer.parseInt(obj.optString("uploadSpeed", "0"));
        seeder = Boolean.parseBoolean(obj.optString("seeder", "false"));
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
