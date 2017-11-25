package com.gianlu.aria2app.Activities.MoreAboutDownload.Files;

import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gianlu.aria2app.NetIO.JTA2.AriaDirectory;
import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.ProfilesManager.ProfilesManager;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.NiceBaseBottomSheet;
import com.gianlu.commonutils.SuperTextView;

import java.util.Locale;

// TODO: Reimplement "selected"
public class DirBottomSheet extends NiceBaseBottomSheet {
    private final ISheet listener;

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
    protected void onCreateHeaderView(@NonNull ViewGroup parent, Object... payloads) {
        TextView percentage = parent.findViewById(R.id.dirSheet_percentage);
        percentage.setTypeface(Typeface.createFromAsset(getContext().getAssets(), "fonts/Roboto-Medium.ttf"));
        TextView title = parent.findViewById(R.id.dirSheet_title);

        Download download = (Download) payloads[0];
        AriaDirectory dir = (AriaDirectory) payloads[1];

        int colorAccent = download.isTorrent() ? R.color.colorTorrent : R.color.colorAccent;
        parent.setBackgroundResource(colorAccent);

        title.setText(dir.name);
        percentage.setText(String.format(Locale.getDefault(), "%d%%", (int) dir.getProgress()));
    }

    @Override
    protected void onCreateContentView(@NonNull ViewGroup parent, Object... payloads) {
        SuperTextView indexes = parent.findViewById(R.id.dirSheet_indexes);
        SuperTextView path = parent.findViewById(R.id.dirSheet_path);
        SuperTextView length = parent.findViewById(R.id.dirSheet_length);
        SuperTextView completedLength = parent.findViewById(R.id.dirSheet_completedLength);

        AriaDirectory dir = (AriaDirectory) payloads[1];

        indexes.setHtml(R.string.indexes, CommonUtils.join(dir.indexes, ", "));
        path.setHtml(R.string.path, dir.fullPath);
        length.setHtml(R.string.total_length, CommonUtils.dimensionFormatter(dir.totalLength, false));
        completedLength.setHtml(R.string.completed_length, CommonUtils.dimensionFormatter(dir.completedLength, false));
    }

    public interface ISheet {
        void onDownloadDirectory(MultiProfile profile, Download download, AriaDirectory dir);
    }
}
