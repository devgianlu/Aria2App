package com.gianlu.aria2app.activities.editprofile;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.gianlu.aria2app.R;
import com.gianlu.aria2app.downloader.SftpHelper;
import com.gianlu.aria2app.profiles.MultiProfile;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.permissions.AskPermission;
import com.gianlu.commonutils.ui.Toaster;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputLayout;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.JSchException;

import java.net.URL;
import java.security.cert.X509Certificate;

import static com.gianlu.aria2app.activities.editprofile.InvalidFieldException.Where;

public class DirectDownloadFragment extends FieldErrorFragmentWithState implements CertificateInputView.ActivityProvider {
    private ScrollView layout;
    private CheckBox enableDirectDownload;
    private LinearLayout container;
    private MaterialButtonToggleGroup typePick;
    private LinearLayout encryptionContainer;
    private CheckBox encryptionEnabled;
    private CertificateInputView certificate;
    private LinearLayout webContainer;
    private LinearLayout ftpContainer;
    private LinearLayout sftpContainer;
    private LinearLayout smbContainer;

    private TextInputLayout webAddress;
    private CheckBox webAuth;
    private LinearLayout webAuthContainer;
    private TextInputLayout webUsername;
    private TextInputLayout webPassword;

    private TextInputLayout ftpHost;
    private TextInputLayout ftpPort;
    private TextInputLayout ftpPath;
    private TextInputLayout ftpUsername;
    private TextInputLayout ftpPassword;

    private TextInputLayout sftpHost;
    private TextInputLayout sftpPort;
    private TextInputLayout sftpPath;
    private TextInputLayout sftpUsername;
    private TextInputLayout sftpPassword;
    private Button sftpVerify;
    private HostKey sftpHostKey;

    private TextInputLayout smbHost;
    private CheckBox smbAnonymous;
    private LinearLayout smbAuth;
    private TextInputLayout smbUsername;
    private TextInputLayout smbPassword;
    private TextInputLayout smbDomain;
    private TextInputLayout smbShare;
    private TextInputLayout smbPath;

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
            case SFTP:
                return R.id.editProfile_ddType_sftp;
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
                    MultiProfile.DirectDownload.Ftp ftp = profile.directDownload.ftp;
                    Bundle ftpBundle = new Bundle();
                    ftpBundle.putString("host", ftp.hostname);
                    ftpBundle.putString("port", String.valueOf(ftp.port));
                    ftpBundle.putString("path", ftp.path);
                    ftpBundle.putString("username", ftp.username);
                    ftpBundle.putString("password", ftp.password);
                    ftpBundle.putBoolean("encryption", ftp.serverSsl);
                    if (ftp.serverSsl)
                        ftpBundle.putBundle("certificate", CertificateInputView.stateFromDirectDownload(ftp));
                    bundle.putBundle("ftp", ftpBundle);
                    break;
                case SFTP:
                    MultiProfile.DirectDownload.Sftp sftp = profile.directDownload.sftp;
                    Bundle sftpBundle = new Bundle();
                    sftpBundle.putString("host", sftp.hostname);
                    sftpBundle.putString("port", String.valueOf(sftp.port));
                    sftpBundle.putString("username", sftp.username);
                    sftpBundle.putString("password", sftp.password);
                    sftpBundle.putString("hostKey", sftp.hostKey);
                    sftpBundle.putString("path", sftp.path);
                    bundle.putBundle("sftp", sftpBundle);
                    break;
                case SMB:
                    MultiProfile.DirectDownload.Smb smb = profile.directDownload.smb;
                    Bundle smbBundle = new Bundle();
                    smbBundle.putString("host", smb.hostname);
                    smbBundle.putBoolean("anonymous", smb.anonymous);
                    smbBundle.putString("username", smb.username);
                    smbBundle.putString("password", smb.password);
                    smbBundle.putString("domain", smb.domain);
                    smbBundle.putString("shareName", smb.shareName);
                    smbBundle.putString("path", smb.path);
                    bundle.putBundle("smb", smbBundle);
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
    private static MultiProfile.DirectDownload.Ftp validateFtpState(@NonNull Bundle ftpBundle) throws InvalidFieldException {
        String host = ftpBundle.getString("host");
        if (host == null || host.isEmpty())
            throw new InvalidFieldException(Where.DIRECT_DOWNLOAD, R.id.editProfile_ddFtp_host, R.string.invalidHost);

        int port;
        String portStr = ftpBundle.getString("port", "");
        try {
            port = Integer.parseInt(portStr);
        } catch (Exception ex) {
            throw new InvalidFieldException(Where.DIRECT_DOWNLOAD, R.id.editProfile_ddFtp_port, R.string.invalidPort);
        }

        if (port <= 0 || port > 65535)
            throw new InvalidFieldException(Where.CONNECTION, R.id.editProfile_ddFtp_port, R.string.invalidPort);

        String username = ftpBundle.getString("username");
        if (username == null || username.isEmpty())
            throw new InvalidFieldException(Where.DIRECT_DOWNLOAD, R.id.editProfile_ddFtp_username, R.string.invalidUsername);

        String password = ftpBundle.getString("password");
        if (password == null) password = "";

        boolean encryption = ftpBundle.getBoolean("encryption", false);
        boolean hostnameVerifier = false;
        X509Certificate certificate = null;
        Bundle certBundle = ftpBundle.getBundle("certificate");
        if (certBundle != null) {
            hostnameVerifier = certBundle.getBoolean("hostnameVerifier", false);
            certificate = (X509Certificate) certBundle.getSerializable("certificate");
        }

        String path = ftpBundle.getString("path", "/");

        return new MultiProfile.DirectDownload.Ftp(host, port, path, username, password, encryption, certificate, hostnameVerifier);
    }

