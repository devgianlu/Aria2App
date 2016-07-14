package com.gianlu.aria2app.Options;

import java.util.List;

public class OptionChild {
    private String option;
    private String desc;
    private Object defaultVal;
    private Object value;
    private OPTION_TYPE optionType;
    private List<String> possibleValues;
    private Object currentValue;

    public OptionChild(String option, Object value, Object defaultVal, OPTION_TYPE optionType, String desc) {
        this.option = option;
        this.value = value;
        this.desc = desc;
        this.defaultVal = defaultVal;
        this.optionType = optionType;
        this.possibleValues = null;
    }

    public OptionChild(String option, Object value, Object defaultVal, String desc, List<String> possibleValues) {
        this.option = option;
        this.value = value;
        this.desc = desc;
        this.defaultVal = defaultVal;
        this.optionType = OPTION_TYPE.MULTIPLE;
        this.possibleValues = possibleValues;
    }

    public OPTION_TYPE getOptionType() {
        return optionType;
    }

    public Object getValue() {
        return value;
    }

    public Object getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(Object currentValue) {
        this.currentValue = currentValue;
    }

    public Object getDefaultVal() {
        return defaultVal;
    }

    public List<String> getPossibleValues() {
        return possibleValues;
    }

    public String getDesc() {
        return desc;
    }

    public String getOption() {
        return option;
    }

    public enum OPTION_TYPE {
        BOOLEAN,
        STRING,
        INTEGER,
        MULTIPLE
    }
}
