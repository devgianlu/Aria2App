package com.gianlu.aria2app.NetIO.JTA2;

import java.util.List;

public interface IFiles {
    void onFiles(List<File> files);

    void onException(Exception exception);
}
