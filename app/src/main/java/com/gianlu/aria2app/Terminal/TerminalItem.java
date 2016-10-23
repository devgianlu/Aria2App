package com.gianlu.aria2app.Terminal;

public class TerminalItem {
    public static final int FROM_SERVER = 1;
    public static final int FROM_CLIENT = 2;
    public static final int TYPE_CONVERSATION = 4;
    public static final int TYPE_INFO = 8;

    public Exception exception;
    public String text;
    public int type;
    public boolean fromServer;

    private TerminalItem() {
    }

    public static TerminalItem createInfoItem(String text) {
        TerminalItem item = new TerminalItem();
        item.type = TYPE_INFO;
        item.text = text;
        return item;
    }

    public static TerminalItem createInfoItem(Exception ex) {
        TerminalItem item = new TerminalItem();
        item.type = TYPE_INFO;
        item.exception = ex;
        item.text = ex.getMessage();
        return item;
    }

    public boolean isException() {
        return exception != null;
    }
}
