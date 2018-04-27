package com.gianlu.aria2app.NetIO.Aria2;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class TreeNode extends DownloadChild {
    public final List<TreeNode> files;
    public final List<TreeNode> dirs;
    public final String incrementalPath;
    public final String name;
    public final TreeNode parent;
    private final String separator;
    public AriaFile obj;

    private TreeNode(DownloadWithUpdate download, TreeNode parent, String incrementalPath, AriaFile obj) {
        super(download);
        this.separator = TreeNode.guessSeparator(download.update().dir);
        this.parent = parent;
        this.incrementalPath = incrementalPath;
        this.obj = obj;
        this.files = null;
        this.dirs = null;
        this.name = obj.getName();
    }

    private TreeNode(DownloadWithUpdate download, TreeNode parent, String name, String incrementalPath) {
        super(download);
        this.separator = TreeNode.guessSeparator(download.update().dir);
        this.parent = parent;
        this.dirs = new ArrayList<>();
        this.files = new ArrayList<>();
        this.name = name;
        this.obj = null;
        this.incrementalPath = incrementalPath;
    }

    TreeNode(DownloadWithUpdate download, TreeNode node) {
        super(download);
        this.separator = TreeNode.guessSeparator(download.update().dir);
        this.files = node.files;
        this.dirs = node.dirs;
        this.obj = node.obj;
        this.incrementalPath = node.incrementalPath;
        this.name = node.name;
        this.parent = node.parent;
    }

    private static boolean allSelected(TreeNode node) {
        if (node.isFile()) return node.obj.selected;

        for (TreeNode file : node.files)
            if (!file.obj.selected)
                return false;

        for (TreeNode dir : node.dirs)
            if (!allSelected(dir))
                return false;

        return true;
    }

    private static String guessSeparator(String path) {
        if (path.contains("/")) return "/";
        else if (path.contains("\\")) return "\\";
        else return "/";
    }

    public static TreeNode create(DownloadWithUpdate download, List<AriaFile> files) {
        String dir = download.update().dir;
        TreeNode rootNode = new TreeNode(download, null, TreeNode.guessSeparator(dir), "");
        for (AriaFile file : files) rootNode.addElement(file, dir);
        return rootNode;
    }

    public boolean allSelected() {
        return allSelected(this);
    }

    public List<AriaFile> allObjs() {
        if (isFile()) return Collections.singletonList(obj);
        List<AriaFile> objs = new ArrayList<>(objs());
        for (TreeNode dir : dirs) objs.addAll(dir.allObjs());
        return objs;
    }

    public Integer[] allIndexes() {
        if (isFile()) return new Integer[]{obj.index};
        return AriaFile.allIndexes(allObjs());
    }

    public List<AriaFile> objs() {
        if (isFile()) return Collections.singletonList(obj);
        List<AriaFile> objs = new ArrayList<>();
        for (TreeNode file : files) objs.add(file.obj);
        return objs;
    }

    private TreeNode root() {
        if (isRoot()) return this;
        return parent.root();
    }

    public void updateHierarchy(List<AriaFile> objs) {
        TreeNode root = root();
        root.updateOrFall(new ArrayList<>(objs));
    }

    private void updateOrFall(List<AriaFile> newFiles) {
        if (isFile()) {
            update(AriaFile.find(newFiles, this.obj));
            return;
        }

        for (TreeNode file : files) {
            List<AriaFile> toRemove = new ArrayList<>();
            for (AriaFile newFile : newFiles) {
                if (file.obj.equals(newFile)) {
                    file.obj = newFile;
                    toRemove.add(newFile);
                }
            }

            newFiles.removeAll(toRemove);
        }

        for (TreeNode dir : dirs) dir.updateOrFall(newFiles);
    }

    public void update(@NonNull AriaFile file) {
        this.obj = file;
    }

    public int indexOfObj(AriaFile obj) {
        if (isFile()) return -1;

        for (int i = 0; i < files.size(); i++)
            if (Objects.equals(files.get(i).obj, obj))
                return i;

        return -1;
    }

    public boolean isRoot() {
        return parent == null;
    }

    public int indexOfDir(TreeNode node) {
        if (isFile()) return -1;
        for (int i = 0; i < dirs.size(); i++)
            if (Objects.equals(dirs.get(i), node))
                return i;

        return -1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TreeNode treeNode = (TreeNode) o;
        return incrementalPath.equals(treeNode.incrementalPath) && name.equals(treeNode.name);
    }

    private void addElement(AriaFile element, String commonRoot) {
        if (element.path.isEmpty()) return;
        String[] list = element.path.replace(commonRoot, "").split(Objects.equals(separator, "/") ? separator : ("\\u005C" + separator + "\\u005C"));
        addElement(incrementalPath, list, element);
    }

    private void addElement(String currentPath, String[] list, AriaFile file) {
        while (list[0] == null || list[0].isEmpty())
            list = Arrays.copyOfRange(list, 1, list.length);

        if (list.length == 1) {
            files.add(new TreeNode(download, this, currentPath, file));
        } else {
            TreeNode currentChild = new TreeNode(download, this, list[0], currentPath + separator + list[0]);

            int index = indexOfDir(currentChild);
            if (index == -1) {
                dirs.add(currentChild);
                currentChild.addElement(currentChild.incrementalPath, Arrays.copyOfRange(list, 1, list.length), file);
            } else {
                TreeNode nextChild = dirs.get(index);
                nextChild.addElement(currentChild.incrementalPath, Arrays.copyOfRange(list, 1, list.length), file);
            }
        }
    }

    public boolean isFile() {
        return files == null || dirs == null;
    }
}
