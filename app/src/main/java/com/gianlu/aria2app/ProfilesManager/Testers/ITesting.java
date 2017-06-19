package com.gianlu.aria2app.ProfilesManager.Testers;

import com.gianlu.aria2app.ProfilesManager.MultiProfile;

public interface ITesting {
    void onUpdate(String message);

    void onConnectionResult(NetProfileTester tester, MultiProfile.UserProfile profile, MultiProfile.TestStatus status);

    void onAria2Result(boolean successful, String message);

    void onEnd();
}
