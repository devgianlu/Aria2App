package com.gianlu.aria2app;

import android.content.Context;

import com.gianlu.commonutils.Prefs;

import java.util.HashSet;
import java.util.Set;

public class TutorialManager {

    @SuppressWarnings("ConstantConditions")
    public static boolean shouldShowHintFor(Context context, Discovery discovery) {
        if (context == null) return false;
        Set<String> set = Prefs.getSet(context, PKeys.A2_TUTORIAL_DISCOVERIES, new HashSet<String>());
        return !set.contains(discovery.name());
    }

    public static void setHintShown(Context context, Discovery discovery) {
        Prefs.addToSet(context, PKeys.A2_TUTORIAL_DISCOVERIES, discovery.name());
    }

    public static void restartTutorial(Context context) {
        Prefs.remove(context, PKeys.A2_TUTORIAL_DISCOVERIES);
    }

    public enum Discovery {
        CARD,
        PEERS_SERVERS,
        FILES,
        FOLDERS,
        TOOLBAR
    }
}
