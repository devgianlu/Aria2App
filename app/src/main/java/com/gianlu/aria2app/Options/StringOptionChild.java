package com.gianlu.aria2app.Options;


import android.support.annotation.Nullable;

public class StringOptionChild extends OptionChild {
    private String defaultValue;
    private String currentValue;

    public StringOptionChild(String description, String defaultValue, String currentValue) {
        super(SourceOption.OPTION_TYPE.STRING, description);
        this.defaultValue = defaultValue;
        this.currentValue = currentValue;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getCurrentValue() {
        if (currentValue == null) return defaultValue;
        return currentValue;
    }

    public void setCurrentValue(@Nullable String currentValue) {
        this.currentValue = currentValue;
    }

    @Override
    public boolean isChanged() {
        return currentValue != null && !currentValue.isEmpty();
    }

    @Override
    public String getStringValue() {
        if (getCurrentValue() == null || getCurrentValue().equals("null"))
            return "";
        else
            return getCurrentValue();
    }
}