    @NonNull
    private static MultiProfile.DirectDownload.Sftp validateSftpState(@NonNull Bundle sftpBundle) throws InvalidFieldException {
        String host = sftpBundle.getString("host");
        if (host == null || host.isEmpty())
            throw new InvalidFieldException(Where.DIRECT_DOWNLOAD, R.id.editProfile_ddSftp_host, R.string.invalidHost);

        int port;
        String portStr = sftpBundle.getString("port", "");
        try {
            port = Integer.parseInt(portStr);
        } catch (Exception ex) {
            throw new InvalidFieldException(Where.DIRECT_DOWNLOAD, R.id.editProfile_ddSftp_port, R.string.invalidPort);
        }

        if (port <= 0 || port > 65535)
            throw new InvalidFieldException(Where.CONNECTION, R.id.editProfile_ddSftp_port, R.string.invalidPort);

        String username = sftpBundle.getString("username");
        if (username == null || username.isEmpty())
            throw new InvalidFieldException(Where.DIRECT_DOWNLOAD, R.id.editProfile_ddSftp_username, R.string.invalidUsername);

        String password = sftpBundle.getString("password");
        if (password == null) password = "";

        String path = sftpBundle.getString("path", "/");

        return new MultiProfile.DirectDownload.Sftp(host, port, path, username, password, sftpBundle.getString("hostKey", ""));
    }

    @NonNull
    private static MultiProfile.DirectDownload.Smb validateSmbState(@NonNull Bundle smbBundle) throws InvalidFieldException {
        String host = smbBundle.getString("host");
        if (host == null || host.isEmpty())
            throw new InvalidFieldException(Where.DIRECT_DOWNLOAD, R.id.editProfile_ddSamba_host, R.string.invalidHost);

        String username = "";
        String password = "";
        String domain = "";
        boolean anonymous = smbBundle.getBoolean("anonymous");
        if (!anonymous) {
            username = smbBundle.getString("username");
            if (username == null || username.isEmpty())
                throw new InvalidFieldException(Where.DIRECT_DOWNLOAD, R.id.editProfile_ddSamba_username, R.string.invalidUsername);

            password = smbBundle.getString("password", "");
            domain = smbBundle.getString("domain", "");
        }

        String shareName = smbBundle.getString("shareName");
        if (shareName == null || shareName.isEmpty())
            throw new InvalidFieldException(Where.DIRECT_DOWNLOAD, R.id.editProfile_ddSamba_share, R.string.invalidShareName);

        String path = smbBundle.getString("path", "/");

        return new MultiProfile.DirectDownload.Smb(host, anonymous, username, password, domain, shareName, path);
    }

