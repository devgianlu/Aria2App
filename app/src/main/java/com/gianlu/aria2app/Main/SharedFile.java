package com.gianlu.aria2app.Main;


import java.io.File;

public class SharedFile {
    public final File file;
    public final String mimeType;

    public SharedFile(File file, String mimeType) {
        this.file = file;
        this.mimeType = mimeType;
    }
}
