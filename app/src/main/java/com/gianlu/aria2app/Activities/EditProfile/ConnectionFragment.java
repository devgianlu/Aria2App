package com.gianlu.aria2app.Activities.EditProfile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.aria2app.CountryFlags;
import com.gianlu.aria2app.NetIO.Geolocalization.GeoIP;
import com.gianlu.aria2app.NetIO.Geolocalization.IPDetails;
import com.gianlu.aria2app.NetIO.NetUtils;
import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;
import com.google.android.material.textfield.TextInputLayout;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.X509Certificate;
import java.util.Timer;
import java.util.TimerTask;

public class ConnectionFragment extends FieldErrorFragment implements CertificateInputView.ActivityProvider {
    private final CountryFlags flags = CountryFlags.get();
    private ScrollView layout;
    private TextView completeAddress;
    private RadioGroup connectionMethod;
    private TextInputLayout address;
    private TextInputLayout port;
    private TextInputLayout endpoint;
    private CheckBox encryption;
    private CertificateInputView certificate;
    private ImageView addressFlag;

    @NonNull
    public static ConnectionFragment getInstance(Context context, @Nullable MultiProfile.UserProfile edit) {
        ConnectionFragment fragment = new ConnectionFragment();
        fragment.setRetainInstance(true);
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.connection));
        if (edit != null) args.putSerializable("edit", edit);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        certificate.detachActivity();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        layout = (ScrollView) inflater.inflate(R.layout.fragment_edit_profile_connection, container, false);
        completeAddress = layout.findViewById(R.id.editProfile_completeAddress);
        addressFlag = layout.findViewById(R.id.editProfile_addressFlag);
        connectionMethod = layout.findViewById(R.id.editProfile_connectionMethod);
        connectionMethod.setOnCheckedChangeListener((group, checkedId) -> updateCompleteAddress());
        address = layout.findViewById(R.id.editProfile_address);
        CommonUtils.getEditText(address).addTextChangedListener(new TextWatcher() {
            private final Timer timer = new Timer();
            private TimerTask task;
            private String lastAddress;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                address.setErrorEnabled(false);
                updateCompleteAddress();
                lastAddress = s.toString();

                if (task != null) task.cancel();
                task = new TimerTask() {
                    @Override
                    public void run() {
                        if (lastAddress != null) {
                            GeoIP.get().getIPDetails(lastAddress, getActivity(), new GeoIP.OnIpDetails() {
                                @Override
                                public void onDetails(@NonNull IPDetails details) {
                                    if (!isAdded()) return;

                                        addressFlag.setVisibility(View.VISIBLE);
                                    addressFlag.setImageDrawable(flags.loadFlag(requireContext(), details.countryCode));
                                }

                                @Override
                                public void onException(@NonNull Exception ex) {
                                    addressFlag.setVisibility(View.GONE);
                                    Logging.log(ex);
                                }
                            });
                        }
                    }
                };

                timer.schedule(task, 500);
            }
        });
        port = layout.findViewById(R.id.editProfile_port);
        CommonUtils.getEditText(port).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                port.setErrorEnabled(false);
                updateCompleteAddress();
            }
        });
        endpoint = layout.findViewById(R.id.editProfile_endpoint);
        CommonUtils.getEditText(endpoint).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                endpoint.setErrorEnabled(false);
                updateCompleteAddress();
            }
        });
        encryption = layout.findViewById(R.id.editProfile_encryption);
        encryption.setOnCheckedChangeListener((buttonView, isChecked) -> {
            certificate.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            updateCompleteAddress();
        });

        certificate = layout.findViewById(R.id.editProfile_certificate);
        certificate.attachActivity(this);

        Bundle args = getArguments();
        MultiProfile.UserProfile edit;
        if (args != null && (edit = (MultiProfile.UserProfile) args.getSerializable("edit")) != null) {
            switch (edit.connectionMethod) {
                default:
                case HTTP:
                    connectionMethod.check(R.id.editProfile_connectionMethod_http);
                    break;
                case WEBSOCKET:
                    connectionMethod.check(R.id.editProfile_connectionMethod_ws);
                    break;
            }

            CommonUtils.setText(address, edit.serverAddr);
            CommonUtils.setText(port, String.valueOf(edit.serverPort));
            CommonUtils.setText(endpoint, edit.serverEndpoint);
            encryption.setChecked(edit.serverSsl);
            certificate.hostnameVerifier(edit.hostnameVerifier);
            if (edit.serverSsl && edit.certificate != null)
                certificate.showCertificateDetails(edit.certificate);
        }

        created = true;

        return layout;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CertificateInputView.CODE_PICK_CERT && resultCode == Activity.RESULT_OK && isAdded())
            certificate.loadCertificateUri(data.getData());
    }

    private void updateCompleteAddress() {
        if (!isAdded()) return;

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
            completeAddress.setVisibility(View.VISIBLE);
        } catch (InvalidFieldException | URISyntaxException | NullPointerException ex) {
            completeAddress.setVisibility(View.GONE);
        }
    }

    public Fields getFields(Context context, boolean partial) throws InvalidFieldException {
        if (!created || !isAdded()) {
            Bundle args = getArguments();
            MultiProfile.UserProfile edit;
            if (args == null || (edit = (MultiProfile.UserProfile) getArguments().getSerializable("edit")) == null)
                return null;
            else
                return new Fields(edit.connectionMethod, edit.serverAddr, edit.serverPort, edit.serverEndpoint, edit.serverSsl, edit.certificate, edit.hostnameVerifier);
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

        String address = CommonUtils.getText(this.address).trim();
        if (address.isEmpty())
            throw new InvalidFieldException(getClass(), R.id.editProfile_address, context.getString(R.string.addressEmpty));

        int port;
        try {
            port = Integer.parseInt(CommonUtils.getText(this.port).trim());
            if (port <= 0 || port > 65535) throw new Exception();
        } catch (Exception ex) {
            throw new InvalidFieldException(getClass(), R.id.editProfile_port, context.getString(R.string.invalidPort));
        }

        String endpoint = CommonUtils.getText(this.endpoint).trim();
        if (endpoint.isEmpty())
            throw new InvalidFieldException(getClass(), R.id.editProfile_endpoint, context.getString(R.string.endpointEmpty));

        boolean encryption = this.encryption.isChecked();

        if (!NetUtils.isUrlValid(address, port, endpoint, encryption))
            throw new InvalidFieldException(getClass(), R.id.editProfile_address, getString(R.string.invalidCompleteAddress));

        if (partial)
            return new Fields(connectionMethod, address, port, endpoint, encryption, null, false);

        return new Fields(connectionMethod, address, port, endpoint, encryption, certificate.lastLoadedCertificate(), certificate.hostnameVerifier());
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
        public final MultiProfile.ConnectionMethod connectionMethod;
        public final String address;
        public final int port;
        public final String endpoint;
        public final boolean encryption;
        public final X509Certificate certificate;
        public final boolean hostnameVerifier;

        public Fields(MultiProfile.ConnectionMethod connectionMethod, String address, int port, String endpoint, boolean encryption, @Nullable X509Certificate certificate, boolean hostnameVerifier) {
            this.connectionMethod = connectionMethod;
            this.address = address;
            this.port = port;
            this.endpoint = endpoint;
            this.encryption = encryption;
            this.certificate = certificate;
            this.hostnameVerifier = hostnameVerifier;
        }
    }
}
