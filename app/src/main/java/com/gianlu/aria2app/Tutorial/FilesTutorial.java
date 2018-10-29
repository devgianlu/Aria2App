package com.gianlu.aria2app.Tutorial;

import android.graphics.Rect;

import com.gianlu.aria2app.Activities.MoreAboutDownload.Files.FilesAdapter;
import com.gianlu.aria2app.NetIO.Aria2.AriaDirectory;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Tutorial.BaseTutorial;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

public class FilesTutorial extends BaseTutorial {

    @Keep
    public FilesTutorial() {
        super(Discovery.FILES);
    }

    public final boolean buildSequence(@NonNull RecyclerView list, @Nullable AriaDirectory dir) {
        int firstFile = dir == null ? 0 : dir.dirs.size();
        RecyclerView.ViewHolder holder = list.findViewHolderForLayoutPosition(firstFile);
        if (holder != null) {
            list.scrollToPosition(firstFile);

            Rect rect = new Rect();
            holder.itemView.getGlobalVisibleRect(rect);
            rect.offset((int) -(holder.itemView.getWidth() * 0.3), 0);

            forBounds(rect, R.string.fileDetails, R.string.fileDetails_desc)
                    .tintTarget(false)
                    .transparentTarget(true);

            return true;
        }

        return false;
    }

    public final boolean canShow(Fragment fragment, FilesAdapter adapter) {
        return fragment != null && CommonUtils.isVisible(fragment) && adapter != null && adapter.getCurrentDir() != null && adapter.getCurrentDir().files.size() >= 1;
    }
}
