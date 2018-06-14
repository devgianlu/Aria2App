package com.gianlu.aria2app.Tutorial;

import android.app.Activity;
import android.support.annotation.NonNull;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;

public abstract class BaseTutorial {
    public final TutorialManager.Discovery discovery;
    private TapTargetSequence sequence;

    public BaseTutorial(@NonNull TutorialManager.Discovery discovery) {
        this.discovery = discovery;
    }

    @NonNull
    public final TapTargetSequence newSequence(@NonNull Activity activity) {
        sequence = new TapTargetSequence(activity);
        return sequence;
    }

    public final void show(@NonNull Listener listener) {
        if (sequence != null)
            sequence.continueOnCancel(true)
                    .listener(new ListenerWrapper(listener))
                    .start();
    }

    public interface Listener {
        void onSequenceFinish(@NonNull BaseTutorial tutorial);

        void onSequenceStep(@NonNull BaseTutorial tutorial, @NonNull TapTarget lastTarget, boolean targetClicked);

        void onSequenceCanceled(@NonNull BaseTutorial tutorial, @NonNull TapTarget lastTarget);
    }

    private class ListenerWrapper implements TapTargetSequence.Listener {
        private final Listener listener;

        ListenerWrapper(@NonNull Listener listener) {
            this.listener = listener;
        }

        @Override
        public void onSequenceFinish() {
            listener.onSequenceFinish(BaseTutorial.this);
        }

        @Override
        public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {
            listener.onSequenceStep(BaseTutorial.this, lastTarget, targetClicked);
        }

        @Override
        public void onSequenceCanceled(TapTarget lastTarget) {
            listener.onSequenceCanceled(BaseTutorial.this, lastTarget);
        }
    }
}
