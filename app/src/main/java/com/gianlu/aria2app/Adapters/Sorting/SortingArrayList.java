package com.gianlu.aria2app.Adapters.Sorting;

import android.util.Pair;

import com.gianlu.aria2app.NetIO.JTA2.Download;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SortingArrayList extends ArrayList<Download> {
    private SortBy currentSort = SortBy.STATUS;

    public SortingArrayList(List<Download> objs) {
        super(objs);
    }

    public void sort(SortBy sortBy) {
        currentSort = sortBy;

        switch (sortBy) {
            case STATUS:
                Collections.sort(this, new Download.StatusComparator());
                break;
            case PROGRESS:
                Collections.sort(this, new Download.ProgressComparator());
                break;
            case DOWNLOAD_SPEED:
                Collections.sort(this, new Download.DownloadSpeedComparator());
                break;
            case UPLOAD_SPEED:
                Collections.sort(this, new Download.UploadSpeedComparator());
                break;
            case COMPLETED_LENGTH:
                Collections.sort(this, new Download.CompletedLengthComparator());
                break;
            case LENGTH:
                Collections.sort(this, new Download.LengthComparator());
                break;
        }
    }

    public Pair<Integer, Integer> addAndSort(Download element) {
        int from = indexOf(element);
        if (from == -1) add(element);
        else set(from, element);
        sort(currentSort);
        int to = indexOf(element);
        return new Pair<>(from, to);
    }

    public enum SortBy {
        STATUS,
        PROGRESS,
        DOWNLOAD_SPEED,
        UPLOAD_SPEED,
        COMPLETED_LENGTH,
        LENGTH
    }
}
