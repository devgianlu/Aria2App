package com.gianlu.aria2app.Options;

public abstract class OptionChild {
    private String description;
    private SourceOption.OPTION_TYPE type;

    public OptionChild(SourceOption.OPTION_TYPE type, String description) {
        this.description = description;
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public SourceOption.OPTION_TYPE getType() {
        return type;
    }

    public abstract boolean isChanged();

    public abstract String getStringValue();
}
