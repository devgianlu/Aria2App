package com.gianlu.jtitan.Aria2Helper;

public interface IStats {
    void onStats(GlobalStats stats);

    void onException(Exception exception);
}
