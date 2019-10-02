package com.gianlu.aria2app.Activities.EditProfile;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.dialogs.FragmentWithDialog;

public abstract class FieldErrorFragmentWithState extends FragmentWithDialog implements OnFieldError {
    private Bundle stateToRestore;

    @NonNull
    public final Bundle save() throws IllegalStateException {
        if (!isAdded()) throw new IllegalStateException();

        if (stateToRestore != null) return stateToRestore;

        Bundle bundle = new Bundle();
        onSaveInstanceState(bundle);
        return bundle;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (stateToRestore != null) {
            onRestoreInstanceState(stateToRestore);
            stateToRestore = null;
        }
    }

    public final void restore(@Nullable Bundle bundle) {
        if (bundle == null) bundle = new Bundle();

        if (isAdded() && isResumed()) {
            stateToRestore = null;
            onRestoreInstanceState(bundle);
        } else {
            stateToRestore = bundle;
        }
    }

    protected abstract void onRestoreInstanceState(@NonNull Bundle bundle);
}