    @NonNull
    public static Fields validateStateAndCreateFields(@NonNull Bundle bundle) throws InvalidFieldException {
        MultiProfile.DirectDownload dd = null;
        if (bundle.getBoolean("enabled", false)) {
            switch (bundle.getInt("type", R.id.editProfile_ddType_web)) {
                case R.id.editProfile_ddType_web:
                    Bundle webBundle = bundle.getBundle("web");
                    if (webBundle != null)
                        dd = new MultiProfile.DirectDownload(validateWebState(webBundle), null, null, null);
                    break;
                case R.id.editProfile_ddType_ftp:
                    Bundle ftpBundle = bundle.getBundle("ftp");
                    if (ftpBundle != null)
                        dd = new MultiProfile.DirectDownload(null, validateFtpState(ftpBundle), null, null);
                    break;
                case R.id.editProfile_ddType_sftp:
                    Bundle sftpBundle = bundle.getBundle("sftp");
                    if (sftpBundle != null)
                        dd = new MultiProfile.DirectDownload(null, null, validateSftpState(sftpBundle), null);
                    break;
                case R.id.editProfile_ddType_samba:
                    Bundle smbBundle = bundle.getBundle("smb");
                    if (smbBundle != null)
                        dd = new MultiProfile.DirectDownload(null, null, null, validateSmbState(smbBundle));
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
                webBundle.putBoolean("encryption", encryptionEnabled.isChecked());
                if (encryptionEnabled.isChecked())
                    webBundle.putBundle("certificate", certificate.saveState());
                else
                    webBundle.remove("certificate");

                outState.putBundle("web", webBundle);
                outState.remove("ftp");
                outState.remove("sftp");
                outState.remove("smb");
                break;
            case R.id.editProfile_ddType_ftp:
                Bundle ftpBundle = new Bundle();
                ftpBundle.putString("host", CommonUtils.getText(ftpHost));
                ftpBundle.putString("port", CommonUtils.getText(ftpPort));
                ftpBundle.putString("path", CommonUtils.getText(ftpPath));
                ftpBundle.putString("username", CommonUtils.getText(ftpUsername));
                ftpBundle.putString("password", CommonUtils.getText(ftpPassword));
                ftpBundle.putBoolean("encryption", encryptionEnabled.isChecked());
                if (encryptionEnabled.isChecked())
                    ftpBundle.putBundle("certificate", certificate.saveState());
                else
                    ftpBundle.remove("certificate");

                outState.putBundle("ftp", ftpBundle);
                outState.remove("web");
                outState.remove("sftp");
                outState.remove("smb");
                break;
            case R.id.editProfile_ddType_sftp:
                Bundle sftpBundle = new Bundle();
                sftpBundle.putString("host", CommonUtils.getText(sftpHost));
                sftpBundle.putString("port", CommonUtils.getText(sftpPort));
                sftpBundle.putString("path", CommonUtils.getText(sftpPath));
                sftpBundle.putString("username", CommonUtils.getText(sftpUsername));
                sftpBundle.putString("password", CommonUtils.getText(sftpPassword));
                sftpBundle.putString("hostKey", SftpHelper.toString(sftpHostKey));
                outState.putBundle("sftp", sftpBundle);
                outState.remove("web");
                outState.remove("ftp");
                outState.remove("smb");
                break;
            case R.id.editProfile_ddType_samba:
                Bundle smbBundle = new Bundle();
                smbBundle.putString("host", CommonUtils.getText(smbHost));
                smbBundle.putBoolean("anonymous", smbAnonymous.isChecked());
                smbBundle.putString("username", CommonUtils.getText(smbUsername));
                smbBundle.putString("password", CommonUtils.getText(smbPassword));
                smbBundle.putString("domain", CommonUtils.getText(smbDomain));
                smbBundle.putString("shareName", CommonUtils.getText(smbShare));
                smbBundle.putString("path", CommonUtils.getText(smbPath));
                outState.putBundle("smb", smbBundle);
                outState.remove("web");
                outState.remove("ftp");
                outState.remove("sftp");
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

            encryptionEnabled.setChecked(webBundle.getBoolean("encryption", false));
            certificate.restore(webBundle.getBundle("certificate"), encryptionEnabled.isChecked());
        }

        Bundle ftpBundle = bundle.getBundle("ftp");
        if (ftpBundle != null) {
            CommonUtils.setText(ftpHost, ftpBundle.getString("host"));
            CommonUtils.setText(ftpPort, ftpBundle.getString("port"));
            CommonUtils.setText(ftpUsername, ftpBundle.getString("username"));
            CommonUtils.setText(ftpPassword, ftpBundle.getString("password"));
            CommonUtils.setText(ftpPath, ftpBundle.getString("path", "/"));

            encryptionEnabled.setChecked(ftpBundle.getBoolean("encryption", false));
            certificate.restore(ftpBundle.getBundle("certificate"), encryptionEnabled.isChecked());
        }

        Bundle sftpBundle = bundle.getBundle("sftp");
        if (sftpBundle != null) {
            CommonUtils.setText(sftpHost, sftpBundle.getString("host"));
            CommonUtils.setText(sftpPort, sftpBundle.getString("port"));
            CommonUtils.setText(sftpPath, sftpBundle.getString("path", "/"));
            CommonUtils.setText(sftpUsername, sftpBundle.getString("username"));
            CommonUtils.setText(sftpPassword, sftpBundle.getString("password"));
            sftpHostKey = SftpHelper.parseHostKey(sftpBundle.getString("hostKey", ""));
        }

        Bundle smbBundle = bundle.getBundle("smb");
        if (smbBundle != null) {
            CommonUtils.setText(smbHost, smbBundle.getString("host"));
            smbAnonymous.setChecked(smbBundle.getBoolean("anonymous"));
            CommonUtils.setText(smbUsername, smbBundle.getString("username"));
            CommonUtils.setText(smbPassword, smbBundle.getString("password"));
            CommonUtils.setText(smbDomain, smbBundle.getString("domain"));
            CommonUtils.setText(smbShare, smbBundle.getString("shareName"));
            CommonUtils.setText(smbPath, smbBundle.getString("path", "/"));
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
            if (!isChecked || getActivity() == null) return;

            AskPermission.Listener listener = new AskPermission.Listener() {
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
            };
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                listener.permissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                return;
            }

            AskPermission.ask(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE, listener);
        });

