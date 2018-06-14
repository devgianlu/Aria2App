package com.gianlu.aria2app.Tutorial;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.gianlu.aria2app.PKeys;
import com.gianlu.commonutils.Dialogs.DialogUtils;
import com.gianlu.commonutils.Preferences.Prefs;

import java.util.ArrayList;
import java.util.List;

public final class TutorialManager implements BaseTutorial.Listener {
    private final Listener listener;
    private final List<BaseTutorial> tutorials;
    private final SharedPreferences preferences;
    private boolean isShowingTutorial = false;

    public TutorialManager(Context context, Listener listener, Discovery... discoveries) {
        this.listener = listener;
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);

        if (discoveries.length == 0)
            throw new IllegalStateException("Then why are you initializing this...");

        try {
            tutorials = new ArrayList<>();
            for (Discovery discovery : discoveries)
                tutorials.add(discovery.tutorialClass.newInstance());
        } catch (InstantiationException | IllegalAccessException ex) {
            throw new RuntimeException("Something is wrong with your tutorials!", ex);
        }
    }

    public static void restartTutorial(@NonNull Context context) {
        Prefs.remove(context, PKeys.A2_TUTORIAL_DISCOVERIES);
    }

    private boolean shouldShowFor(@NonNull BaseTutorial tutorial) {
        return !Prefs.setContains(preferences, PKeys.A2_TUTORIAL_DISCOVERIES, tutorial.discovery.name());
    }

    private void setShown(@NonNull BaseTutorial tutorial) {
        Prefs.addToSet(preferences, PKeys.A2_TUTORIAL_DISCOVERIES, tutorial.discovery.name());
    }

    public void tryShowingTutorials(Activity activity) {
        if (isShowingTutorial || !DialogUtils.isContextValid(activity)) return;

        for (BaseTutorial tutorial : tutorials) {
            if (shouldShowFor(tutorial) && listener.canShow(tutorial)) {
                show(activity, tutorial);
                return;
            }
        }
    }

    private void show(@NonNull Activity activity, @NonNull BaseTutorial tutorial) {
        TapTargetSequence seq = tutorial.newSequence(activity);
        if (listener.buildSequence(tutorial, seq)) {
            isShowingTutorial = true;
            tutorial.show(this);
        }
    }

    @Override
    public void onSequenceFinish(@NonNull BaseTutorial tutorial) {
        isShowingTutorial = false;
        setShown(tutorial);
    }

    @Override
    public void onSequenceStep(@NonNull BaseTutorial tutorial, @NonNull TapTarget lastTarget, boolean targetClicked) {
    }

    @Override
    public void onSequenceCanceled(@NonNull BaseTutorial tutorial, @NonNull TapTarget lastTarget) {
    }

    public enum Discovery {
        DOWNLOADS_TOOLBAR(DownloadsToolbarTutorial.class),
        DOWNLOADS_CARDS(DownloadCardsTutorial.class),
        FOLDERS(FoldersTutorial.class),
        FILES(FilesTutorial.class),
        PEERS_SERVERS(PeersServersTutorial.class);

        public final Class<? extends BaseTutorial> tutorialClass;

        Discovery(@NonNull Class<? extends BaseTutorial> tutorialClass) {
            this.tutorialClass = tutorialClass;
        }
    }

    public interface Listener {
        boolean canShow(@NonNull BaseTutorial tutorial);

        /**
         * @return Whether the action was successful
         */
        boolean buildSequence(@NonNull BaseTutorial tutorial, @NonNull TapTargetSequence sequence);
    }
}
