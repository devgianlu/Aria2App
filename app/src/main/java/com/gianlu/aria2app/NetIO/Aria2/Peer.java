package com.gianlu.aria2app.NetIO.Aria2;

import androidx.annotation.Nullable;

import com.gianlu.aria2app.NetIO.PeerIdParser;
import com.gianlu.commonutils.Adapters.Filterable;
import com.gianlu.commonutils.Adapters.NotFilterable;
import com.gianlu.commonutils.Logging;

import org.json.JSONObject;

import java.util.Comparator;
import java.util.Objects;

public class Peer implements Filterable<NotFilterable> {
    public final boolean amChoking;
    public final boolean peerChoking;
    public final int downloadSpeed;
    public final int uploadSpeed;
    public final boolean seeder;
    public final String ip;
    public final int port;
    public final String bitfield;
    private final String peerId;

    public Peer(JSONObject obj) {
        peerId = obj.optString("peerId", null);
        ip = obj.optString("ip", null);
        port = obj.optInt("port", -1);
        bitfield = obj.optString("bitfield", null);
        amChoking = obj.optBoolean("amChoking", false);
        peerChoking = obj.optBoolean("peerChoking", false);
        downloadSpeed = obj.optInt("downloadSpeed", 0);
        uploadSpeed = obj.optInt("uploadSpeed", 0);
        seeder = obj.optBoolean("seeder", false);
    }

    @Nullable
    public PeerIdParser.Parsed peerId() {
        try {
            return PeerIdParser.parse(peerId);
        } catch (Exception ex) {
            Logging.log("Failed parsing peer id: " + peerId, ex);
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Peer peer = (Peer) o;
        return port == peer.port && Objects.equals(ip, peer.ip);
    }

    @Override
    public NotFilterable getFilterable() {
        return new NotFilterable();
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
