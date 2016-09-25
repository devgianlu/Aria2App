package com.gianlu.aria2app.Options;

import java.util.List;

class OptionChild {
    private final String defValue;
    private final List<String> values;
    private final Option.TYPE type;
    private String currValue;

    OptionChild(Option.TYPE type, Object defValue, String currValue, List<String> values) {
        this.type = type;
        if (defValue == null)
            this.defValue = null;
        else
            this.defValue = String.valueOf(defValue);
        this.currValue = currValue;
        this.values = values;
    }

    boolean isChanged() {
        return currValue != null;
    }

    void setCurrentValue(String currValue) {
        this.currValue = currValue;
    }

    List<String> getValues() {
        return values;
    }

    public String getValue() {
        if (isChanged()) return currValue;
        return defValue;
    }

    Option.TYPE getType() {
        return type;
    }

    String getDefaultValue() {
        if (defValue == null) return "";
        return defValue;
    }
}
