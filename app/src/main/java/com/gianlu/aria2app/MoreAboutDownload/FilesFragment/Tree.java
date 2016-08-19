package com.gianlu.aria2app.MoreAboutDownload.FilesFragment;

import com.gianlu.aria2app.NetIO.JTA2.File;

import java.util.List;

public class Tree {
    public static String separator = "/";

    private TreeDirectory root;
    private TreeDirectory commonRoot;

    private Tree(TreeDirectory root) {
        this.root = root;
        commonRoot = null;
    }

    public static Tree newTree() {
        return new Tree(TreeDirectory.root());
    }

    public void addElement(File element) {
        String[] list = element.path.split(Tree.separator);
        root.addElement(root.getIncrementalPath(), list, element);
    }

    public Tree addElements(List<File> elements) {
        for (File e : elements) {
            addElement(e);
        }

        return this;
    }

    public TreeDirectory getCommonRoot() {
        if (commonRoot != null) {
            return commonRoot;
        } else {
            TreeDirectory curr = root;
            while (curr.getFiles().size() <= 0) {
                curr = curr.getChildren().get(0);
            }

            commonRoot = curr;
            return commonRoot;
        }
    }

    public TreeFile findFile(String path) {
        return getCommonRoot().findFile(path);
    }
}
