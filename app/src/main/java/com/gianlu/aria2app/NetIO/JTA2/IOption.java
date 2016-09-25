package com.gianlu.aria2app.NetIO.JTA2;

import java.util.Map;

public interface IOption {
    void onOptions(Map<String, String> options);
    void onException(Exception exception);
}
