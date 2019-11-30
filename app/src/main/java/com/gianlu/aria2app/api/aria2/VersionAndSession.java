package com.gianlu.aria2app.api.aria2;

public class VersionAndSession {
    public final VersionInfo version;
    public final SessionInfo session;

    public VersionAndSession(VersionInfo version, SessionInfo session) {
        this.version = version;
        this.session = session;
    }
}
