package com.gianlu.aria2app.MoreAboutDownload.FilesFragment;

import com.gianlu.aria2app.NetIO.JTA2.AFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class TreeDirectory {
    public final String incrementalPath;
    public final List<TreeDirectory> children;
    public final List<TreeFile> files;
    public final String name;
    public DirectoryViewHolder viewHolder;

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

    void addElement(String currentPath, String[] list, AFile file) {
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

    public Long getLength() {
        return doLengthSum(this);
    }

    public Long getCompletedLength() {
        return doCompletedLengthSum(this);
    }
}
