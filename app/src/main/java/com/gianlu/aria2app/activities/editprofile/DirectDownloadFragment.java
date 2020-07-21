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
import com.google.android.material.textfield.TextInputLayout;

import java.net.URL;
import java.security.cert.X509Certificate;

import static com.gianlu.aria2app.activities.editprofile.InvalidFieldException.Where;

public class DirectDownloadFragment extends FieldErrorFragmentWithState implements CertificateInputView.ActivityProvider {
    private ScrollView layout;
    private CheckBox enableDirectDownload;
    private LinearLayout container;
    private TextInputLayout address;
    private CheckBox auth;
    private LinearLayout authContainer;
    private TextInputLayout username;
    private TextInputLayout password;
    private CheckBox encryption;
    private CertificateInputView certificate;

    @NonNull
    public static DirectDownloadFragment getInstance(@NonNull Context context) {
        DirectDownloadFragment fragment = new DirectDownloadFragment();
        fragment.setRetainInstance(true);
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.directDownload));
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    public static Bundle stateFromProfile(@NonNull MultiProfile.UserProfile profile) {
        MultiProfile.DirectDownload dd = profile.directDownload;

        Bundle bundle = new Bundle();
        bundle.putBoolean("enabled", dd != null);
        if (dd != null) {
            bundle.putString("address", dd.address);
            bundle.putBoolean("auth", dd.auth);
            bundle.putString("username", dd.username);
            bundle.putString("password", dd.password);
            bundle.putBoolean("encryption", dd.serverSsl);
            if (dd.serverSsl)
                bundle.putBundle("certificate", CertificateInputView.stateFromDirectDownload(dd));
        }

        return bundle;
    }

    @NonNull
    public static Fields validateStateAndCreateFields(@NonNull Bundle bundle) throws InvalidFieldException {
        MultiProfile.DirectDownload dd = null;
        if (bundle.getBoolean("enabled", false)) {
            String address = bundle.getString("address", null);
            try {
                new URL(address);
            } catch (Exception ex) {
                throw new InvalidFieldException(Where.DIRECT_DOWNLOAD, R.id.editProfile_dd_address, R.string.invalidAddress);
            }

            String username = null;
            String password = null;
            boolean auth = bundle.getBoolean("auth", false);
            if (auth) {
                username = bundle.getString("username", null);
                if (username == null || (username = username.trim()).isEmpty())
                    throw new InvalidFieldException(Where.DIRECT_DOWNLOAD, R.id.editProfile_dd_username, R.string.emptyUsername);

                password = bundle.getString("password", null);
                if (password == null || (password = password.trim()).isEmpty())
                    throw new InvalidFieldException(Where.DIRECT_DOWNLOAD, R.id.editProfile_dd_password, R.string.emptyPassword);
            }

            boolean encryption = bundle.getBoolean("encryption", false);

            boolean hostnameVerifier = false;
            X509Certificate certificate = null;
            Bundle certBundle = bundle.getBundle("certificate");
            if (certBundle != null) {
                hostnameVerifier = certBundle.getBoolean("hostnameVerifier", false);
                certificate = (X509Certificate) certBundle.getSerializable("certificate");
            }

            dd = new MultiProfile.DirectDownload(address, auth, username, password, encryption, certificate, hostnameVerifier);
        }

        return new Fields(dd);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean("enabled", enableDirectDownload.isChecked());
        outState.putString("address", CommonUtils.getText(address));
        outState.putBoolean("auth", auth.isChecked());
        outState.putString("username", CommonUtils.getText(username));
        outState.putString("password", CommonUtils.getText(password));
        outState.putBoolean("encryption", encryption.isChecked());

        if (encryption.isChecked()) outState.putBundle("certificate", certificate.saveState());
        else outState.remove("certificate");
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle bundle) {
        enableDirectDownload.setChecked(bundle.getBoolean("enabled", false));

        CommonUtils.setText(address, bundle.getString("address"));
        auth.setChecked(bundle.getBoolean("auth", false));
        CommonUtils.setText(username, bundle.getString("username"));
        CommonUtils.setText(password, bundle.getString("password"));

        encryption.setChecked(bundle.getBoolean("encryption", false));
        certificate.restore(bundle.getBundle("certificate"), encryption.isChecked());
    }

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
        address = layout.findViewById(R.id.editProfile_dd_address);
        CommonUtils.clearErrorOnEdit(address);
        auth = layout.findViewById(R.id.editProfile_dd_auth);
        auth.setOnCheckedChangeListener((buttonView, isChecked) -> authContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE));
        authContainer = layout.findViewById(R.id.editProfile_dd_authContainer);
        username = layout.findViewById(R.id.editProfile_dd_username);
        CommonUtils.clearErrorOnEdit(username);
        password = layout.findViewById(R.id.editProfile_dd_password);
        CommonUtils.clearErrorOnEdit(password);

        encryption = layout.findViewById(R.id.editProfile_dd_encryption);
        encryption.setOnCheckedChangeListener((buttonView, isChecked) -> certificate.setVisibility(isChecked ? View.VISIBLE : View.GONE));

        certificate = layout.findViewById(R.id.editProfile_dd_certificate);
        certificate.attachActivity(this);

        return layout;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CertificateInputView.CODE_PICK_CERT && resultCode == Activity.RESULT_OK && isAdded())
            certificate.loadCertificateUri(data.getData());
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