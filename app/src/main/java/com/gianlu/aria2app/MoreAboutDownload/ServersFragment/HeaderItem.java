package com.gianlu.aria2app.MoreAboutDownload.ServersFragment;

public class HeaderItem extends Item {
    private int index;

    public HeaderItem(int index) {
        this.index = index;
    }

    public String getTitle() {
        return "Index " + index;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public int getItemType() {
        return Item.HEADER;
    }
}
