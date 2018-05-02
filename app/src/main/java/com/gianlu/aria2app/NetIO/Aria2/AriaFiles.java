package com.gianlu.aria2app.NetIO.Aria2;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class AriaFiles extends ArrayList<AriaFile> {

    public AriaFiles(JSONArray array) throws JSONException {
        for (int i = 0; i < array.length(); i++)
            add(new AriaFile(array.getJSONObject(i)));
    }

    private AriaFiles(List<AriaFile> files) {
        addAll(files);
    }

    private AriaFiles() {
    }

    @NonNull
    public static AriaFiles singleton(AriaFile file) {
        AriaFiles files = new AriaFiles();
        files.add(file);
        return files;
    }


    @NonNull
    public static AriaFiles fromDirectory(TreeNode dir) {
        AriaFiles files = new AriaFiles();
        files.addAll(dir.allObjs());
        for (TreeNode node : dir.dirs) files.addAll(node.allObjs());
        return files;
    }

    @NonNull
    public static AriaFiles fromFiles(List<TreeNode> nodes) {
        AriaFiles files = new AriaFiles();
        for (TreeNode node : nodes) files.add(node.obj);
        return files;
    }

    @NonNull
    public AriaFiles copy() {
        return new AriaFiles(this);
    }

    @NonNull
    public AriaFile opt(@NonNull AriaFile obj) {
        for (AriaFile file : this)
            if (file.equals(obj))
                return file;

        return obj;
    }

    @Nullable
    public AriaFile findFileIndex(int index) {
        for (AriaFile file : this)
            if (file.index == index)
                return file;

        return null;
    }
}
