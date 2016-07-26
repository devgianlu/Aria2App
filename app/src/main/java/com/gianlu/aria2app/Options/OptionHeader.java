package com.gianlu.aria2app.Options;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class OptionHeader {
    private String optionName;
    private String optionCMD;
    private String optionValue;
    private boolean needRestart;

    public OptionHeader(String optionName, String optionCMD, String optionValue, boolean needRestart) {
        this.optionName = optionName;
        this.optionCMD = optionCMD;
        this.optionValue = optionValue;
        this.needRestart = needRestart;
    }

    @NonNull
    public String getOptionName() {
        return optionName;
    }

    @NonNull
    public String getOptionCommandLine() {
        return optionCMD;
    }

    @Nullable
    public String getOptionStringValue() {
        return optionValue;
    }

    public boolean needRestart() {
        return needRestart;
    }
}
