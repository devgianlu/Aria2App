package com.gianlu.aria2app.Options;

import java.util.Objects;

public class Option {
    public String longName;
    public String value;

    public String newValue;
    public boolean isQuick;

    public Option(String longName, String value, boolean isQuick) {
        this.longName = longName;
        this.value = value;
        this.isQuick = isQuick;
    }

    public boolean isChanged() {
        if (newValue == null) {
            return false;
        } else {
            return !Objects.equals(value, newValue);
        }
    }
}
