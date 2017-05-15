package com.gianlu.aria2app.NetIO.JTA2;

import org.json.JSONObject;

import java.util.Comparator;
import java.util.Objects;

public class Peer {
    public final String peerId;
    public final boolean amChoking;
    public final boolean peerChoking;
    public final int downloadSpeed;
    public final int uploadSpeed;
    public final boolean seeder;
    public final String ip;
    public final int port;
    public final String bitfield;

    public Peer(JSONObject obj) {
        peerId = obj.optString("peerId", null);
        ip = obj.optString("ip", null);
        port = Integer.parseInt(obj.optString("port", "-1"));
        bitfield = obj.optString("bitfield", null);
        amChoking = Boolean.parseBoolean(obj.optString("amChoking", "false"));
        peerChoking = Boolean.parseBoolean(obj.optString("peerChoking", "false"));
        downloadSpeed = Integer.parseInt(obj.optString("downloadSpeed", "0"));
        uploadSpeed = Integer.parseInt(obj.optString("uploadSpeed", "0"));
        seeder = Boolean.parseBoolean(obj.optString("seeder", "false"));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Peer peer = (Peer) o;
        return peerId.equals(peer.peerId);
    }

    public static class DownloadSpeedComparator implements Comparator<Peer> {
        @Override
        public int compare(Peer o1, Peer o2) {
            if (Objects.equals(o1.downloadSpeed, o2.downloadSpeed)) return 0;
            else if (o1.downloadSpeed > o2.downloadSpeed) return -1;
            else return 1;
        }
    }

    public static class UploadSpeedComparator implements Comparator<Peer> {
        @Override
        public int compare(Peer o1, Peer o2) {
            if (Objects.equals(o1.uploadSpeed, o2.uploadSpeed)) return 0;
            else if (o1.uploadSpeed > o2.uploadSpeed) return -1;
            else return 1;
        }
    }
}
