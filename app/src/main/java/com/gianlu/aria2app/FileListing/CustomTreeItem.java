package com.gianlu.aria2app.FileListing;

public class CustomTreeItem {
    public TYPE type;
    public Object file;

    public CustomTreeItem(Object file, TYPE type) {
        this.file = file;
        this.type = type;
    }

    public enum TYPE {
        FOLDER,
        FILE
    }
}
