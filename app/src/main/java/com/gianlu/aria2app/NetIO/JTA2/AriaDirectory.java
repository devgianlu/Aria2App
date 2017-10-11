package com.gianlu.aria2app.NetIO.JTA2;

import com.gianlu.aria2app.Activities.MoreAboutDownload.Files.TreeNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AriaDirectory extends TreeNode {
    public final long totalLength;
    public final long completedLength;
    public final List<Integer> indexes;
    public final String fullPath;

    public AriaDirectory(TreeNode node, Download download) {
        super(node);

        if (node.isFile()) throw new IllegalArgumentException("node is a file!");

        totalLength = calcTotalLength(this, 0);
        completedLength = calcCompletedLength(this, 0);

        indexes = new ArrayList<>();
        findIndexes(indexes, this);
        Collections.sort(indexes);

        fullPath = download.dir + node.incrementalPath;
    }

    public static void update(TreeNode node, List<AriaFile> files) {
        for (TreeNode file : node.files) file.update(AriaFile.find(files, file.obj));
        for (TreeNode dir : node.dirs) update(dir, files);
    }

    private static long calcCompletedLength(TreeNode parent, long sum) {
        for (TreeNode file : parent.files) sum += file.obj.completedLength;
        for (TreeNode dir : parent.dirs) calcCompletedLength(dir, sum);
        return sum;
    }

    private static long calcTotalLength(TreeNode parent, long sum) {
        for (TreeNode file : parent.files) sum += file.obj.length;
        for (TreeNode dir : parent.dirs) calcTotalLength(dir, sum);
        return sum;
    }

    private static void findIndexes(List<Integer> indexes, TreeNode parent) {
        for (TreeNode file : parent.files) indexes.add(file.obj.index);
        for (TreeNode dir : parent.dirs) findIndexes(indexes, dir);
    }

    public AriaDirectory update(Download download, List<AriaFile> files) {
        update(this, files);
        return new AriaDirectory(this, download);
    }

    public float getProgress() {
        return ((float) completedLength) / ((float) totalLength) * 100;
    }
}
