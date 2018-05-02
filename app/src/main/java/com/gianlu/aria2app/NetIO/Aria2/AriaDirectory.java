package com.gianlu.aria2app.NetIO.Aria2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class AriaDirectory extends TreeNode {
    public final long totalLength;
    public final long completedLength;
    public final List<Integer> indexes;
    public final String fullPath;

    public AriaDirectory(TreeNode node, DownloadWithUpdate download) {
        super(download, node);

        if (node.isFile()) throw new IllegalArgumentException("node is a file!");

        totalLength = calcTotalLength(this, new AtomicLong(0));
        completedLength = calcCompletedLength(this, new AtomicLong(0));

        indexes = new ArrayList<>();
        findIndexes(indexes, this);
        Collections.sort(indexes);

        fullPath = download.update().dir + node.incrementalPath;
    }

    public static void update(TreeNode node, AriaFiles files) {
        for (TreeNode file : node.files) file.update(files.opt(file.obj));
        for (TreeNode dir : node.dirs) update(dir, files);
    }

    private static long calcCompletedLength(TreeNode parent, AtomicLong sum) {
        for (TreeNode file : parent.files) sum.addAndGet(file.obj.completedLength);
        for (TreeNode dir : parent.dirs) calcCompletedLength(dir, sum);
        return sum.get();
    }

    private static long calcTotalLength(TreeNode parent, AtomicLong sum) {
        for (TreeNode file : parent.files) sum.addAndGet(file.obj.length);
        for (TreeNode dir : parent.dirs) calcTotalLength(dir, sum);
        return sum.get();
    }

    private static void findIndexes(List<Integer> indexes, TreeNode parent) {
        for (TreeNode file : parent.files) indexes.add(file.obj.index);
        for (TreeNode dir : parent.dirs) findIndexes(indexes, dir);
    }

    public AriaDirectory update(DownloadWithUpdate download, AriaFiles files) {
        update(this, files);
        return new AriaDirectory(this, download);
    }

    public float getProgress() {
        return ((float) completedLength) / ((float) totalLength) * 100;
    }
}
