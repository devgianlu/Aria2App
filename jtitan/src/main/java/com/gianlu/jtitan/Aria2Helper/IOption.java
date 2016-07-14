package com.gianlu.jtitan.Aria2Helper;

import java.util.Map;

public interface IOption {
    void onOptions(Map<String, String> options);

    void onException(Exception exception);
}
