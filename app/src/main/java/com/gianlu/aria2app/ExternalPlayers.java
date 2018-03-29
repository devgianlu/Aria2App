package com.gianlu.aria2app;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ExternalPlayers {
    private static final Map<Player, List<String>> supportedMimeTypes = new HashMap<>();

    static {
        List<String> vlcSupported = new ArrayList<>();
        vlcSupported.add("video/*");
        vlcSupported.add("audio/*");
        vlcSupported.add("*/rmvb");
        vlcSupported.add("*/avi ");
        vlcSupported.add("*/mkv");
        vlcSupported.add("application/3gpp*");
        vlcSupported.add("application/mp4");
        vlcSupported.add("application/mpeg*");
        vlcSupported.add("application/ogg");
        vlcSupported.add("application/sdp");
        vlcSupported.add("application/vnd.3gp*");
        vlcSupported.add("application/vnd.apple.mpegurl");
        vlcSupported.add("application/vnd.dvd*");
        vlcSupported.add("application/vnd.dolby*");
        vlcSupported.add("application/vnd.rn-realmedia*");
        vlcSupported.add("application/x-iso9660-image");
        vlcSupported.add("application/x-extension-mp4");
        vlcSupported.add("application/x-flac");
        vlcSupported.add("application/x-matroska");
        vlcSupported.add("application/x-mpegURL");
        vlcSupported.add("application/x-ogg");
        vlcSupported.add("application/x-quicktimeplayer");
        vlcSupported.add("application/x-shockwave-flash");
        vlcSupported.add("application/xspf+xml");
        vlcSupported.add("misc/ultravox");
        supportedMimeTypes.put(Player.VLC, vlcSupported);
    }

    private static boolean matchesMime(List<String> list, @NonNull String mime) {
        for (String supported : list) {
            if (supported.charAt(0) == '*') {
                if (mime.endsWith(supported.substring(1)))
                    return true;
            } else if (supported.charAt(supported.length() - 1) == '*') {
                if (mime.startsWith(supported.substring(0, supported.length() - 1)))
                    return true;
            } else {
                if (mime.equalsIgnoreCase(supported))
                    return true;
            }
        }

        return false;
    }

    @Nullable
    public static Player supportedBy(@NonNull String mime) {
        for (Player player : supportedMimeTypes.keySet()) {
            if (matchesMime(supportedMimeTypes.get(player), mime))
                return player;
        }

        return null;
    }

    public static void play(@NonNull Context context, @NonNull Player player) {
        switch (player) {
            case VLC:
                break;
        }
    }

    public enum Player {
        VLC
    }
}
