package com.gianlu.aria2app.Options;

import android.support.annotation.Nullable;

public class OptionHeader {
    private String optionLong;
    private String optionShort;
    private String optionValue;

    public OptionHeader(String optionLong, String optionShort, String optionValue) {
        this.optionShort = optionShort;
        this.optionValue = optionValue;
        this.optionLong = optionLong;
    }

    public String getOptionLong() {
        return optionLong;
    }

    public String getOptionShort() {
        return optionShort;
    }

    @Nullable
    public String getOptionValue() {
        return optionValue;
    }
}
