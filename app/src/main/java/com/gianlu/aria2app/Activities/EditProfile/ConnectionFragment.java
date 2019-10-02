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
import com.google.android.material.textfield.TextInputLayout;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.X509Certificate;
import java.util.Timer;
import java.util.TimerTask;

import static com.gianlu.aria2app.Activities.EditProfile.InvalidFieldException.Where;

public class ConnectionFragment extends FieldErrorFragmentWithState implements CertificateInputView.ActivityProvider {
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
    public static ConnectionFragment getInstance(@NonNull Context context) {
        ConnectionFragment fragment = new ConnectionFragment();
        fragment.setRetainInstance(true);
        Bundle args = new Bundle();
        args.putString("title", context.getString(R.string.connection));
        fragment.setArguments(args);
        return fragment;
    }

    private static int radioIdFromConnMethod(@NonNull MultiProfile.ConnectionMethod method) {
        switch (method) {
            default:
            case HTTP:
                return R.id.editProfile_connectionMethod_http;
            case WEBSOCKET:
                return R.id.editProfile_connectionMethod_ws;
        }
    }

    @NonNull
    public static Bundle stateFromProfile(@NonNull MultiProfile.UserProfile profile) {
        Bundle bundle = new Bundle();
        bundle.putInt("connectionMethod", radioIdFromConnMethod(profile.connectionMethod));
        bundle.putString("address", profile.serverAddr);
        bundle.putString("port", String.valueOf(profile.serverPort));
        bundle.putString("endpoint", profile.serverEndpoint);
        bundle.putBoolean("encryption", profile.serverSsl);
        if (profile.serverSsl)
            bundle.putBundle("certificate", CertificateInputView.stateFromProfile(profile));
        return bundle;
    }

    @NonNull
    public static Fields validateStateAndCreateFields(@NonNull Bundle bundle, boolean partial) throws InvalidFieldException {
        MultiProfile.ConnectionMethod connectionMethod;
        switch (bundle.getInt("connectionMethod", R.id.editProfile_connectionMethod_http)) {
            case R.id.editProfile_connectionMethod_ws:
                connectionMethod = MultiProfile.ConnectionMethod.WEBSOCKET;
                break;
            default:
            case R.id.editProfile_connectionMethod_http:
                connectionMethod = MultiProfile.ConnectionMethod.HTTP;
                break;
        }

        String address = bundle.getString("address", null);
        if (address == null || (address = address.trim()).isEmpty())
            throw new InvalidFieldException(Where.CONNECTION, R.id.editProfile_address, R.string.addressEmpty);

        int port;
        try {
            String portStr = bundle.getString("port");
            if (portStr == null || (portStr = portStr.trim()).isEmpty())
                throw new Exception();

            port = Integer.parseInt(portStr);
            if (port <= 0 || port > 65535) throw new Exception();
        } catch (Exception ex) {
            throw new InvalidFieldException(Where.CONNECTION, R.id.editProfile_port, R.string.invalidPort);
        }

        String endpoint = bundle.getString("endpoint", null);
        if (endpoint == null || (endpoint = endpoint.trim()).isEmpty())
            throw new InvalidFieldException(Where.CONNECTION, R.id.editProfile_endpoint, R.string.endpointEmpty);

        boolean encryption = bundle.getBoolean("encryption", false);

        if (!NetUtils.isUrlValid(address, port, endpoint, encryption))
            throw new InvalidFieldException(Where.CONNECTION, R.id.editProfile_address, R.string.invalidCompleteAddress);

        if (partial)
            return new Fields(connectionMethod, address, port, endpoint, encryption, null, false);

        boolean hostnameVerifier = false;
        X509Certificate certificate = null;
        Bundle certBundle = bundle.getBundle("certificate");
        if (certBundle != null) {
            hostnameVerifier = certBundle.getBoolean("hostnameVerifier", false);
            certificate = (X509Certificate) certBundle.getSerializable("certificate");
        }

        return new Fields(connectionMethod, address, port, endpoint, encryption, certificate, hostnameVerifier);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        certificate.detachActivity();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt("connectionMethod", connectionMethod.getCheckedRadioButtonId());
        outState.putString("address", CommonUtils.getText(address));
        outState.putString("port", CommonUtils.getText(port));
        outState.putString("endpoint", CommonUtils.getText(endpoint));
        outState.putBoolean("encryption", encryption.isChecked());

        if (encryption.isChecked()) outState.putBundle("certificate", certificate.saveState());
        else outState.remove("certificate");
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle bundle) {
        connectionMethod.check(bundle.getInt("connectionMethod", R.id.editProfile_connectionMethod_http));

        CommonUtils.setText(address, bundle.getString("address"));
        CommonUtils.setText(port, bundle.getString("port"));
        CommonUtils.setText(endpoint, bundle.getString("endpoint", "/jsonrpc"));
        encryption.setChecked(bundle.getBoolean("encryption", false));
        certificate.restore(bundle.getBundle("certificate"), encryption.isChecked());
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
                                    addressFlag.setVisibility(View.VISIBLE);
                                    addressFlag.setImageDrawable(flags.loadFlag(requireContext(), details.countryCode));
                                }

                                @Override
                                public void onException(@NonNull Exception ex) {
                                    addressFlag.setVisibility(View.GONE);
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
            Fields fields = validateStateAndCreateFields(save(), true);

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

    @Override
    public void onFieldError(@IdRes int fieldId, String reason) {
        if (layout == null) return;

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
