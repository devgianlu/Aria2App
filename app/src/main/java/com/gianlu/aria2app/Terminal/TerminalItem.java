package com.gianlu.aria2app.Terminal;

public class TerminalItem {
    public static final int TYPE_INFO = 8;
    static final int FROM_SERVER = 1;
    static final int FROM_CLIENT = 2;
    static final int TYPE_CONVERSATION = 4;
    public final long at;
    public String text;
    public int type;
    boolean fromServer;
    private Exception exception;

    private TerminalItem() {
        at = System.currentTimeMillis();
    }

    static TerminalItem createInfoItem(String text) {
        TerminalItem item = new TerminalItem();
        item.type = TYPE_INFO;
        item.text = text;
        return item;
    }

    static TerminalItem createInfoItem(Exception ex) {
        TerminalItem item = new TerminalItem();
        item.type = TYPE_INFO;
        item.exception = ex;
        item.text = ex.getMessage();
        return item;
    }

    public static TerminalItem createConversationClientItem(String message) {
        TerminalItem item = new TerminalItem();
        item.type = TYPE_CONVERSATION;
        item.fromServer = false;
        item.text = message;
        return item;
    }

    static TerminalItem createConversationServerItem(String message) {
        TerminalItem item = new TerminalItem();
        item.type = TYPE_CONVERSATION;
        item.fromServer = true;
        item.text = message;
        return item;
    }

    public boolean isException() {
        return exception != null;
    }
}
