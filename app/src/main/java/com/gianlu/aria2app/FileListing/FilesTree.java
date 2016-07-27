package com.gianlu.aria2app.FileListing;

import com.gianlu.aria2app.NetIO.JTA2.File;
import com.unnamed.b.atv.model.TreeNode;

public class FilesTree {
    private FileNode root;
    private FileNode commonRoot;

    public FilesTree(FileNode root) {
        this.root = root;
        commonRoot = null;
    }

    public void addElement(File elementValue) {
        String[] list = elementValue.path.split("/");

        root.addElement(root.incrementalPath, list, elementValue);
    }

    public TreeNode toTreeNode() {
        getCommonRoot();
        return commonRoot.toTreeNode("/", TreeNode.root());
    }

    public FileNode getCommonRoot() {
        if (commonRoot != null)
            return commonRoot;
        else {
            FileNode current = root;
            while (current.getLeafs().size() <= 0) {
                current = current.getChildren().get(0);
            }
            commonRoot = current;
            return commonRoot;
        }
    }
}
