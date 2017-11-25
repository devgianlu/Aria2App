package com.gianlu.aria2app.Activities.MoreAboutDownload.Files;

import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gianlu.aria2app.NetIO.JTA2.AriaDirectory;
import com.gianlu.aria2app.NetIO.JTA2.AriaFile;
import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.NiceBaseBottomSheet;
import com.gianlu.commonutils.SuperTextView;

import java.util.List;
import java.util.Locale;

// TODO: Reimplement "selected"
public class DirBottomSheet extends NiceBaseBottomSheet {
    private final ISheet listener;
    private SuperTextView length;
    private SuperTextView completedLength;
    private TextView percentage;
    private AriaDirectory currentDir = null;

    public DirBottomSheet(ViewGroup parent, ISheet listener) {
        super(parent, R.layout.dir_sheet_header, R.layout.dir_sheet, true);
        this.listener = listener;
    }

    @Override
    protected boolean onPrepareAction(@NonNull FloatingActionButton fab, Object... payloads) {
        final Download download = (Download) payloads[0];
        final AriaDirectory dir = (AriaDirectory) payloads[1];

        try {
            final MultiProfile profile = ProfilesManager.get(getContext()).getCurrent(getContext());
            if (download.isMetadata() || !profile.getProfile(getContext()).isDirectDownloadEnabled()) {
                return false;
            } else {
                fab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (listener != null) listener.onDownloadDirectory(profile, download, dir);
                    }
                });

                return true;
            }
        } catch (ProfilesManager.NoCurrentProfileException ex) {
            Logging.logMe(ex);
            return false;
        }
    }

    @Override
    protected void cleanUp() {
        currentDir = null;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onUpdateViews(Object... payloads) {
        if (currentDir != null) {
            currentDir.update((Download) payloads[0], (List<AriaFile>) payloads[1]);
            updateContentViews(currentDir);
            updateHeaderViews(currentDir);
        }
    }

    private void updateHeaderViews(AriaDirectory dir) {
        percentage.setText(String.format(Locale.getDefault(), "%d%%", (int) dir.getProgress()));
    }

    @Override
    protected void onCreateHeaderView(@NonNull ViewGroup parent, Object... payloads) {
        percentage = parent.findViewById(R.id.dirSheet_percentage);
        percentage.setTypeface(Typeface.createFromAsset(getContext().getAssets(), "fonts/Roboto-Medium.ttf"));
        TextView title = parent.findViewById(R.id.dirSheet_title);

        Download download = (Download) payloads[0];
        AriaDirectory dir = (AriaDirectory) payloads[1];

        int colorAccent = download.isTorrent() ? R.color.colorTorrent : R.color.colorAccent_light;
        parent.setBackgroundResource(colorAccent);

        title.setText(dir.name);
        updateHeaderViews(dir);
    }

    private void updateContentViews(AriaDirectory dir) {
        length.setHtml(R.string.total_length, CommonUtils.dimensionFormatter(dir.totalLength, false));
        completedLength.setHtml(R.string.completed_length, CommonUtils.dimensionFormatter(dir.completedLength, false));
    }

    @Override
    protected void onCreateContentView(@NonNull ViewGroup parent, Object... payloads) {
        SuperTextView indexes = parent.findViewById(R.id.dirSheet_indexes);
        SuperTextView path = parent.findViewById(R.id.dirSheet_path);

        length = parent.findViewById(R.id.dirSheet_length);

        completedLength = parent.findViewById(R.id.dirSheet_completedLength);

        AriaDirectory dir = (AriaDirectory) payloads[1];

        indexes.setHtml(R.string.indexes, CommonUtils.join(dir.indexes, ", "));
        path.setHtml(R.string.path, dir.fullPath);
        updateContentViews(dir);
    }

    public interface ISheet {
        void onDownloadDirectory(MultiProfile profile, Download download, AriaDirectory dir);
    }
}
