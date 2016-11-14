package com.gianlu.aria2app.Profile;

class ConnectivityCondition {
    private final TYPE type;
    private final String ssid;

    private ConnectivityCondition(TYPE type) {
        this.type = type;
        this.ssid = null;
    }

    private ConnectivityCondition(String ssid) {
        this.type = TYPE.WIFI;
        this.ssid = ssid;
    }

    static ConnectivityCondition newWiFiCondition(String ssid) {
        return new ConnectivityCondition(ssid);
    }

    static ConnectivityCondition newMobileCondition() {
        return new ConnectivityCondition(TYPE.MOBILE);
    }

    static ConnectivityCondition newBluetoothCondition() {
        return new ConnectivityCondition(TYPE.BLUETOOTH);
    }

    static ConnectivityCondition newEthernetCondition() {
        return new ConnectivityCondition(TYPE.ETHERNET);
    }

    static TYPE getTypeFromString(String type) {
        switch (type.toUpperCase()) {
            case "WIFI":
                return TYPE.WIFI;
            case "MOBILE":
                return TYPE.MOBILE;
            case "ETHERNET":
                return TYPE.ETHERNET;
            case "BLUETOOTH":
                return TYPE.BLUETOOTH;
            default:
                return TYPE.UNKNOWN;
        }
    }

    TYPE getType() {
        return type;
    }

    String getSSID() {
        return ssid;
    }

    String getFormalName() {
        return type.getFormal() + (type == TYPE.WIFI ? ": " + ssid : "");
    }

    enum TYPE {
        WIFI("WiFi"),
        MOBILE("Mobile"),
        ETHERNET("Ethernet"),
        BLUETOOTH("Bluetooth"),
        UNKNOWN("Unknown");

        private final String formal;

        TYPE(String formal) {
            this.formal = formal;
        }

        public String getFormal() {
            return formal;
        }

        @Override
        public String toString() {
            return formal;
        }
    }
}
