package com.gianlu.aria2app.Options;

import android.support.annotation.Nullable;

public class BooleanOptionChild extends OptionChild {
    private Boolean defaultValue;
    private Boolean currentValue;

    public BooleanOptionChild(String description, Boolean defaultValue, Boolean currentValue) {
        super(SourceOption.OPTION_TYPE.BOOLEAN, description);
        this.defaultValue = defaultValue;
        this.currentValue = currentValue;
    }

    public Boolean getCurrentValue() {
        if (currentValue == null) return defaultValue;
        return currentValue;
    }

    public void setCurrentValue(@Nullable Boolean currentValue) {
        this.currentValue = currentValue;
    }

    @Override
    public boolean isChanged() {
        return currentValue != null;
    }

    @Override
    public String getStringValue() {
        return String.valueOf(getCurrentValue());
    }
}
