package com.gianlu.aria2app.Activities.EditProfile;


import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

public class InvalidFieldException extends Exception {
    public final int fieldId;
    public final int reasonRes;
    public final Where where;
    /**
     * The current condition position in the conditions list
     */
    public int pos = -1;

    public InvalidFieldException(@NonNull Where where, @IdRes int fieldId, @StringRes int reasonRes) {
        this.where = where;
        this.fieldId = fieldId;
        this.reasonRes = reasonRes;
    }

    public enum Where {
        ACTIVITY, CONNECTION, AUTHENTICATION, DIRECT_DOWNLOAD;

        public int pagerPos() {
            switch (this) {
                default:
                case ACTIVITY:
                    return -1;
                case CONNECTION:
                    return 0;
                case AUTHENTICATION:
                    return 1;
                case DIRECT_DOWNLOAD:
                    return 2;
            }
        }
    }
}
