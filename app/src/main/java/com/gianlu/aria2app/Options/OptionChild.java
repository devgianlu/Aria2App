package com.gianlu.aria2app.Options;

import java.util.Objects;

public class OptionChild {
    private String description;
    private String defValue;
    private String currValue;

    public OptionChild(String description, String defValue, String currValue) {
        this.description = description;
        this.defValue = defValue;
        this.currValue = currValue;
    }

    public String getDescription() {
        return description;
    }

    public boolean isChanged() {
        return currValue != null && (!Objects.equals(currValue, "null")) && !currValue.isEmpty() && (!Objects.equals(currValue, defValue));
    }

    public void setCurrentValue(String currValue) {
        this.currValue = currValue;
    }

    public String getValue() {
        if (isChanged()) return currValue;
        return defValue;
    }

    public String getDefaultValue() {
        return defValue;
    }
}
