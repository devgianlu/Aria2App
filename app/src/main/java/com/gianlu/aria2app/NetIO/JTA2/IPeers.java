package com.gianlu.aria2app.NetIO.JTA2;

import java.util.List;

public interface IPeers {
    void onPeers(List<Peer> peers);

    void onException(Exception exception);
}
