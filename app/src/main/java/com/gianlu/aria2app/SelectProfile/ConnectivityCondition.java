package com.gianlu.aria2app.SelectProfile;

public class ConnectivityCondition {
    private TYPE type;
    private String ssid;

    private ConnectivityCondition(TYPE type) {
        this.type = type;
        this.ssid = null;
    }

    private ConnectivityCondition(TYPE type, String ssid) {
        this.type = type;
        this.ssid = ssid;
    }

    public static ConnectivityCondition newWiFiCondition(String ssid) {
        return new ConnectivityCondition(TYPE.WIFI, ssid);
    }

    public static ConnectivityCondition newMobileCondition() {
        return new ConnectivityCondition(TYPE.MOBILE);
    }

    public static ConnectivityCondition newBluetoothCondition() {
        return new ConnectivityCondition(TYPE.BLUETOOTH);
    }

    public static ConnectivityCondition newEthernetCondition() {
        return new ConnectivityCondition(TYPE.ETHERNET);
    }

    public static TYPE getTypeFromString(String type) {
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

    public TYPE getType() {
        return type;
    }

    public String getSSID() {
        return ssid;
    }

    public String getFormalName() {
        return type.getFormal() + (type == TYPE.WIFI ? ": " + ssid : "");
    }

    public enum TYPE {
        WIFI("WiFi"),
        MOBILE("Mobile"),
        ETHERNET("Ethernet"),
        BLUETOOTH("Bluetooth"),
        UNKNOWN("Unknown");

        private String formal;

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
