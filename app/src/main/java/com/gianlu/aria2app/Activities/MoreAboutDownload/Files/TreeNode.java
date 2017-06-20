package com.gianlu.aria2app.Activities.MoreAboutDownload.Files;

import com.gianlu.aria2app.NetIO.JTA2.AFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class TreeNode {
    private static String SEPARATOR = "/";
    public final List<TreeNode> files;
    public final List<TreeNode> dirs;
    public final String incrementalPath;
    public final String name;
    public final TreeNode parent;
    public AFile obj;

    public TreeNode(TreeNode parent, String incrementalPath, AFile obj) {
        this.parent = parent;
        this.incrementalPath = incrementalPath;
        this.obj = obj;
        this.files = null;
        this.dirs = null;
        this.name = obj.getName();
    }

    private TreeNode(TreeNode parent, String name, String incrementalPath) {
        this.parent = parent;
        this.dirs = new ArrayList<>();
        this.files = new ArrayList<>();
        this.name = name;
        this.obj = null;
        this.incrementalPath = incrementalPath;
    }

    public TreeNode(TreeNode node) {
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

    public static void guessSeparator(String path) {
        if (path.contains("/")) SEPARATOR = "/";
        else if (path.contains("\\")) SEPARATOR = "\\";
        else SEPARATOR = "/";
    }

    public static TreeNode create(List<AFile> files, String commonRoot) {
        TreeNode rootNode = new TreeNode(null, SEPARATOR, "");
        for (AFile file : files) rootNode.addElement(file, commonRoot);
        return rootNode;
    }

    public boolean allSelected() {
        return allSelected(this);
    }

    public List<AFile> allObjs() {
        if (isFile()) return Collections.singletonList(obj);
        List<AFile> objs = new ArrayList<>();
        objs.addAll(objs());
        for (TreeNode dir : dirs) objs.addAll(dir.allObjs());
        return objs;
    }

    public List<AFile> objs() {
        if (isFile()) return Collections.singletonList(obj);
        List<AFile> objs = new ArrayList<>();
        for (TreeNode file : files) objs.add(file.obj);
        return objs;
    }

    private TreeNode root() {
        if (isRoot()) return this;
        return parent.root();
    }

    public void updateHierarchy(List<AFile> objs) {
        TreeNode root = root();
        root.updateOrFall(new ArrayList<>(objs));
    }

    private void updateOrFall(List<AFile> newFiles) {
        if (isFile()) {
            update(AFile.find(newFiles, this.obj));
            return;
        }

        for (TreeNode file : files) {
            List<AFile> toRemove = new ArrayList<>();
            for (AFile newFile : newFiles) {
                if (file.obj.equals(newFile)) {
                    file.obj = newFile;
                    toRemove.add(newFile);
                }
            }

            newFiles.removeAll(toRemove);
        }

        for (TreeNode dir : dirs) dir.updateOrFall(newFiles);
    }

    public void update(AFile file) {
        this.obj = file;
    }

    public int indexOfObj(AFile obj) {
        if (isFile()) return -1;
        for (int i = 0; i < files.size(); i++) {
            if (Objects.equals(files.get(i).obj, obj))
                return i;
        }

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

    public void addElement(AFile element, String commonRoot) {
        if (element.path.isEmpty()) return;
        String[] list = element.path.replace(commonRoot, "").split(SEPARATOR == "/" ? SEPARATOR : ("\\u005C" + SEPARATOR + "\\u005C"));
        addElement(incrementalPath, list, element);
    }

    public void addElement(String currentPath, String[] list, AFile file) {
        while (list[0] == null || list[0].isEmpty())
            list = Arrays.copyOfRange(list, 1, list.length);

        if (list.length == 1) {
            files.add(new TreeNode(this, currentPath, file));
        } else {
            TreeNode currentChild = new TreeNode(this, list[0], currentPath + SEPARATOR + list[0]);

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
