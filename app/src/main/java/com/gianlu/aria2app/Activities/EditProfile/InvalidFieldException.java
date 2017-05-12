package com.gianlu.aria2app.Activities.EditProfile;


import android.support.annotation.IdRes;

public class InvalidFieldException extends Exception {
    public final Class fragmentClass;
    public final int fieldId;
    public final String reason;

    public InvalidFieldException(Class fragmentClass, @IdRes int fieldId, String reason) {
        this.fragmentClass = fragmentClass;
        this.fieldId = fieldId;
        this.reason = reason;
    }
}
