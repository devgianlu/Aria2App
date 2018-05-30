package com.gianlu.aria2app.NetIO.Aria2;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class AriaDirectory {
    public static String SEPARATOR = "/";
    public final List<AriaDirectory> dirs;
    public final List<AriaFile> files;
    public final String path;
    public final AriaDirectory parent;
    public final String name;
    public final Set<Integer> indexes;
    private boolean allFilesSelected = true;
    private long length = 0;
    private long completedLength = 0;

    private AriaDirectory(@NonNull String path, @Nullable AriaDirectory parent) {
        this.path = path;
        this.parent = parent;
        this.dirs = new ArrayList<>();
        this.files = new ArrayList<>();
        this.indexes = new HashSet<>();

        if (path.length() == 1) {
            this.name = path;
        } else {
            int last = path.lastIndexOf(SEPARATOR);
            this.name = path.substring(last + 1);
        }
    }

    @NonNull
    public static AriaDirectory createRoot(@NonNull DownloadWithUpdate download, @NonNull AriaFiles files) {
        String path = download.update().dir;
        AriaDirectory dir = new AriaDirectory(SEPARATOR, null);
        for (AriaFile file : files) {
            dir.addElement("", file.path.substring(path.length() + 1).split(Pattern.quote(SEPARATOR)), file);
        }
        return dir;
    }

    public static void guessSeparator(@NonNull AriaFile file) {
        int slash = file.path.split("/").length;
        int backslash = file.path.split("\\\\").length;
        if (slash > backslash) SEPARATOR = "/";
        else SEPARATOR = "\\";
    }

    public long completedLength() {
        return completedLength;
    }

    public long length() {
        return length;
    }

    public boolean areAllFilesSelected() {
        return allFilesSelected;
    }

    private void addElement(String path, String[] split, @NonNull AriaFile child) {
        if (!child.selected) allFilesSelected = false;
        completedLength += child.completedLength;
        length += child.length;
        indexes.add(child.index);

        if (split.length == 1) {
            files.add(child);
        } else {
            AriaDirectory dir = getOrCreate(path + SEPARATOR + split[0]);
            dir.addElement(dir.path, Arrays.copyOfRange(split, 1, split.length), child);
        }
    }

    @NonNull
    private AriaDirectory getOrCreate(@NonNull String path) {
        for (AriaDirectory dir : dirs)
            if (dir.path.equals(path))
                return dir;

        AriaDirectory dir = new AriaDirectory(path, this);
        dirs.add(dir);
        return dir;
    }

    public boolean isRoot() {
        return parent == null;
    }

    public float getProgress() {
        return ((float) completedLength / (float) length) * 100;
    }

    private void allFiles(List<AriaFile> files) {
        files.addAll(this.files);
        for (AriaDirectory dir : dirs) files.addAll(dir.files);
    }

    @NonNull
    public List<AriaFile> allFiles() {
        List<AriaFile> files = new ArrayList<>();
        allFiles(files);
        return files;
    }

    @Nullable
    public AriaDirectory findDirectory(String path) {
        if (this.path.equals(path)) return this;

        for (AriaDirectory dir : dirs) {
            AriaDirectory find = dir.findDirectory(path);
            if (find != null) return find;
        }

        return null;
    }
}
