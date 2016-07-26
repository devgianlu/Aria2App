package com.gianlu.aria2app.Options;


import java.util.List;

public class MultipleOptionChild extends OptionChild {
    private String currentValue;
    private String defaultValue;
    private List<String> possibleValues;

    public MultipleOptionChild(String description, String currentValue, String defaultValue, List<String> possibleValues) {
        super(SourceOption.OPTION_TYPE.MULTIPLE, description);
        this.currentValue = currentValue;
        this.defaultValue = defaultValue;
        this.possibleValues = possibleValues;
    }

    public String getCurrentValue() {
        if (currentValue == null) return defaultValue;
        return currentValue;
    }

    public void setCurrentValue(String currentValue) {
        this.currentValue = currentValue;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public List<String> getPossibleValues() {
        return possibleValues;
    }

    @Override
    public boolean isChanged() {
        return currentValue != null && !currentValue.isEmpty();
    }

    @Override
    public String getStringValue() {
        return getCurrentValue();
    }
}
