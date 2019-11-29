package com.gianlu.aria2app.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class PeerIdParser {
    // Az style two byte code identifiers to real client name
    private static final Map<String, String> azStyleClients = new HashMap<>();
    private static final Map<String, VersionProvider> azStyleClientVersions = new HashMap<>();
    // Shadow's style one byte code identifiers to real client name
    private static final Map<String, String> shadowStyleClients = new HashMap<>();
    private static final Map<String, VersionProvider> shadowStyleClientVersions = new HashMap<>();
    // Mainline's new style uses one byte code identifiers too
    private static final Map<String, String> mainlineStyleClients = new HashMap<>();
    // Clients with completely custom naming schemes
    private static final List<SimpleClient> customStyleClients = new ArrayList<>();
    private static final VersionProvider VER_AZ_THREE_DIGITS = v -> v.charAt(0) + "." + v.charAt(1) + "." + v.charAt(2);
    private static final VersionProvider VER_AZ_FOUR_DIGITS = v -> v.charAt(0) + "." + v.charAt(1) + "." + v.charAt(2) + "." + v.charAt(3);
    private static final VersionProvider VER_AZ_DELUGE = v -> {
        String alphabet = "ABCDE";
        if (!Character.isDigit(v.charAt(2)))
            return v.charAt(0) + "." + v.charAt(1) + ".1" + alphabet.indexOf(v.charAt(2));

        return v.charAt(0) + "." + v.charAt(1) + "." + v.charAt(2);
    };
    private static final VersionProvider VER_AZ_GS = v -> {
        String alphabet = "ABCD";
        if (!Character.isDigit(v.charAt(2)))
            return v.charAt(0) + "." + v.charAt(1) + ".1" + alphabet.indexOf(v.charAt(2));

        return v.charAt(0) + "." + v.charAt(1) + "." + v.charAt(2);
    };
    private static final VersionProvider VER_AZ_THREE_DIGITS_PLUS_MNEMONIC = v -> {
        // "1.2.3 [4]"
        String mnemonic;
        switch (v.charAt(3)) {
            case 'B':
                mnemonic = "Beta";
                break;
            case 'A':
                mnemonic = "Alpha";
                break;
            default:
                mnemonic = "";
        }

        return v.charAt(0) + "." + v.charAt(1) + "." + v.charAt(2) + " " + mnemonic;
    };
    private static final VersionProvider VER_AZ_WEBTORRENT_STYLE = v -> {
        // "webtorrent"
        String version = "";
        if (v.charAt(0) == '0') {
            version += v.charAt(1);
            version += ".";
        } else {
            version += v.charAt(0);
            version += v.charAt(1);
            version += ".";
        }

        if (v.charAt(2) == '0') {
            version += v.charAt(3);
        } else {
            version += v.charAt(2);
            version += v.charAt(3);
        }

        return version;
    };
    private static final VersionProvider VER_AZ_THREE_ALPHANUMERIC_DIGITS = v -> "2.33.4";
    private static final VersionProvider VER_NONE = v -> "NO_VERSION";
    private static final VersionProvider VER_AZ_TWO_MAJ_TWO_MIN = v -> (v.charAt(0) + v.charAt(1)) + "." + v.charAt(2) + "." + v.charAt(3); // "12.34"
    private static final VersionProvider VER_AZ_SKIP_FIRST_ONE_MAJ_TWO_MIN = v -> v.charAt(0) + "." + v.charAt(1) + v.charAt(2); // "2.34"
    private static final VersionProvider VER_AZ_KTORRENT_STYLE = v -> "1.2.3=[RD].4";
    private static final VersionProvider VER_AZ_TRANSMISSION_STYLE = v -> {
        // "transmission"
        if (v.charAt(0) == '0' && v.charAt(1) == '0' && v.charAt(2) == '0') {
            return "0." + v.charAt(3);
        } else if (v.charAt(0) == '0' && v.charAt(1) == '0') {
            return "0." + v.charAt(2) + v.charAt(3);
        }

        return v.charAt(0) + "." + v.charAt(1) + v.charAt(2) + (v.charAt(3) == 'Z' || v.charAt(3) == 'X' ? '+' : "");
    };

    static {
        addAzStyle("A~", "Ares", VER_AZ_THREE_DIGITS);
        addAzStyle("AG", "Ares", VER_AZ_THREE_DIGITS);
        addAzStyle("AN", "Ares", VER_AZ_FOUR_DIGITS);
        addAzStyle("AR", "Ares"); // Ares is more likely than ArcticTorrent
        addAzStyle("AV", "Avicora");
        addAzStyle("AX", "BitPump", VER_AZ_TWO_MAJ_TWO_MIN);
        addAzStyle("AT", "Artemis");
        addAzStyle("AZ", "Vuze", VER_AZ_FOUR_DIGITS);
        addAzStyle("BB", "BitBuddy", "1.234");
        addAzStyle("BC", "BitComet", VER_AZ_SKIP_FIRST_ONE_MAJ_TWO_MIN);
        addAzStyle("BE", "BitTorrent SDK");
        addAzStyle("BF", "BitFlu", VER_NONE);
        addAzStyle("BG", "BTG", VER_AZ_FOUR_DIGITS);
        addAzStyle("bk", "BitKitten (libtorrent)");
        addAzStyle("BR", "BitRocket", "1.2(34)");
        addAzStyle("BS", "BTSlave");
        addAzStyle("BT", "BitTorrent", VER_AZ_THREE_DIGITS_PLUS_MNEMONIC);
        addAzStyle("BW", "BitWombat");
        addAzStyle("BX", "BittorrentX");
        addAzStyle("CB", "Shareaza Plus");
        addAzStyle("CD", "Enhanced CTorrent", VER_AZ_TWO_MAJ_TWO_MIN);
        addAzStyle("CT", "CTorrent", "1.2.34");
        addAzStyle("DP", "Propogate Data Client");
        addAzStyle("DE", "Deluge", VER_AZ_DELUGE);
        addAzStyle("EB", "EBit");
        addAzStyle("ES", "Electric Sheep", VER_AZ_THREE_DIGITS);
        addAzStyle("FC", "FileCroc");
        addAzStyle("FG", "FlashGet", VER_AZ_SKIP_FIRST_ONE_MAJ_TWO_MIN);
        addAzStyle("FT", "FoxTorrent/RedSwoosh");
        addAzStyle("FX", "Freebox BitTorrent");
        addAzStyle("GR", "GetRight", "1.2");
        addAzStyle("GS", "GSTorrent", VER_AZ_GS);
        addAzStyle("HL", "Halite", VER_AZ_THREE_DIGITS);
        addAzStyle("HN", "Hydranode");
        addAzStyle("KG", "KGet");
        addAzStyle("KT", "KTorrent", VER_AZ_KTORRENT_STYLE);
        addAzStyle("LC", "LeechCraft");
        addAzStyle("LH", "LH-ABC");
        addAzStyle("LK", "linkage", VER_AZ_THREE_DIGITS);
        addAzStyle("LP", "Lphant", VER_AZ_TWO_MAJ_TWO_MIN);
        addAzStyle("LT", "libtorrent (Rasterbar)", VER_AZ_THREE_ALPHANUMERIC_DIGITS);
        addAzStyle("lt", "libTorrent (Rakshasa)", VER_AZ_THREE_ALPHANUMERIC_DIGITS);
        addAzStyle("LW", "LimeWire", VER_NONE); // The "0001" bytes found after the LW commonly refers to the version of the BT protocol implemented. Documented here: http://www.limewire.org/wiki/index.php?title=BitTorrentRevision
        addAzStyle("MO", "MonoTorrent");
        addAzStyle("MP", "MooPolice", VER_AZ_THREE_DIGITS);
        addAzStyle("MR", "Miro");
        addAzStyle("MT", "MoonlightTorrent");
        addAzStyle("NE", "BT Next Evolution", VER_AZ_THREE_DIGITS);
        addAzStyle("NX", "Net Transport");
        addAzStyle("OS", "OneSwarm", VER_AZ_FOUR_DIGITS);
        addAzStyle("OT", "OmegaTorrent");
        addAzStyle("PC", "CacheLogic", "12.3-4");
        addAzStyle("PT", "Popcorn Time");
        addAzStyle("PD", "Pando");
        addAzStyle("PE", "PeerProject");
        addAzStyle("pX", "pHoeniX");
        addAzStyle("qB", "qBittorrent", VER_AZ_DELUGE);
        addAzStyle("QD", "qqdownload");
        addAzStyle("RT", "Retriever");
        addAzStyle("RZ", "RezTorrent");
        addAzStyle("S~", "Shareaza alpha/beta");
        addAzStyle("SB", "SwiftBit");
        addAzStyle("SD", "\u8FC5\u96F7\u5728\u7EBF (Xunlei)"); // Apparently, the English name of the client is "Thunderbolt".
        addAzStyle("SG", "GS Torrent", VER_AZ_FOUR_DIGITS);
        addAzStyle("SN", "ShareNET");
        addAzStyle("SP", "BitSpirit", VER_AZ_THREE_DIGITS); // >= 3.6
        addAzStyle("SS", "SwarmScope");
        addAzStyle("ST", "SymTorrent", "2.34");
        addAzStyle("st", "SharkTorrent");
        addAzStyle("SZ", "Shareaza");
        addAzStyle("TG", "Torrent GO");
        addAzStyle("TN", "Torrent.NET");
        addAzStyle("TR", "Transmission", VER_AZ_TRANSMISSION_STYLE);
        addAzStyle("TS", "TorrentStorm");
        addAzStyle("TT", "TuoTu", VER_AZ_THREE_DIGITS);
        addAzStyle("UL", "uLeecher!");
        addAzStyle("UE", "\u00B5Torrent Embedded", VER_AZ_THREE_DIGITS_PLUS_MNEMONIC);
        addAzStyle("UT", "\u00B5Torrent", VER_AZ_THREE_DIGITS_PLUS_MNEMONIC);
        addAzStyle("UM", "\u00B5Torrent Mac", VER_AZ_THREE_DIGITS_PLUS_MNEMONIC);
        addAzStyle("UW", "\u00B5Torrent Web", VER_AZ_THREE_DIGITS_PLUS_MNEMONIC);
        addAzStyle("WD", "WebTorrent Desktop", VER_AZ_WEBTORRENT_STYLE);
        addAzStyle("WT", "Bitlet");
        addAzStyle("WW", "WebTorrent", VER_AZ_WEBTORRENT_STYLE);
        addAzStyle("WY", "FireTorrent"); // formerly Wyzo.
        addAzStyle("VG", "\u54c7\u560E (Vagaa)", VER_AZ_FOUR_DIGITS);
        addAzStyle("XL", "\u8FC5\u96F7\u5728\u7EBF (Xunlei)"); // Apparently, the English name of the client is "Thunderbolt".
        addAzStyle("XT", "XanTorrent");
        addAzStyle("XF", "Xfplay", VER_AZ_TRANSMISSION_STYLE);
        addAzStyle("XX", "XTorrent", "1.2.34");
        addAzStyle("XC", "XTorrent", "1.2.34");
        addAzStyle("ZT", "ZipTorrent");
        addAzStyle("7T", "aTorrent");
        addAzStyle("ZO", "Zona", VER_AZ_FOUR_DIGITS);
        addAzStyle("#@", "Invalid PeerID");

        addShadowStyle("A", "ABC");
        addShadowStyle("O", "Osprey Permaseed");
        addShadowStyle("Q", "BTQueue");
        addShadowStyle("R", "Tribler");
        addShadowStyle("S", "Shad0w");
        addShadowStyle("T", "BitTornado");
        addShadowStyle("U", "UPnP NAT");

        addMainlineStyle("M", "Mainline");
        addMainlineStyle("Q", "Queen Bee");

        // Simple clients with no version number.
        addSimpleClient("\u00B5Torrent", "1.7.0 RC", "-UT170-"); // http://forum.utorrent.com/viewtopic.php?pid=260927#p260927
        addSimpleClient("Azureus", "1", "Azureus");
        addSimpleClient("Azureus", "2.0.3.2", "Azureus", 5);
        addSimpleClient("Aria", "2", "-aria2-");
        addSimpleClient("BitTorrent Plus!", "II", "PRC.P---");
        addSimpleClient("BitTorrent Plus!", "P87.P---");
        addSimpleClient("BitTorrent Plus!", "S587Plus");
        addSimpleClient("BitTyrant (Azureus Mod)", "AZ2500BT");
        addSimpleClient("Blizzard Downloader", "BLZ");
        addSimpleClient("BTGetit", "BG", 10);
        addSimpleClient("BTugaXP", "btuga");
        addSimpleClient("BTugaXP", "BTuga", 5);
        addSimpleClient("BTugaXP", "oernu");
        addSimpleClient("Deadman Walking", "BTDWV-");
        addSimpleClient("Deadman", "Deadman Walking-");
        addSimpleClient("External Webseed", "Ext");
        addSimpleClient("G3 Torrent", "-G3");
        addSimpleClient("GreedBT", "2.7.1", "271-");
        addSimpleClient("Hurricane Electric", "arclight");
        addSimpleClient("HTTP Seed", "-WS");
        addSimpleClient("JVtorrent", "10-------");
        addSimpleClient("Limewire", "LIME");
        addSimpleClient("Martini Man", "martini");
        addSimpleClient("Pando", "Pando");
        addSimpleClient("PeerApp", "PEERAPP");
        addSimpleClient("SimpleBT", "btfans", 4);
        addSimpleClient("Swarmy", "a00---0");
        addSimpleClient("Swarmy", "a02---0");
        addSimpleClient("Teeweety", "T00---0");
        addSimpleClient("TorrentTopia", "346-");
        addSimpleClient("XanTorrent", "DansClient");
        addSimpleClient("MediaGet", "-MG1");
        addSimpleClient("MediaGet", "2.1", "-MG21");

        /*
         * This is interesting - it uses Mainline style, except uses two characters instead of one.
         * And then - the particular numbering style it uses would actually break the way we decode
         * version numbers (our code is too hardcoded to "-x-y-z--" style version numbers).
         *
         * This should really be declared as a Mainline style peer ID, but I would have to
         * make my code more generic. Not a bad thing - just something I'm not doing right
         * now.
         */
        addSimpleClient("Amazon AWS S3", "S3-");

        // Simple clients with custom version schemes
        addSimpleClient("BitTorrent DNA", "DNA");
        addSimpleClient("Opera", "OP"); // Pre build 10000 versions
        addSimpleClient("Opera", "O"); // Post build 10000 versions
        addSimpleClient("Burst!", "Mbrst");
        addSimpleClient("TurboBT", "turbobt");
        addSimpleClient("BT Protocol Daemon", "btpd");
        addSimpleClient("Plus!", "Plus");
        addSimpleClient("XBT", "XBT");
        addSimpleClient("BitsOnWheels", "-BOW");
        addSimpleClient("eXeem", "eX");
        addSimpleClient("MLdonkey", "-ML");
        addSimpleClient("Bitlet", "BitLet");
        addSimpleClient("AllPeers", "AP");
        addSimpleClient("BTuga Revolution", "BTM");
        addSimpleClient("Rufus", "RS", 2);
        addSimpleClient("BitMagnet", "BM", 2); // BitMagnet - predecessor to Rufus
        addSimpleClient("QVOD", "QVOD");
        // Top-BT is based on BitTornado, but doesn"t quite stick to Shadow"s naming conventions,
        // so we'll use substring matching instead.
        addSimpleClient("Top-BT", "TB");
        addSimpleClient("Tixati", "TIX");
        // seems to have a sub-version encoded in following 3 bytes, not worked out how: "folx/1.0.456.591" : 2D 464C 3130 FF862D 486263574A43585F66314D5A
        addSimpleClient("folx", "-FL");
        addSimpleClient("\u00B5Torrent Mac", "-UM");
        addSimpleClient("\u00B5Torrent", "-UT"); // UT 3.4+
    }

    private PeerIdParser() {
    }

    private static void addAzStyle(String id, String client, VersionProvider version) {
        azStyleClients.put(id, client);
        azStyleClientVersions.put(client, version);
    }

    private static void addAzStyle(String id, String client) {
        addAzStyle(id, client, VER_AZ_FOUR_DIGITS);
    }

    private static void addAzStyle(String id, String client, String version) {
        addAzStyle(id, client, VersionProvider.of(version));
    }

    private static void addShadowStyle(String id, String client, String version) {
        addShadowStyle(id, client, VersionProvider.of(version));
    }

    private static void addShadowStyle(String id, String client, VersionProvider version) {
        shadowStyleClients.put(id, client);
        shadowStyleClientVersions.put(client, version);
    }

    private static void addShadowStyle(String id, String client) {
        addShadowStyle(id, client, VER_AZ_THREE_DIGITS);
    }

    private static void addMainlineStyle(String id, String client) {
        mainlineStyleClients.put(id, client);
    }

    private static void addSimpleClient(@NonNull String client, @NonNull String id, int position) {
        customStyleClients.add(new SimpleClient(id, client, null, position));
    }

    private static void addSimpleClient(@NonNull String client, @NonNull String version, @NonNull String id, int position) {
        customStyleClients.add(new SimpleClient(id, client, version, position));
    }

    private static void addSimpleClient(@NonNull String client, @NonNull String version, String id) {
        customStyleClients.add(new SimpleClient(id, client, version, 0));
    }

    private static void addSimpleClient(@NonNull String client, @NonNull String version) {
        customStyleClients.add(new SimpleClient(version, client, null, 0));
    }

    private static String getAzStyleClientName(String peerId) {
        return azStyleClients.get(peerId.substring(1, 3));
    }

    private static String getShadowStyleClientName(String peerId) {
        return shadowStyleClients.get(peerId.substring(0, 1));
    }

    private static String getMainlineStyleClientName(String peerId) {
        return mainlineStyleClients.get(peerId.substring(0, 1));
    }

    @Nullable
    private static SimpleClient getSimpleClient(String peerId) {
        for (int i = 0; i < customStyleClients.size(); ++i) {
            SimpleClient client = customStyleClients.get(i);
            if (peerId.startsWith(client.id, client.position))
                return client;
        }

        return null;
    }

    private static String getAzStyleClientVersion(String client, String peerId) {
        VersionProvider version = azStyleClientVersions.get(client);
        if (version == null) return null;

        return getAzStyleVersionNumber(peerId.substring(3, 7), version);
    }

    @Nullable
    public static Parsed parse(@NonNull String id) {
        String peerId;
        try {
            peerId = URLDecoder.decode(id, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }

        byte[] buffer = peerId.getBytes();

        if (isPossibleSpoofClient(peerId)) {
            Parsed client;
            if ((client = decodeBitSpiritClient(peerId)) != null) return client;
            if ((client = decodeBitCometClient(peerId, buffer)) != null) return client;
            return new Parsed("BitSpirit?", null);
        }

        // See if the client uses Az style identification
        if (isAzStyle(peerId)) {
            String client;
            if ((client = getAzStyleClientName(peerId)) != null) {
                String version = getAzStyleClientVersion(client, peerId);

                // Hack for fake ZipTorrent clients - there seems to be some clients
                // which use the same identifier, but they aren't valid ZipTorrent clients
                if (client.startsWith("ZipTorrent") && peerId.startsWith("bLAde", 8))
                    return new Parsed("Unknown [Fake: ZipTorrent]", version);

                // BitTorrent 6.0 Beta currently misidentifies itself
                if (client.equals("\u00B5Torrent") && Objects.equals(version, "6.0 Beta"))
                    return new Parsed("Mainline", "6.0 Beta");

                // If it's the rakshasa libtorrent, then it's probably rTorrent
                if (client.startsWith("libTorrent (Rakshasa)"))
                    return new Parsed(client + " / rTorrent*", version);

                return new Parsed(client, version);
            }
        }

        // See if the client uses Shadow style identification
        if (isShadowStyle(peerId)) {
            String client;
            if ((client = getShadowStyleClientName(peerId)) != null)
                return new Parsed(client, null);
        }

        // See if the client uses Mainline style identification
        if (isMainlineStyle(peerId)) {
            String client;
            if ((client = getMainlineStyleClientName(peerId)) != null)
                return new Parsed(client, null);
        }

        // Check for BitSpirit / BitComet disregarding spoof mode
        Parsed client;
        if ((client = decodeBitSpiritClient(peerId)) != null) return client;
        if ((client = decodeBitCometClient(peerId, buffer)) != null) return client;

        // See if the client identifies itself using a particular substring
        SimpleClient data = getSimpleClient(peerId);
        if (data != null)
            return new Parsed(data.client, data.version);

        // See if client is known to be awkward / nonstandard
        if ((client = identifyAwkwardClient(buffer)) != null)
            return client;

        return null;
    }

    private static boolean isAzStyle(String peerId) {
        if (peerId.charAt(0) != '-') return false;
        if (peerId.charAt(7) == '-') return true;

        /*
         * Hack for FlashGet - it doesn't use the trailing dash.
         * Also, LH-ABC has strayed into "forgetting about the delimiter" territory.
         *
         * In fact, the code to generate a peer ID for LH-ABC is based on BitTornado's,
         * yet tries to give an Az style peer ID... oh dear.
         *
         * BT Next Evolution seems to be in the same boat as well.
         *
         * KTorrent 3 appears to use a dash rather than a final character.
         */
        if (peerId.substring(1, 3).equals("FG")) return true;
        if (peerId.substring(1, 3).equals("LH")) return true;
        if (peerId.substring(1, 3).equals("NE")) return true;
        if (peerId.substring(1, 3).equals("KT")) return true;
        return peerId.substring(1, 3).equals("SP");

    }

    /**
     * Checking whether a peer ID is Shadow style or not is a bit tricky.
     * <p>
     * The BitTornado peer ID convention code is explained here:
     * http://forums.degreez.net/viewtopic.php?t=7070
     * <p>
     * The main thing we are interested in is the first six characters.
     * Although the other characters are base64 characters, there's no
     * guarantee that other clients which follow that style will follow
     * that convention (though the fact that some of these clients use
     * BitTornado in the core does blur the lines a bit between what is
     * "style" and what is just common across clients).
     * <p>
     * So if we base it on the version number information, there's another
     * problem - there isn't the use of absolute delimiters (no fixed dash
     * character, for example).
     * <p>
     * There are various things we can do to determine how likely the peer
     * ID is to be of that style, but for now, I'll keep it to a relatively
     * simple check.
     * <p>
     * We'll assume that no client uses the fifth version digit, so we'll
     * expect a dash. We'll also assume that no client has reached version 10
     * yet, so we expect the first two characters to be "letter,digit".
     * <p>
     * We've seen some clients which don't appear to contain any version
     * information, so we need to allow for that.
     */
    private static boolean isShadowStyle(String peerId) {
        if (peerId.charAt(5) != '-') return false;
        if (!Character.isLetter(peerId.charAt(0))) return false;
        if (!(Character.isDigit(peerId.charAt(1)) || peerId.charAt(1) == '-')) return false;

        // Find where the version number string ends.
        int lastVersionNumberIndex = 4;
        for (; lastVersionNumberIndex > 0; lastVersionNumberIndex--) {
            if (peerId.charAt(lastVersionNumberIndex) != '-')
                break;
        }

        // For each digit in the version string, check if it is a valid version identifier.
        for (int i = 1; i <= lastVersionNumberIndex; i++) {
            char c = peerId.charAt(i);
            if (c == '-') return false;
            if (!isAlphaNumeric(c)) return false;
        }

        return true;
    }

    private static boolean isMainlineStyle(String peerId) {
        /*
         * One of the following styles will be used:
         *   Mx-y-z--
         *   Mx-yy-z-
         */
        return peerId.charAt(2) == '-' && peerId.charAt(7) == '-' && (peerId.charAt(4) == '-' || peerId.charAt(5) == '-');
    }

    private static boolean isPossibleSpoofClient(String peerId) {
        return peerId.endsWith("UDP0") || peerId.endsWith("HTTPBT");
    }

    private static String getAzStyleVersionNumber(String peerId, VersionProvider version) {
        if (version != null) return version.version(peerId);
        else return null;
    }

    private static Parsed decodeBitSpiritClient(String peerId) {
        if (!peerId.substring(2, 4).equals("BS")) return null;

        char version = peerId.charAt(1);
        if (version == '0') version = '1';

        return new Parsed("BitSpirit", String.valueOf(version));
    }

    private static Parsed decodeBitCometClient(String peerId, byte[] buffer) {
        String modName;
        if (peerId.startsWith("exbc")) modName = "";
        else if (peerId.startsWith("FUTB")) modName = "(Solidox Mod)";
        else if (peerId.startsWith("xUTB")) modName = "(Mod 2)";
        else return null;

        boolean isBitlord = peerId.substring(6, 10).equals("LORD");

        // Older versions of BitLord are of the form x.yy, whereas new versions (1 and onwards),
        // are of the form x.y. BitComet is of the form x.yy
        String clientName = isBitlord ? "BitLord" : "BitComet";
        int majVersion = decodeNumericValueOfByte(buffer[4]);

        return new Parsed(clientName + modName, majVersion + "." + decodeNumericValueOfByte(buffer[5]));
    }

    private static Parsed identifyAwkwardClient(byte[] buffer) {
        int firstNonZeroIndex = 20;
        int i;

        for (i = 0; i < 20; ++i) {
            if (buffer[i] > 0) {
                firstNonZeroIndex = i;
                break;
            }
        }

        // Shareaza check
        if (firstNonZeroIndex == 0) {
            boolean isShareaza = true;
            for (i = 0; i < 16; ++i) {
                if (buffer[i] == 0) {
                    isShareaza = false;
                    break;
                }
            }

            if (isShareaza) {
                for (i = 16; i < 20; ++i) {
                    if (buffer[i] != (buffer[i % 16] ^ buffer[15 - (i % 16)])) {
                        isShareaza = false;
                        break;
                    }
                }

                if (isShareaza) return new Parsed("Shareaza", null);
            }
        }

        if (firstNonZeroIndex == 9 && buffer[9] == 3 && buffer[10] == 3 && buffer[11] == 3)
            return new Parsed("I2PSnark", null);

        if (firstNonZeroIndex == 12 && buffer[12] == 97 && buffer[13] == 97)
            return new Parsed("Experimental", "3.2.1b2");

        if (firstNonZeroIndex == 12 && buffer[12] == 0 && buffer[13] == 0)
            return new Parsed("Experimental", "3.1");

        if (firstNonZeroIndex == 12)
            return new Parsed("Mainline", null);

        return null;
    }

    private static boolean isAlphaNumeric(char s) {
        return Character.isDigit(s) || Character.isLetter(s) || s == '.';
    }

    private static int decodeNumericValueOfByte(byte b) {
        return b & 0xFF;
    }

    private interface VersionProvider {
        @NonNull
        static VersionProvider of(@NonNull String version) {
            return peerId -> version;
        }

        @NonNull
        String version(@NonNull String str);
    }

    private static class SimpleClient {
        public final String id;
        public final String client;
        public final String version;
        public final int position;

        SimpleClient(@NonNull String id, @NonNull String client, String version, int position) {
            this.id = id;
            this.client = client;
            this.version = version;
            this.position = position;
        }
    }

    public static class Parsed {
        public final String client;
        public final String version;

        Parsed(@NonNull String client, @Nullable String version) {
            this.client = client;
            this.version = version;
        }

        @NonNull
        @Override
        public String toString() {
            return client + (version != null ? (" " + version) : "");
        }
    }
}
