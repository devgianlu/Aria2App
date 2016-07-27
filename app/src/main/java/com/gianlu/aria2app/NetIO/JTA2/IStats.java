package com.gianlu.aria2app.NetIO.JTA2;

public interface IStats {
    void onStats(GlobalStats stats);

    void onException(Exception exception);
}
