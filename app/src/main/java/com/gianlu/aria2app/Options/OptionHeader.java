package com.gianlu.aria2app.Options;

public class OptionHeader {
    private String optionName;
    private String optionFull;
    private Object optionValue;

    public OptionHeader(String optionName, String optionFull, Object optionValue) {
        this.optionName = optionName;
        this.optionFull = optionFull;
        this.optionValue = optionValue;
    }

    public String getOptionName() {
        return optionName;
    }

    public Object getOptionValue() {
        return optionValue;
    }

    public String getOptionFull() {
        return optionFull;
    }
}
