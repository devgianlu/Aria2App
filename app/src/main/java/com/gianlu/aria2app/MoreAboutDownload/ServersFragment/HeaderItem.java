package com.gianlu.aria2app.MoreAboutDownload.ServersFragment;

class HeaderItem extends Item {
    private final int index;

    HeaderItem(int index) {
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
