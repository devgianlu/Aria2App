package com.gianlu.aria2app.Tutorial;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.gianlu.aria2app.Adapters.FilesAdapter;
import com.gianlu.aria2app.NetIO.Aria2.AriaDirectory;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;

public class FilesTutorial extends BaseTutorial {

    @Keep
    public FilesTutorial() {
        super(TutorialManager.Discovery.FILES);
    }

    public final boolean buildSequence(@NonNull Context context, @NonNull TapTargetSequence seq, @NonNull RecyclerView list, @Nullable AriaDirectory dir) {
        int firstFile = dir == null ? 0 : dir.dirs.size();
        RecyclerView.ViewHolder holder = list.findViewHolderForLayoutPosition(firstFile);
        if (holder != null) {
            list.scrollToPosition(firstFile);

            Rect rect = new Rect();
            holder.itemView.getGlobalVisibleRect(rect);
            rect.offset((int) -(holder.itemView.getWidth() * 0.3), 0);

            seq.target(TapTarget.forBounds(rect, context.getString(R.string.fileDetails), context.getString(R.string.fileDetails_desc))
                    .tintTarget(false)
                    .transparentTarget(true));

            return true;
        }

        return false;
    }

    public final boolean canShow(Fragment fragment, FilesAdapter adapter) {
        return fragment != null && CommonUtils.isVisible(fragment) && adapter != null && adapter.getCurrentDir() != null && adapter.getCurrentDir().files.size() >= 1;
    }
}
