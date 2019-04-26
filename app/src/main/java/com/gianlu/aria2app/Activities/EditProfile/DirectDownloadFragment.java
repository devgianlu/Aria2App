package com.gianlu.aria2app.Activities.EditProfile;

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

import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.AskPermission;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Toaster;
import com.google.android.material.textfield.TextInputLayout;

import java.net.URL;

public class DirectDownloadFragment extends FieldErrorFragment implements CertificateInputView.ActivityProvider {
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
    public static DirectDownloadFragment getInstance(Context context, @Nullable MultiProfile.UserProfile edit) {
        DirectDownloadFragment fragment = new DirectDownloadFragment();
        fragment.setRetainInstance(true);
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.directDownload));
        if (edit != null) args.putSerializable("edit", edit);
        fragment.setArguments(args);
        return fragment;
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
                        showToast(Toaster.build().message(R.string.writePermissionDenied).error(true));
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

        Bundle args = getArguments();
        MultiProfile.UserProfile edit;
        if (args != null && (edit = (MultiProfile.UserProfile) args.getSerializable("edit")) != null) {
            enableDirectDownload.setChecked(edit.directDownload != null);
            if (edit.directDownload != null) {
                CommonUtils.setText(address, edit.directDownload.address);
                auth.setChecked(edit.directDownload.auth);
                encryption.setChecked(edit.directDownload.serverSsl);
                if (edit.directDownload.certificate != null && edit.directDownload.serverSsl) {
                    certificate.showCertificateDetails(edit.directDownload.certificate);
                    certificate.hostnameVerifier(edit.directDownload.hostnameVerifier);
                }

                if (edit.directDownload.auth) {
                    CommonUtils.setText(username, edit.directDownload.username);
                    CommonUtils.setText(password, edit.directDownload.password);
                }
            }
        }

        created = true;

        return layout;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CertificateInputView.CODE_PICK_CERT && resultCode == Activity.RESULT_OK && isAdded())
            certificate.loadCertificateUri(data.getData());
    }

    @Nullable
    public Fields getFields(@NonNull Context context) throws InvalidFieldException {
        if (!created) {
            Bundle args = getArguments();
            MultiProfile.UserProfile edit;
            if (args == null || (edit = (MultiProfile.UserProfile) getArguments().getSerializable("edit")) == null)
                return null;
            else
                return new Fields(edit.directDownload);
        }

        MultiProfile.DirectDownload dd = null;
        if (this.enableDirectDownload.isChecked()) {
            String address = CommonUtils.getText(this.address).trim();
            try {
                new URL(address);
            } catch (Exception ex) {
                throw new InvalidFieldException(getClass(), R.id.editProfile_dd_address, context.getString(R.string.invalidAddress));
            }

            String username = null;
            String password = null;
            boolean auth = this.auth.isChecked();
            if (auth) {
                username = CommonUtils.getText(this.username).trim();
                if (username.isEmpty())
                    throw new InvalidFieldException(getClass(), R.id.editProfile_dd_username, context.getString(R.string.emptyUsername));

                password = CommonUtils.getText(this.password).trim();
                if (password.isEmpty())
                    throw new InvalidFieldException(getClass(), R.id.editProfile_dd_password, context.getString(R.string.emptyPassword));
            }

            dd = new MultiProfile.DirectDownload(address, auth, username, password, encryption.isChecked(), certificate.lastLoadedCertificate(), certificate.hostnameVerifier());
        }

        return new Fields(dd);
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