package com.gianlu.aria2app.NetIO.JTA2;

public interface ISession {
    void onSessionInfo(String sessionID);

    void onException(Exception exception);
}
