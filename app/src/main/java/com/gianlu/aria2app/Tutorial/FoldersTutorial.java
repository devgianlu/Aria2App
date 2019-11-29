package com.gianlu.aria2app.Tutorial;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.aria2app.Activities.MoreAboutDownload.Files.FilesAdapter;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.tutorial.BaseTutorial;

import me.toptas.fancyshowcase.FocusShape;

public final class FoldersTutorial extends BaseTutorial {

    @Keep
    public FoldersTutorial() {
        super(Discovery.FOLDERS);
    }

    public final boolean buildSequence(@NonNull RecyclerView list) {
        RecyclerView.ViewHolder holder = list.findViewHolderForLayoutPosition(0);
        if (holder != null) {
            list.scrollToPosition(0);

            add(forView(holder.itemView, R.string.tutorial_folderDetails)
                    .enableAutoTextPosition()
                    .roundRectRadius(8)
                    .focusShape(FocusShape.ROUNDED_RECTANGLE));
            return true;
        }

        return false;
    }

    public final boolean canShow(Fragment fragment, FilesAdapter adapter) {
        return fragment != null && CommonUtils.isVisible(fragment) && adapter != null && adapter.getCurrentDir() != null && adapter.getCurrentDir().dirs.size() >= 1;
    }
}
