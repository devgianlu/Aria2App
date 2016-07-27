package com.gianlu.aria2app.FileListing;

import com.gianlu.aria2app.NetIO.JTA2.File;
import com.unnamed.b.atv.model.TreeNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileNode {
    public String incrementalPath;
    public File file;
    private List<FileNode> children;
    private List<FileNode> leafs;
    private String data;

    public FileNode(String nodeValue, String incrementalPath, File file) {
        children = new ArrayList<>();
        leafs = new ArrayList<>();
        data = nodeValue;
        this.incrementalPath = incrementalPath;
        this.file = file;
    }

    public boolean isLeaf() {
        return children.isEmpty() && leafs.isEmpty();
    }

    public void addElement(String currentPath, String[] list, File file) {
        while (list[0] == null || list[0].equals(""))
            list = Arrays.copyOfRange(list, 1, list.length);

        FileNode currentChild = new FileNode(list[0], currentPath + "/" + list[0], file);
        if (list.length == 1) {
            leafs.add(currentChild);
        } else {
            int index = children.indexOf(currentChild);
            if (index == -1) {
                children.add(currentChild);
                currentChild.addElement(currentChild.incrementalPath, Arrays.copyOfRange(list, 1, list.length), file);
            } else {
                FileNode nextChild = children.get(index);
                nextChild.addElement(currentChild.incrementalPath, Arrays.copyOfRange(list, 1, list.length), file);
            }
        }
    }

    public TreeNode toTreeNode(String parentPath, TreeNode parent) {
        TreeNode node;

        if (isLeaf()) {
            node = new TreeNode(new CustomTreeItem(file, CustomTreeItem.TYPE.FILE));
        } else {
            node = new TreeNode(new CustomTreeItem(new Directory(incrementalPath.replace(parentPath, ""), this), CustomTreeItem.TYPE.FOLDER));
        }

        if (parentPath.equals("/")) {
            for (FileNode n : children) n.toTreeNode(incrementalPath + "/", parent);
            for (FileNode n : leafs) n.toTreeNode(incrementalPath, parent);
        } else {
            for (FileNode n : children) n.toTreeNode(incrementalPath + "/", node);
            for (FileNode n : leafs) n.toTreeNode(incrementalPath, node);

            parent.addChild(node);
        }

        return parent;
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object obj) {
        FileNode cmpObj = (FileNode) obj;
        return this.incrementalPath.equals(cmpObj.incrementalPath) && this.data.equals(cmpObj.data);
    }

    @Override
    public String toString() {
        return data;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public List<FileNode> getChildren() {
        return children;
    }

    public List<FileNode> getLeafs() {
        return leafs;
    }
}
