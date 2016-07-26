package com.gianlu.aria2app.Options;

import android.support.annotation.Nullable;

public class IntegerOptionChild extends OptionChild {
    private Integer defaultValue;
    private Integer currentValue;

    public IntegerOptionChild(String description, Integer defaultValue, Integer currentValue) {
        super(SourceOption.OPTION_TYPE.INTEGER, description);
        this.defaultValue = defaultValue;
        this.currentValue = currentValue;
    }

    public Integer getDefaultValue() {
        return defaultValue;
    }

    public Integer getCurrentValue() {
        if (currentValue == null) return defaultValue;
        return currentValue;
    }

    public void setCurrentValue(@Nullable Integer currentValue) {
        this.currentValue = currentValue;
    }

    @Override
    public boolean isChanged() {
        return currentValue != null;
    }

    @Override
    public String getStringValue() {
        if (getCurrentValue() == null)
            return "";
        else
            return String.valueOf(getCurrentValue());
    }
}
