package com.gianlu.aria2app.FileListing;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Directory {
    public String name;
    public FileNode node;

    private Long totalLengthCounter = 0L;
    private Long completedLengthCounter = 0L;
    private boolean allSelected = true;
    private List<Integer> subIndexes;

    public Directory(String name, FileNode node) {
        this.name = name;
        this.node = node;

        doSum(node);

        subIndexes = new ArrayList<>();
        getIndexes(node);

        areSelected(node);
    }

    private void doSum(FileNode node) {
        for (FileNode file : node.getLeafs()) {
            totalLengthCounter += file.getFile().length;
            completedLengthCounter += file.getFile().completedLength;
        }

        for (FileNode dir : node.getChildren()) {
            doSum(dir);
        }
    }

    private void areSelected(FileNode node) {
        for (FileNode file : node.getLeafs()) {
            if (!file.file.selected) allSelected = false;
        }

        for (FileNode dir : node.getChildren()) {
            areSelected(dir);
        }
    }

    private void getIndexes(FileNode node) {
        for (FileNode file : node.getLeafs()) {
            subIndexes.add(file.file.index);
        }

        for (FileNode dir : node.getChildren()) {
            getIndexes(dir);
        }
    }

    public Long getTotalLength() {
        return totalLengthCounter;
    }

    public Long getCompletedLength() {
        return completedLengthCounter;
    }

    public Float getProgress() {
        return completedLengthCounter.floatValue() / totalLengthCounter.floatValue() * 100;
    }

    public String getPercentage() {
        return String.format(Locale.getDefault(), "%.2f", getProgress()) + " %";
    }

    public boolean areAllSelected() {
        return allSelected;
    }

    public List<Integer> getSubIndexes() {
        return subIndexes;
    }
}
