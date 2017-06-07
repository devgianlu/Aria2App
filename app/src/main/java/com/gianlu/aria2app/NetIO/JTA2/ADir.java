package com.gianlu.aria2app.NetIO.JTA2;

import com.gianlu.aria2app.Activities.MoreAboutDownload.Files.TreeNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ADir extends TreeNode {
    public final long totalLength;
    public final long completedLength;
    public final List<Integer> indexes;
    public final String fullPath;

    public ADir(TreeNode node, Download download) {
        super(node);

        if (node.isFile()) throw new IllegalArgumentException("node is a file!");

        totalLength = calcTotalLength(this, 0);
        completedLength = calcCompletedLength(this, 0);

        indexes = new ArrayList<>();
        findIndexes(indexes, this);
        Collections.sort(indexes);

        fullPath = download.dir + node.incrementalPath;
    }

    public static void update(TreeNode node, List<AFile> files) {
        for (TreeNode file : node.files) file.update(AFile.find(files, file.obj));
        for (TreeNode dir : node.dirs) update(dir, files);
    }

    private static boolean allSelected(TreeNode node) {
        for (TreeNode file : node.files)
            if (!file.obj.selected)
                return false;

        for (TreeNode dir : node.dirs)
            if (!allSelected(dir))
                return false;

        return true;
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

    public ADir update(Download download, List<AFile> files) {
        update(this, files);
        return new ADir(this, download);
    }

    public boolean allSelected() {
        return allSelected(this);
    }

    public List<AFile> objs() {
        List<AFile> objs = new ArrayList<>();
        for (TreeNode file : files) objs.add(file.obj);
        return objs;
    }

    public float getProgress() {
        return ((float) completedLength) / ((float) totalLength) * 100;
    }
}
