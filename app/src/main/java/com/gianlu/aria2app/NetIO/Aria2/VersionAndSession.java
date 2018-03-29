package com.gianlu.aria2app.NetIO.Aria2;

public class VersionAndSession {
    public final VersionInfo version;
    public final SessionInfo session;

    public VersionAndSession(VersionInfo version, SessionInfo session) {
        this.version = version;
        this.session = session;
    }
}
