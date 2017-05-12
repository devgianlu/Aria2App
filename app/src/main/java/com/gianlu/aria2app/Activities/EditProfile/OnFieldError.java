package com.gianlu.aria2app.Activities.EditProfile;

import android.support.annotation.IdRes;

public interface OnFieldError {
    void onFieldError(@IdRes int fieldId, String reason);
}
