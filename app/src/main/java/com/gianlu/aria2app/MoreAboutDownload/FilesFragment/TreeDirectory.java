package com.gianlu.aria2app.MoreAboutDownload.FilesFragment;

import android.annotation.SuppressLint;

import com.gianlu.aria2app.NetIO.JTA2.File;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

class TreeDirectory {
    private final String incrementalPath;
    private final List<TreeDirectory> children;
    private final List<TreeFile> files;
    private final String name;
    DirectoryViewHolder viewHolder;

    private TreeDirectory(String name, String incrementalPath) {
        children = new ArrayList<>();
        files = new ArrayList<>();
        this.name = name;
        this.incrementalPath = incrementalPath;
    }

    static TreeDirectory root() {
        return new TreeDirectory("", "");
    }

    private static int indexOf(List<TreeDirectory> nodes, TreeDirectory node) {
        for (int i = 0; i < nodes.size(); i++)
            if (areEquals(nodes.get(i), node)) return i;

        return -1;
    }

    private static boolean areEquals(TreeDirectory first, TreeDirectory second) {
        return Objects.equals(first.incrementalPath, second.incrementalPath) && Objects.equals(first.name, second.name);
    }

    private static Long doLengthSum(TreeDirectory parent) {
        Long length = 0L;

        for (TreeFile file : parent.files) {
            length += file.file.length;
        }

        for (TreeDirectory child : parent.children) {
            length += doLengthSum(child);
        }

        return length;
    }

    private static Long doCompletedLengthSum(TreeDirectory parent) {
        Long completedLength = 0L;

        for (TreeFile file : parent.files) {
            completedLength += file.file.completedLength;
        }

        for (TreeDirectory child : parent.children) {
            completedLength += doLengthSum(child);
        }

        return completedLength;
    }

    void addElement(String currentPath, String[] list, File file) {
        if (list.length == 0) return;

        while (list[0] == null || list[0].isEmpty()) {
            list = Arrays.copyOfRange(list, 1, list.length);
            if (list.length == 0) return;
        }

        if (list.length == 1) {
            files.add(new TreeFile(file));
        } else {
            TreeDirectory currentChild = new TreeDirectory(list[0], currentPath + "/" + list[0]);

            int index = indexOf(children, currentChild);
            if (index == -1) {
                children.add(currentChild);
                currentChild.addElement(currentChild.incrementalPath, Arrays.copyOfRange(list, 1, list.length), file);
            } else {
                TreeDirectory nextChild = children.get(index);
                nextChild.addElement(currentChild.incrementalPath, Arrays.copyOfRange(list, 1, list.length), file);
            }
        }
    }

    @SuppressWarnings("LoopStatementThatDoesntLoop")
    TreeFile findFile(String path) {
        for (TreeDirectory dir : children) {
            return dir.findFile(path);
        }

        for (TreeFile file : files) {
            if (Objects.equals(file.file.path, path))
                return file;
        }

        return null;
    }

    String getIncrementalPath() {
        return incrementalPath;
    }

    List<TreeDirectory> getChildren() {
        return children;
    }

    public List<TreeFile> getFiles() {
        return files;
    }

    public String getName() {
        return name;
    }

    private Long getLength() {
        return doLengthSum(this);
    }

    private Long getCompletedLength() {
        return doCompletedLengthSum(this);
    }

    private Float getProgress() {
        return getCompletedLength().floatValue() / getLength().floatValue() * 100;
    }

    @SuppressLint("unused")
    public String getPercentage() {
        return String.format(Locale.getDefault(), "%.2f", getProgress()) + " %";
    }
}
