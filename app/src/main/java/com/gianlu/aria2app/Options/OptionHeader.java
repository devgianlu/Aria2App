package com.gianlu.aria2app.Options;

import android.support.annotation.Nullable;

class OptionHeader {
    private String optionLong;
    private String optionShort;
    private String optionValue;
    private boolean isQuick;

    OptionHeader(String optionLong, String optionShort, String optionValue, boolean isQuick) {
        this.optionShort = optionShort;
        this.optionValue = optionValue;
        this.optionLong = optionLong;
        this.isQuick = isQuick;
    }

    String getOptionLong() {
        return optionLong;
    }

    String getOptionShort() {
        return optionShort;
    }

    @Nullable
    String getOptionValue() {
        return optionValue;
    }

    boolean isQuick() {
        return isQuick;
    }

    void setQuick(boolean quick) {
        isQuick = quick;
    }
}
