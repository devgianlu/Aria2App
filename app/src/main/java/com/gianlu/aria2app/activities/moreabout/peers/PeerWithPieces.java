package com.gianlu.aria2app.activities.moreabout.peers;

import com.gianlu.aria2app.api.aria2.Peer;

public class PeerWithPieces {
    public final Peer peer;
    public final int numPieces;

    public PeerWithPieces(Peer peer, int numPieces) {
        this.peer = peer;
        this.numPieces = numPieces;
    }
}
