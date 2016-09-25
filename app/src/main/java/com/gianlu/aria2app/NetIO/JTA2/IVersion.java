package com.gianlu.aria2app.NetIO.JTA2;

import java.util.List;

public interface IVersion {
    void onVersion(List<String> rawFeatures, String version);

    void onException(Exception exception);
}
