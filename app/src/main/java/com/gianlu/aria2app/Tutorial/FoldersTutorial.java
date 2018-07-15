package com.gianlu.aria2app.Tutorial;

import android.graphics.Rect;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;

import com.gianlu.aria2app.Activities.MoreAboutDownload.Files.FilesAdapter;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Tutorial.BaseTutorial;

public class FoldersTutorial extends BaseTutorial {

    @Keep
    public FoldersTutorial() {
        super(Discovery.FOLDERS);
    }

    public final boolean buildSequence(@NonNull RecyclerView list) {
        RecyclerView.ViewHolder holder = list.findViewHolderForLayoutPosition(0);
        if (holder != null) {
            list.scrollToPosition(0);

            Rect rect = new Rect();
            holder.itemView.getGlobalVisibleRect(rect);
            rect.offset((int) -(holder.itemView.getWidth() * 0.3), 0);

            forBounds(rect, R.string.folderDetails, R.string.folderDetails_desc)
                    .tintTarget(false)
                    .transparentTarget(true);

            return true;
        }

        return false;
    }

    public final boolean canShow(Fragment fragment, FilesAdapter adapter) {
        return fragment != null && CommonUtils.isVisible(fragment) && adapter != null && adapter.getCurrentDir() != null && adapter.getCurrentDir().dirs.size() >= 1;
    }
}
