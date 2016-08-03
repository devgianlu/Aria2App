package com.gianlu.aria2app.NetIO.JTA2;

import java.util.List;
import java.util.Map;

public interface IServers {
    void onServers(Map<Integer, List<Server>> servers);

    void onException(Exception exception);

    void onDownloadNotActive(Exception exception);
}
