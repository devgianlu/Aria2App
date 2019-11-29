package com.gianlu.aria2app.Tutorial;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.aria2app.Activities.MoreAboutDownload.Files.FilesAdapter;
import com.gianlu.aria2app.NetIO.Aria2.AriaDirectory;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.tutorial.BaseTutorial;

import me.toptas.fancyshowcase.FocusShape;

public final class FilesTutorial extends BaseTutorial {

    @Keep
    public FilesTutorial() {
        super(Discovery.FILES);
    }

    public final boolean buildSequence(@NonNull RecyclerView list, @Nullable AriaDirectory dir) {
        int firstFile = dir == null ? 0 : dir.dirs.size();
        RecyclerView.ViewHolder holder = list.findViewHolderForLayoutPosition(firstFile);
        if (holder != null) {
            list.scrollToPosition(firstFile);

            add(forView(holder.itemView, R.string.tutorial_fileDetails)
                    .enableAutoTextPosition()
                    .roundRectRadius(8)
                    .focusShape(FocusShape.ROUNDED_RECTANGLE));
            return true;
        }

        return false;
    }

    public final boolean canShow(Fragment fragment, FilesAdapter adapter) {
        return fragment != null && CommonUtils.isVisible(fragment) && adapter != null && adapter.getCurrentDir() != null && adapter.getCurrentDir().files.size() >= 1;
    }
}
