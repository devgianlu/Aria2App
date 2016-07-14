package com.gianlu.aria2app.Google;

import android.app.Activity;

import com.gianlu.aria2app.Utils;
import com.google.android.vending.licensing.AESObfuscator;
import com.google.android.vending.licensing.LicenseChecker;
import com.google.android.vending.licensing.LicenseCheckerCallback;
import com.google.android.vending.licensing.ServerManagedPolicy;

public class CheckerCallback implements LicenseCheckerCallback {
    private static LicenseChecker checker;
    private static byte[] SALT = {-45, 57, 33, -9, 12, -4, 99, 67, -25, 1, 77, 34, -20, 78, -49, 19, 59, 48, -6, 90};
    private Activity context;

    CheckerCallback(Activity context) {
        this.context = context;
    }

    public static void check(Activity context, String packageName, String deviceID) {
        String BASE64_RSA = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA00sW2ghC8Wr4ay1XN+YwtFpwcpxkHJelpBolNaIOU7eBLtxmSzASYBH90Rv6vu07tD+Jmfh6UcP88uBE7yriWXJ0BWYw3d9Ei7oaL3mucSq02rfb5g2ZqW7BflmaGZ13g8Tg/Li/UlrhtIzNmzxN9siaUKEH7loPUomVUD1UhW2DbR9euHk4dBrvVmP76nnKHHfi+2DqsYq3wCBenZnKy7yLhJGboIq11iNqF/5AIYKQaHpd1JJGMuZMDEtbve+xm+y5ve01CvRKvzofMYTEV9vZfta1IlvUu67/dS9JgsyBNohsIX3ed2YArgjaSTpYfjfA7DXy2raziT+qgmuW0QIDAQAB";
        checker = new LicenseChecker(context.getApplicationContext(), new ServerManagedPolicy(context, new AESObfuscator(SALT, packageName, deviceID)), BASE64_RSA);
        checker.checkAccess(new CheckerCallback(context));
    }

    public void allow(int reason) {
        checker.onDestroy();
    }

    @Override
    public void applicationError(int errorCode) {
        Utils.UIToast(context, Utils.TOAST_MESSAGES.FAILED_LICENSE_VERIFICATION, "Error code: " + errorCode);
        checker.onDestroy();
    }

    public void dontAllow(int reason) {
        Utils.UIToast(context, Utils.TOAST_MESSAGES.APPLICATION_NOT_LICENSED, "Reason: " + reason);
        checker.onDestroy();
    }
}
