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
import android.widget.RadioGroup;
import android.widget.TextView;

import com.gianlu.aria2app.ProfilesManager.UserProfile;
import com.gianlu.aria2app.R;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

public class ConnectionFragment extends FieldErrorFragment {
    private LinearLayout layout;
    private TextView completeAddress;
    private RadioGroup connectionMethod;
    private TextInputLayout address;
    private TextInputLayout port;
    private TextInputLayout endpoint;
    private CheckBox encryption;
    private TextInputLayout certificatePath;

    public static ConnectionFragment getInstance(Context context, @Nullable UserProfile edit) {
        ConnectionFragment fragment = new ConnectionFragment();
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.connection));
        if (edit != null) args.putSerializable("edit", edit);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        layout = (LinearLayout) inflater.inflate(R.layout.edit_profile_connection_fragment, container, false);
        completeAddress = (TextView) layout.findViewById(R.id.editProfile_completeAddress);
        connectionMethod = (RadioGroup) layout.findViewById(R.id.editProfile_connectionMethod);
        connectionMethod.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                updateCompleteAddress();
            }
        });
        address = (TextInputLayout) layout.findViewById(R.id.editProfile_address);
        address.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                address.setErrorEnabled(false);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                updateCompleteAddress();
            }
        });
        port = (TextInputLayout) layout.findViewById(R.id.editProfile_port);
        port.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                port.setErrorEnabled(false);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                updateCompleteAddress();
            }
        });
        endpoint = (TextInputLayout) layout.findViewById(R.id.editProfile_endpoint);
        endpoint.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                endpoint.setErrorEnabled(false);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                updateCompleteAddress();
            }
        });
        encryption = (CheckBox) layout.findViewById(R.id.editProfile_encryption);
        encryption.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                certificatePath.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                updateCompleteAddress();
                // TODO: Request read permission!!
            }
        });
        certificatePath = (TextInputLayout) layout.findViewById(R.id.editProfile_certificatePath);
        certificatePath.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                certificatePath.setErrorEnabled(false);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        return layout;
    }

    private void updateCompleteAddress() {
        completeAddress.setVisibility(View.VISIBLE);

        try {
            Fields fields = getFields(true);

            String protocol;
            switch (fields.connectionMethod) {
                default:
                case WEBSOCKET:
                    protocol = fields.encryption ? "wss" : "ws";
                    break;
                case HTTP:
                    protocol = fields.encryption ? "https" : "http";
                    break;
            }

            URI url = new URI(protocol, null, fields.address, fields.port, fields.endpoint, null, null);
            completeAddress.setText(url.toString());
        } catch (InvalidFieldException | URISyntaxException | NullPointerException ex) {
            completeAddress.setText(R.string.invalidCompleteAddress);
        }
    }

    public Fields getFields(boolean partial) throws InvalidFieldException {
        UserProfile.ConnectionMethod connectionMethod;
        switch (this.connectionMethod.getCheckedRadioButtonId()) {
            default:
            case R.id.editProfile_connectionMethod_ws:
                connectionMethod = UserProfile.ConnectionMethod.WEBSOCKET;
                break;
            case R.id.editProfile_connectionMethod_http:
                connectionMethod = UserProfile.ConnectionMethod.HTTP;
                break;
        }

        String address = this.address.getEditText().getText().toString().trim();
        if (address.isEmpty()) {
            throw new InvalidFieldException(getClass(), R.id.editProfile_address, getString(R.string.addressEmpty));
        }

        int port;
        try {
            port = Integer.parseInt(this.port.getEditText().getText().toString().trim());
            if (port <= 0 || port > 65535) throw new Exception();
        } catch (Exception ex) {
            throw new InvalidFieldException(getClass(), R.id.editProfile_port, getString(R.string.invalidPort));
        }

        String endpoint = this.endpoint.getEditText().getText().toString().trim();
        if (endpoint.isEmpty()) {
            throw new InvalidFieldException(getClass(), R.id.editProfile_address, getString(R.string.endpointEmpty));
        }

        boolean encryption = this.encryption.isChecked();

        if (partial) return new Fields(connectionMethod, address, port, endpoint, encryption, null);

        String certificatePath = null;
        if (encryption) {
            certificatePath = this.certificatePath.getEditText().getText().toString();
            if (certificatePath.isEmpty()) {
                throw new InvalidFieldException(getClass(), R.id.editProfile_certificatePath, getString(R.string.emptyCertificate));
            }

            File cert = new File(certificatePath);
            if (!cert.exists() || !cert.canRead())
                throw new InvalidFieldException(getClass(), R.id.editProfile_certificatePath, getString(R.string.invalidCertificate));
        }

        return new Fields(connectionMethod, address, port, endpoint, encryption, certificatePath);
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
        public final UserProfile.ConnectionMethod connectionMethod;
        public final String address;
        public final int port;
        public final String endpoint;
        public final boolean encryption;
        public final String certificatePath;

        public Fields(UserProfile.ConnectionMethod connectionMethod, String address, int port, String endpoint, boolean encryption, @Nullable String certificatePath) {
            this.connectionMethod = connectionMethod;
            this.address = address;
            this.port = port;
            this.endpoint = endpoint;
            this.encryption = encryption;
            this.certificatePath = certificatePath;
        }
    }
}
