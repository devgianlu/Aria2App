package com.gianlu.aria2app.ProfilesManager.Testers;

import android.content.Context;

import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.R;

public abstract class NetTester extends BaseTester {
    private final IProfileTester profileListener;

    NetTester(Context context, MultiProfile.UserProfile profile, IPublish listener) {
        super(context, profile, listener);
        this.profileListener = null;
    }

    protected NetTester(Context context, MultiProfile.UserProfile profile, IProfileTester profileListener) {
        super(context, profile, null);
        this.profileListener = profileListener;
    }

    protected void publishResult(MultiProfile.UserProfile profile, MultiProfile.TestStatus status) {
        if (profileListener != null) {
            profileListener.statusUpdated(profile.getParent().id, status);
            return;
        }

        switch (status.status) {
            case OFFLINE:
                publishMessage("Host is offline, check your connection parameters", R.color.red);
                break;
            case ERROR:
                publishMessage("Failed establishing a connection", R.color.red);
                break;
            case ONLINE:
                publishMessage("Host is online, connection parameters are configured correctly", R.color.green);
                break;
            default:
            case UNKNOWN:
                break;
        }

        if (status.ex != null) {
            if (status.ex.getMessage() == null) {
                Throwable cause = status.ex.getCause();
                if (cause.getMessage() != null) publishMessage(cause.getMessage(), R.color.red);
            } else {
                publishMessage(status.ex.getMessage(), R.color.red);
            }
        }

        if (status.latency != -1) publishPing(profile, status.latency);
    }

    protected void publishPing(MultiProfile.UserProfile profile, long ping) {
        if (profileListener != null) {
            profileListener.pingUpdated(profile.getParent().id, ping);
            return;
        }

        publishMessage("Latency is " + ping + "ms", android.R.color.tertiary_text_light);
    }

    public interface IProfileTester {
        void statusUpdated(String profileId, MultiProfile.TestStatus status);

        void pingUpdated(String profileId, long ping);
    }
}
