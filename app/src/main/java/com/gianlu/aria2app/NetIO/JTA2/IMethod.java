package com.gianlu.aria2app.NetIO.JTA2;

import java.util.List;

public interface IMethod {
    void onMethods(List<String> methods);

    void onException(Exception ex);
}
