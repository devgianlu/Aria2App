package com.gianlu.aria2app.Main.Profile;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ProfileItem implements Parcelable {
    public static final Creator<ProfileItem> CREATOR = new Creator<ProfileItem>() {
        @Override
        public ProfileItem createFromParcel(Parcel in) {
            return new ProfileItem(in);
        }

        @Override
        public ProfileItem[] newArray(int size) {
            return new ProfileItem[size];
        }
    };
    protected String fileName;
    protected String globalProfileName;
    protected boolean singleMode;
    protected STATUS status = STATUS.UNKNOWN;
    protected String statusMessage;
    protected boolean isDefault;
    private Long latency = -1L;

    protected ProfileItem(Parcel in) {
        fileName = in.readString();
        globalProfileName = in.readString();
        singleMode = in.readByte() != 0;
        status = STATUS.valueOf(in.readString());
        statusMessage = in.readString();
        isDefault = in.readByte() != 0;
        latency = in.readLong();
    }

    public ProfileItem() {
    }

    public static boolean exists(Context context, String base64name) {
        if (base64name == null) return false;

        try {
            context.openFileInput(base64name);
            return true;
        } catch (FileNotFoundException ex) {
            return false;
        }
    }

    public static boolean isSingleMode(Context context, String base64name) throws JSONException, IOException {
        if (base64name == null) return false;

        FileInputStream in = context.openFileInput(base64name);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder builder = new StringBuilder();

        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }

        return new JSONObject(builder.toString()).isNull("conditions");
    }

    public static List<ProfileItem> getProfiles(Context context) {
        List<ProfileItem> profiles = new ArrayList<>();
        File files[] = context.getFilesDir().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.toLowerCase().endsWith(".profile");
            }
        });

        for (File profile : files) {
            try {
                if (ProfileItem.isSingleMode(context, profile.getName())) {
                    profiles.add(SingleModeProfileItem.fromString(context, profile.getName()));
                } else {
                    profiles.add(MultiModeProfileItem.fromString(context, profile.getName()));
                }
            } catch (Exception ignored) {
            }
        }

        return profiles;
    }

    public static String getProfileName(String fileName) {
        if (fileName == null) return null;

        return new String(Base64.decode(fileName.replace(".profile", "").getBytes(), Base64.DEFAULT));
    }

    public STATUS getStatus() {
        return status;
    }

    public void setStatus(STATUS status) {
        this.status = status;
    }

    public String getGlobalProfileName() {
        return globalProfileName;
    }

    public boolean isSingleMode() {
        return singleMode;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public Long getLatency() {
        return latency;
    }

    public void setLatency(Long latency) {
        this.latency = latency;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(fileName);
        parcel.writeString(globalProfileName);
        parcel.writeByte((byte) (singleMode ? 1 : 0));
        parcel.writeString(status.name());
        parcel.writeString(statusMessage);
        parcel.writeByte((byte) (isDefault ? 1 : 0));
        parcel.writeLong(latency);
    }

    public enum STATUS {
        ONLINE,
        OFFLINE,
        ERROR,
        UNKNOWN
    }
}
