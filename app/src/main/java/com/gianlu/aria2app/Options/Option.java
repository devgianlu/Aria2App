package com.gianlu.aria2app.Options;

import java.util.Objects;

public class Option {
    public final String longName;
    public final String value;

    public String newValue;
    public boolean isQuick;
    public boolean useMe;

    public Option(String longName, String value, boolean isQuick) {
        this.longName = longName;
        this.value = value;
        this.isQuick = isQuick;
    }

    public boolean isChanged() {
        return newValue != null && !Objects.equals(value, newValue);
    }
}