        encryptionContainer = layout.findViewById(R.id.editProfile_dd_encryption);
        encryptionEnabled = encryptionContainer.findViewById(R.id.editProfile_ddEncryption_enabled);
        certificate = encryptionContainer.findViewById(R.id.editProfile_ddEncryption_certificate);
        certificate.attachActivity(this);
        encryptionEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> certificate.setVisibility(isChecked ? View.VISIBLE : View.GONE));

        container = layout.findViewById(R.id.editProfile_dd_container);
        typePick = layout.findViewById(R.id.editProfile_dd_type);
        typePick.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;

            switch (checkedId) {
                default:
                case R.id.editProfile_ddType_web:
                    webContainer.setVisibility(View.VISIBLE);
                    ftpContainer.setVisibility(View.GONE);
                    sftpContainer.setVisibility(View.GONE);
                    smbContainer.setVisibility(View.GONE);

                    encryptionContainer.setVisibility(View.VISIBLE);
                    break;
                case R.id.editProfile_ddType_ftp:
                    webContainer.setVisibility(View.GONE);
                    ftpContainer.setVisibility(View.VISIBLE);
                    sftpContainer.setVisibility(View.GONE);
                    smbContainer.setVisibility(View.GONE);

                    encryptionContainer.setVisibility(View.VISIBLE);
                    break;
                case R.id.editProfile_ddType_sftp:
                    webContainer.setVisibility(View.GONE);
                    ftpContainer.setVisibility(View.GONE);
                    sftpContainer.setVisibility(View.VISIBLE);
                    smbContainer.setVisibility(View.GONE);

                    encryptionContainer.setVisibility(View.GONE);
                    break;
                case R.id.editProfile_ddType_samba:
                    webContainer.setVisibility(View.GONE);
                    ftpContainer.setVisibility(View.GONE);
                    sftpContainer.setVisibility(View.GONE);
                    smbContainer.setVisibility(View.VISIBLE);

                    encryptionContainer.setVisibility(View.GONE);
                    break;
            }
        });

        webContainer = layout.findViewById(R.id.editProfile_ddWeb_container);
        ftpContainer = layout.findViewById(R.id.editProfile_ddFtp_container);
        sftpContainer = layout.findViewById(R.id.editProfile_ddSftp_container);
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
        //endregion

        //region FTP
        ftpHost = ftpContainer.findViewById(R.id.editProfile_ddFtp_host);
        CommonUtils.clearErrorOnEdit(ftpHost);
        ftpPort = ftpContainer.findViewById(R.id.editProfile_ddFtp_port);
        CommonUtils.clearErrorOnEdit(ftpPort);
        ftpPath = ftpContainer.findViewById(R.id.editProfile_ddFtp_path);
        CommonUtils.clearErrorOnEdit(ftpPath);
        ftpUsername = ftpContainer.findViewById(R.id.editProfile_ddFtp_username);
        CommonUtils.clearErrorOnEdit(ftpUsername);
        ftpPassword = ftpContainer.findViewById(R.id.editProfile_ddFtp_password);
        CommonUtils.clearErrorOnEdit(ftpPassword);
        //endregion

        //region SFTP
        sftpHost = sftpContainer.findViewById(R.id.editProfile_ddSftp_host);
        CommonUtils.clearErrorOnEdit(sftpHost);
        sftpPort = sftpContainer.findViewById(R.id.editProfile_ddSftp_port);
        CommonUtils.clearErrorOnEdit(sftpPort);
        sftpPath = sftpContainer.findViewById(R.id.editProfile_ddSftp_path);
        CommonUtils.clearErrorOnEdit(sftpPath);
        sftpUsername = sftpContainer.findViewById(R.id.editProfile_ddSftp_username);
        CommonUtils.clearErrorOnEdit(sftpUsername);
        sftpPassword = sftpContainer.findViewById(R.id.editProfile_ddSftp_password);
        CommonUtils.clearErrorOnEdit(sftpPassword);
        sftpVerify = sftpContainer.findViewById(R.id.editProfile_ddSftp_verify);
        sftpVerify.setOnClickListener(v -> {
            MultiProfile.DirectDownload.Sftp sftp = null;
            try {
                Fields fields = validateStateAndCreateFields(save());
                if (fields.dd != null) sftp = fields.dd.sftp;
            } catch (InvalidFieldException ex) {
                if (ex.where == Where.DIRECT_DOWNLOAD)
                    onFieldError(ex.fieldId, getString(ex.reasonRes));
            }

            if (getActivity() == null || sftp == null)
                return;

            sftpVerify.setEnabled(false);
            SftpHelper.firstConnection(requireActivity(), sftp, new SftpHelper.FirstConnectionListener() {
                @Override
                public void onDone(@NonNull HostKey hostKey) {
                    sftpHostKey = hostKey;
                    sftpVerify.setEnabled(true);

                    showToast(Toaster.build().message(R.string.connectionVerified));
                }

                @Override
                public void onFailed(@NonNull JSchException ex) {
                    sftpHostKey = null;
                    sftpVerify.setEnabled(true);

                    showToast(Toaster.build().message(R.string.failedVerifyingConnection, ex.getMessage()));
                }
            });
        });
        //endregion

        //region Samba
        smbHost = smbContainer.findViewById(R.id.editProfile_ddSamba_host);
        CommonUtils.clearErrorOnEdit(smbHost);
        smbAuth = smbContainer.findViewById(R.id.editProfile_ddSamba_auth);
        smbAnonymous = smbContainer.findViewById(R.id.editProfile_ddSamba_anonymous);
        smbAnonymous.setOnCheckedChangeListener((buttonView, isChecked) -> smbAuth.setVisibility(isChecked ? View.GONE : View.VISIBLE));
        smbUsername = smbContainer.findViewById(R.id.editProfile_ddSamba_username);
        CommonUtils.clearErrorOnEdit(smbUsername);
        smbPassword = smbContainer.findViewById(R.id.editProfile_ddSamba_password);
        CommonUtils.clearErrorOnEdit(smbPassword);
        smbDomain = smbContainer.findViewById(R.id.editProfile_ddSamba_domain);
        CommonUtils.clearErrorOnEdit(smbDomain);
        smbShare = smbContainer.findViewById(R.id.editProfile_ddSamba_share);
        CommonUtils.clearErrorOnEdit(smbShare);
        smbPath = smbContainer.findViewById(R.id.editProfile_ddSamba_path);
        CommonUtils.clearErrorOnEdit(smbPath);
        //endregion

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