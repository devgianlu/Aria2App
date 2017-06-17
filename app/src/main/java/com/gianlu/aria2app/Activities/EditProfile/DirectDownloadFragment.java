package com.gianlu.aria2app.Activities.EditProfile;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;

import java.net.URL;

public class DirectDownloadFragment extends FieldErrorFragment {
    private ScrollView layout;
    private CheckBox enableDirectDownload;
    private LinearLayout container;
    private TextInputLayout address;
    private CheckBox auth;
    private LinearLayout authContainer;
    private TextInputLayout username;
    private TextInputLayout password;

    public static DirectDownloadFragment getInstance(Context context, @Nullable MultiProfile.UserProfile edit) {
        DirectDownloadFragment fragment = new DirectDownloadFragment();
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.directDownload));
        if (edit != null) args.putSerializable("edit", edit);
        fragment.setArguments(args);
        return fragment;
    }

    @SuppressWarnings("ConstantConditions")
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle savedInstanceState) {
        layout = (ScrollView) inflater.inflate(R.layout.edit_profile_dd_fragment, parent, false);
        enableDirectDownload = (CheckBox) layout.findViewById(R.id.editProfile_enableDirectDownload);
        enableDirectDownload.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                container.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                if (isChecked) Utils.requestWritePermission(getActivity(), 10);
            }
        });
        container = (LinearLayout) layout.findViewById(R.id.editProfile_dd_container);
        address = (TextInputLayout) layout.findViewById(R.id.editProfile_dd_address);
        address.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                address.setErrorEnabled(false);
            }
        });
        auth = (CheckBox) layout.findViewById(R.id.editProfile_dd_auth);
        auth.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                authContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });
        authContainer = (LinearLayout) layout.findViewById(R.id.editProfile_dd_authContainer);
        username = (TextInputLayout) layout.findViewById(R.id.editProfile_dd_username);
        username.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                username.setErrorEnabled(false);
            }
        });
        password = (TextInputLayout) layout.findViewById(R.id.editProfile_dd_password);
        password.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                password.setErrorEnabled(false);
            }
        });

        MultiProfile.UserProfile edit = (MultiProfile.UserProfile) getArguments().getSerializable("edit");
        if (edit != null) {
            enableDirectDownload.setChecked(edit.isDirectDownloadEnabled());
            if (edit.isDirectDownloadEnabled()) {
                address.getEditText().setText(edit.directDownload.address);
                auth.setChecked(edit.directDownload.auth);
                if (edit.directDownload.auth) {
                    username.getEditText().setText(edit.directDownload.username);
                    password.getEditText().setText(edit.directDownload.password);
                }
            }
        }

        created = true;

        return layout;
    }

    @SuppressWarnings("ConstantConditions")
    public Fields getFields(Context context) throws InvalidFieldException {
        if (!created) {
            MultiProfile.UserProfile edit = (MultiProfile.UserProfile) getArguments().getSerializable("edit");
            return new Fields(edit.isDirectDownloadEnabled(), edit.directDownload);
        }

        MultiProfile.DirectDownload dd = null;
        boolean enableDirectDownload = this.enableDirectDownload.isChecked();
        if (enableDirectDownload) {
            String address = this.address.getEditText().getText().toString().trim();
            try {
                new URL(address);
            } catch (Exception ex) {
                throw new InvalidFieldException(getClass(), R.id.editProfile_dd_address, context.getString(R.string.invalidAddress));
            }

            String username = null;
            String password = null;
            boolean auth = this.auth.isChecked();
            if (auth) {
                username = this.username.getEditText().getText().toString().trim();
                if (username.isEmpty()) {
                    throw new InvalidFieldException(getClass(), R.id.editProfile_dd_username, context.getString(R.string.emptyUsername));
                }

                password = this.password.getEditText().getText().toString().trim();
                if (password.isEmpty()) {
                    throw new InvalidFieldException(getClass(), R.id.editProfile_dd_password, context.getString(R.string.emptyPassword));
                }
            }

            dd = new MultiProfile.DirectDownload(address, auth, username, password);
        }

        return new Fields(enableDirectDownload, dd);
    }

    @Override
    public void onFieldError(@IdRes int fieldId, String reason) {
        TextInputLayout inputLayout = (TextInputLayout) layout.findViewById(fieldId);
        if (inputLayout != null) {
            inputLayout.setErrorEnabled(true);
            inputLayout.setError(reason);
        }
    }

    public class Fields {
        public final boolean enableDirectDownload;
        public final MultiProfile.DirectDownload dd;

        public Fields(boolean enableDirectDownload, @Nullable MultiProfile.DirectDownload dd) {
            this.enableDirectDownload = enableDirectDownload;
            this.dd = dd;
        }
    }
}