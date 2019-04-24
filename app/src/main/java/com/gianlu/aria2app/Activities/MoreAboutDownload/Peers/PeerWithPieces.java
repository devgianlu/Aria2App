package com.gianlu.aria2app.Activities.MoreAboutDownload.Peers;

import com.gianlu.aria2app.NetIO.Aria2.Peer;

public class PeerWithPieces {
    public final Peer peer;
    public final int numPieces;

    public PeerWithPieces(Peer peer, int numPieces) {
        this.peer = peer;
        this.numPieces = numPieces;
    }
}
