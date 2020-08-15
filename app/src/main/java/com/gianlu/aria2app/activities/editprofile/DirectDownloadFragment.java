package com.gianlu.aria2app.activities.editprofile;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.gianlu.aria2app.R;
import com.gianlu.aria2app.profiles.MultiProfile;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.permissions.AskPermission;
import com.gianlu.commonutils.ui.Toaster;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputLayout;

import java.net.URL;
import java.security.cert.X509Certificate;

import static com.gianlu.aria2app.activities.editprofile.InvalidFieldException.Where;

public class DirectDownloadFragment extends FieldErrorFragmentWithState implements CertificateInputView.ActivityProvider {
    private ScrollView layout;
    private CheckBox enableDirectDownload;
    private LinearLayout container;
    private MaterialButtonToggleGroup typePick;
    private LinearLayout webContainer;
    private LinearLayout ftpContainer;
    private LinearLayout smbContainer;

    private TextInputLayout webAddress;
    private CheckBox webAuth;
    private LinearLayout webAuthContainer;
    private TextInputLayout webUsername;
    private TextInputLayout webPassword;
    private CheckBox webEncryption;
    private CertificateInputView webCertificate;

    @NonNull
    public static DirectDownloadFragment getInstance(@NonNull Context context) {
        DirectDownloadFragment fragment = new DirectDownloadFragment();
        fragment.setRetainInstance(true);
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.directDownload));
        fragment.setArguments(args);
        return fragment;
    }

    private static int radioIdFromType(@NonNull MultiProfile.DirectDownload.Type type) {
        switch (type) {
            default:
            case WEB:
                return R.id.editProfile_ddType_web;
            case FTP:
                return R.id.editProfile_ddType_ftp;
            case SMB:
                return R.id.editProfile_ddType_samba;
        }
    }

    //region Validation

    @NonNull
    public static Bundle stateFromProfile(@NonNull MultiProfile.UserProfile profile) {
        Bundle bundle = new Bundle();
        bundle.putBoolean("enabled", profile.directDownload != null);
        if (profile.directDownload != null) {
            bundle.putInt("type", radioIdFromType(profile.directDownload.type));
            switch (profile.directDownload.type) {
                case WEB:
                    MultiProfile.DirectDownload.Web web = profile.directDownload.web;
                    Bundle webBundle = new Bundle();
                    webBundle.putString("address", web.address);
                    webBundle.putBoolean("auth", web.auth);
                    webBundle.putString("username", web.username);
                    webBundle.putString("password", web.password);
                    webBundle.putBoolean("encryption", web.serverSsl);
                    if (web.serverSsl)
                        webBundle.putBundle("certificate", CertificateInputView.stateFromDirectDownload(web));
                    bundle.putBundle("web", webBundle);
                    break;
                case FTP:
                    // TODO
                    break;
                case SMB:
                    // TODO
                    break;
            }
        }

        return bundle;
    }

    @NonNull
    private static MultiProfile.DirectDownload.Web validateWebState(@NonNull Bundle webBundle) throws InvalidFieldException {
        String address = webBundle.getString("address", null);
        try {
            new URL(address);
        } catch (Exception ex) {
            throw new InvalidFieldException(Where.DIRECT_DOWNLOAD, R.id.editProfile_ddWeb_address, R.string.invalidAddress);
        }

        String username = null;
        String password = null;
        boolean auth = webBundle.getBoolean("auth", false);
        if (auth) {
            username = webBundle.getString("username", null);
            if (username == null || (username = username.trim()).isEmpty())
                throw new InvalidFieldException(Where.DIRECT_DOWNLOAD, R.id.editProfile_ddWeb_username, R.string.emptyUsername);

            password = webBundle.getString("password", null);
            if (password == null || (password = password.trim()).isEmpty())
                throw new InvalidFieldException(Where.DIRECT_DOWNLOAD, R.id.editProfile_ddWeb_password, R.string.emptyPassword);
        }

        boolean encryption = webBundle.getBoolean("encryption", false);

        boolean hostnameVerifier = false;
        X509Certificate certificate = null;
        Bundle certBundle = webBundle.getBundle("certificate");
        if (certBundle != null) {
            hostnameVerifier = certBundle.getBoolean("hostnameVerifier", false);
            certificate = (X509Certificate) certBundle.getSerializable("certificate");
        }

        return new MultiProfile.DirectDownload.Web(address, auth, username, password, encryption, certificate, hostnameVerifier);
    }

    @NonNull
    private static MultiProfile.DirectDownload.Ftp validateFtpState(@NonNull Bundle ftpBundle) {
        return null; // TODO
    }

    @NonNull
    private static MultiProfile.DirectDownload.Smb validateSmbState(@NonNull Bundle smbBundle) {
        return null; // TODO
    }

    @NonNull
    public static Fields validateStateAndCreateFields(@NonNull Bundle bundle) throws InvalidFieldException {
        MultiProfile.DirectDownload dd = null;
        if (bundle.getBoolean("enabled", false)) {
            switch (bundle.getInt("type", R.id.editProfile_ddType_web)) {
                case R.id.editProfile_ddType_web:
                    Bundle webBundle = bundle.getBundle("web");
                    if (webBundle != null)
                        dd = new MultiProfile.DirectDownload(null, validateWebState(webBundle), null);
                    break;
                case R.id.editProfile_ddType_ftp:
                    Bundle ftpBundle = bundle.getBundle("ftp");
                    if (ftpBundle != null)
                        dd = new MultiProfile.DirectDownload(validateFtpState(ftpBundle), null, null);
                    break;
                case R.id.editProfile_ddType_samba:
                    Bundle smbBundle = bundle.getBundle("smb");
                    if (smbBundle != null)
                        dd = new MultiProfile.DirectDownload(null, null, validateSmbState(smbBundle));
                    break;
            }
        }

        return new Fields(dd);
    }

    //endregion

    //region State

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean("enabled", enableDirectDownload.isChecked());
        outState.putInt("type", typePick.getCheckedButtonId());

        switch (typePick.getCheckedButtonId()) {
            case R.id.editProfile_ddType_web:
                Bundle webBundle = new Bundle();
                webBundle.putString("address", CommonUtils.getText(webAddress));
                webBundle.putBoolean("auth", webAuth.isChecked());
                webBundle.putString("username", CommonUtils.getText(webUsername));
                webBundle.putString("password", CommonUtils.getText(webPassword));
                webBundle.putBoolean("encryption", webEncryption.isChecked());
                if (webEncryption.isChecked())
                    webBundle.putBundle("certificate", webCertificate.saveState());
                else
                    webBundle.remove("certificate");

                outState.putBundle("web", webBundle);
                outState.remove("ftp");
                outState.remove("smb");
                break;
            case R.id.editProfile_ddType_ftp:
                // TODO
                break;
            case R.id.editProfile_ddType_samba:
                // TODO
                break;
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle bundle) {
        enableDirectDownload.setChecked(bundle.getBoolean("enabled", false));
        typePick.check(bundle.getInt("type", R.id.editProfile_ddType_web));

        Bundle webBundle = bundle.getBundle("web");
        if (webBundle != null) {
            CommonUtils.setText(webAddress, webBundle.getString("address"));
            webAuth.setChecked(webBundle.getBoolean("auth", false));
            CommonUtils.setText(webUsername, webBundle.getString("username"));
            CommonUtils.setText(webPassword, webBundle.getString("password"));

            webEncryption.setChecked(webBundle.getBoolean("encryption", false));
            webCertificate.restore(webBundle.getBundle("certificate"), webEncryption.isChecked());
        }

        Bundle ftpBundle = bundle.getBundle("ftp");
        if (ftpBundle != null) {
            // TODO
        }

        Bundle smbBundle = bundle.getBundle("smb");
        if (smbBundle != null) {
            // TODO
        }
    }

    //endregion

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle savedInstanceState) {
        layout = (ScrollView) inflater.inflate(R.layout.fragment_edit_profile_dd, parent, false);
        enableDirectDownload = layout.findViewById(R.id.editProfile_enableDirectDownload);
        enableDirectDownload.setOnCheckedChangeListener((buttonView, isChecked) -> {
            container.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (isChecked && getActivity() != null) {
                AskPermission.ask(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE, new AskPermission.Listener() {
                    @Override
                    public void permissionGranted(@NonNull String permission) {
                    }

                    @Override
                    public void permissionDenied(@NonNull String permission) {
                        showToast(Toaster.build().message(R.string.writePermissionDenied));
                    }

                    @Override
                    public void askRationale(@NonNull AlertDialog.Builder builder) {
                        builder.setTitle(R.string.writeExternalStorageRequest_title)
                                .setMessage(R.string.writeExternalStorageRequest_message);
                    }
                });
            }
        });

        container = layout.findViewById(R.id.editProfile_dd_container);
        typePick = layout.findViewById(R.id.editProfile_dd_type);
        typePick.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            switch (checkedId) {
                default:
                case R.id.editProfile_ddType_web:
                    webContainer.setVisibility(View.VISIBLE);
                    ftpContainer.setVisibility(View.GONE);
                    smbContainer.setVisibility(View.GONE);
                    break;
                case R.id.editProfile_ddType_ftp:
                    webContainer.setVisibility(View.GONE);
                    ftpContainer.setVisibility(View.VISIBLE);
                    smbContainer.setVisibility(View.GONE);
                    break;
                case R.id.editProfile_ddType_samba:
                    webContainer.setVisibility(View.GONE);
                    ftpContainer.setVisibility(View.GONE);
                    smbContainer.setVisibility(View.VISIBLE);
                    break;
            }
        });

        webContainer = layout.findViewById(R.id.editProfile_ddWeb_container);
        ftpContainer = layout.findViewById(R.id.editProfile_ddFtp_container);
        smbContainer = layout.findViewById(R.id.editProfile_ddSamba_container);

        //region Web
        webAddress = webContainer.findViewById(R.id.editProfile_ddWeb_address);
        CommonUtils.clearErrorOnEdit(webAddress);
        webAuth = webContainer.findViewById(R.id.editProfile_ddWeb_auth);
        webAuth.setOnCheckedChangeListener((buttonView, isChecked) -> webAuthContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE));
        webAuthContainer = webContainer.findViewById(R.id.editProfile_ddWeb_authContainer);
        webUsername = webContainer.findViewById(R.id.editProfile_ddWeb_username);
        CommonUtils.clearErrorOnEdit(webUsername);
        webPassword = webContainer.findViewById(R.id.editProfile_ddWeb_password);
        CommonUtils.clearErrorOnEdit(webPassword);

        webEncryption = webContainer.findViewById(R.id.editProfile_ddWeb_encryption);
        webEncryption.setOnCheckedChangeListener((buttonView, isChecked) -> webCertificate.setVisibility(isChecked ? View.VISIBLE : View.GONE));

        webCertificate = webContainer.findViewById(R.id.editProfile_ddWeb_certificate);
        webCertificate.attachActivity(this);
        //endregion

        //region FTP
        // TODO
        //endregion

        //region Samba
        // TODO
        //endregion

        return layout;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CertificateInputView.CODE_PICK_CERT && resultCode == Activity.RESULT_OK && isAdded())
            webCertificate.loadCertificateUri(data.getData()); // FIXME
    }

    @Override
    public void onFieldError(@IdRes int fieldId, String reason) {
        TextInputLayout inputLayout = layout.findViewById(fieldId);
        if (inputLayout != null) {
            inputLayout.setErrorEnabled(true);
            inputLayout.setError(reason);
        }
    }

    public static class Fields {
        public final MultiProfile.DirectDownload dd;

        public Fields(@Nullable MultiProfile.DirectDownload dd) {
            this.dd = dd;
        }
    }
}