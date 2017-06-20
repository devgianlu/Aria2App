package com.gianlu.aria2app.Activities.EditProfile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.gianlu.aria2app.Main.SharedFile;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

public class ConnectionFragment extends FieldErrorFragment {
    private final static int CODE_PICK_CERT = 1;
    private ScrollView layout;
    private TextView completeAddress;
    private RadioGroup connectionMethod;
    private TextInputLayout address;
    private TextInputLayout port;
    private TextInputLayout endpoint;
    private CheckBox encryption;
    private LinearLayout certificatePathContainer;
    private TextInputLayout certificatePath;

    public static ConnectionFragment getInstance(Context context, @Nullable MultiProfile.UserProfile edit) {
        ConnectionFragment fragment = new ConnectionFragment();
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.connection));
        if (edit != null) args.putSerializable("edit", edit);
        fragment.setArguments(args);
        return fragment;
    }

    @SuppressWarnings("ConstantConditions")
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        layout = (ScrollView) inflater.inflate(R.layout.edit_profile_connection_fragment, container, false);
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
                certificatePathContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                updateCompleteAddress();
                if (isChecked)
                    Utils.requestReadPermission(getActivity(), R.string.readExternalStorageRequest_certMessage, 11);
            }
        });
        certificatePathContainer = (LinearLayout) layout.findViewById(R.id.editProfile_certificatePathContainer);
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

        ImageButton pickCertificatePath = (ImageButton) layout.findViewById(R.id.editProfile_pickCertificatePath);
        pickCertificatePath.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(Intent.createChooser(new Intent(Intent.ACTION_GET_CONTENT)
                        .setType("*/*")
                        .addCategory(Intent.CATEGORY_OPENABLE), "Select the certificate"), CODE_PICK_CERT);
            }
        });

        MultiProfile.UserProfile edit = (MultiProfile.UserProfile) getArguments().getSerializable("edit");
        if (edit != null) {
            switch (edit.connectionMethod) {
                default:
                case HTTP:
                    connectionMethod.check(R.id.editProfile_connectionMethod_http);
                    break;
                case WEBSOCKET:
                    connectionMethod.check(R.id.editProfile_connectionMethod_ws);
                    break;
            }

            address.getEditText().setText(edit.serverAddr);
            port.getEditText().setText(String.valueOf(edit.serverPort));
            endpoint.getEditText().setText(edit.serverEndpoint);
            encryption.setChecked(edit.serverSSL);
            if (edit.serverSSL) certificatePath.getEditText().setText(edit.certificatePath);
        }

        created = true;

        return layout;
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Context context = getContext();
        if (requestCode == CODE_PICK_CERT && resultCode == Activity.RESULT_OK && certificatePath != null && context != null) {
            SharedFile file = Utils.accessUriFile(context, data.getData());
            if (file != null) certificatePath.getEditText().setText(file.file.getAbsolutePath());
        }
    }

    private void updateCompleteAddress() {
        if (!isAdded()) return;
        completeAddress.setVisibility(View.VISIBLE);

        try {
            Fields fields = getFields(getContext(), true);

            String protocol;
            switch (fields.connectionMethod) {
                case WEBSOCKET:
                    protocol = fields.encryption ? "wss" : "ws";
                    break;
                default:
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

    @SuppressWarnings("ConstantConditions")
    public Fields getFields(Context context, boolean partial) throws InvalidFieldException {
        if (!created) {
            MultiProfile.UserProfile edit = (MultiProfile.UserProfile) getArguments().getSerializable("edit");
            return new Fields(edit.connectionMethod, edit.serverAddr, edit.serverPort, edit.serverEndpoint, edit.serverSSL, edit.certificatePath);
        }

        MultiProfile.ConnectionMethod connectionMethod;
        switch (this.connectionMethod.getCheckedRadioButtonId()) {
            case R.id.editProfile_connectionMethod_ws:
                connectionMethod = MultiProfile.ConnectionMethod.WEBSOCKET;
                break;
            default:
            case R.id.editProfile_connectionMethod_http:
                connectionMethod = MultiProfile.ConnectionMethod.HTTP;
                break;
        }

        String address = this.address.getEditText().getText().toString().trim();
        if (address.isEmpty()) {
            throw new InvalidFieldException(getClass(), R.id.editProfile_address, context.getString(R.string.addressEmpty));
        }

        int port;
        try {
            port = Integer.parseInt(this.port.getEditText().getText().toString().trim());
            if (port <= 0 || port > 65535) throw new Exception();
        } catch (Exception ex) {
            throw new InvalidFieldException(getClass(), R.id.editProfile_port, context.getString(R.string.invalidPort));
        }

        String endpoint = this.endpoint.getEditText().getText().toString().trim();
        if (endpoint.isEmpty()) {
            throw new InvalidFieldException(getClass(), R.id.editProfile_address, context.getString(R.string.endpointEmpty));
        }

        boolean encryption = this.encryption.isChecked();

        if (partial) return new Fields(connectionMethod, address, port, endpoint, encryption, null);

        String certificatePath = null;
        if (encryption) {
            certificatePath = this.certificatePath.getEditText().getText().toString();
            if (certificatePath.isEmpty()) {
                throw new InvalidFieldException(getClass(), R.id.editProfile_certificatePath, context.getString(R.string.emptyCertificate));
            }

            File cert = new File(certificatePath);
            if (!cert.exists() || !cert.canRead())
                throw new InvalidFieldException(getClass(), R.id.editProfile_certificatePath, context.getString(R.string.invalidCertificate));
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
        public final MultiProfile.ConnectionMethod connectionMethod;
        public final String address;
        public final int port;
        public final String endpoint;
        public final boolean encryption;
        public final String certificatePath;

        public Fields(MultiProfile.ConnectionMethod connectionMethod, String address, int port, String endpoint, boolean encryption, @Nullable String certificatePath) {
            this.connectionMethod = connectionMethod;
            this.address = address;
            this.port = port;
            this.endpoint = endpoint;
            this.encryption = encryption;
            this.certificatePath = certificatePath;
        }
    }
}
