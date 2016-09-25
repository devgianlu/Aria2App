package com.gianlu.aria2app.MoreAboutDownload.FilesFragment;

import com.gianlu.aria2app.NetIO.JTA2.File;

import java.util.List;

class Tree {
    private static final String separator = "/";

    private final TreeDirectory root;
    private TreeDirectory commonRoot;

    private Tree(TreeDirectory root) {
        this.root = root;
        commonRoot = null;
    }

    static Tree newTree() {
        return new Tree(TreeDirectory.root());
    }

    private void addElement(File element) {
        String[] list = element.path.split(Tree.separator);
        root.addElement(root.getIncrementalPath(), list, element);
    }

    Tree addElements(List<File> elements) {
        if (elements.isEmpty()) return this;

        for (File e : elements) {
            addElement(e);
        }

        return this;
    }

    TreeDirectory getCommonRoot() {
        if (commonRoot != null) {
            return commonRoot;
        } else {
            TreeDirectory curr = root;

            while (curr.getFiles().size() <= 0) {
                if (curr.getChildren().size() == 0) return null;
                curr = curr.getChildren().get(0);
            }

            commonRoot = curr;
            return commonRoot;
        }
    }

    TreeFile findFile(String path) {
        if (getCommonRoot() == null) return null;
        return getCommonRoot().findFile(path);
    }
}
