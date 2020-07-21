package com.gianlu.aria2app.activities.editprofile;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.aria2app.api.CertUtils;
import com.gianlu.aria2app.profiles.MultiProfile;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.misc.SuperTextView;
import com.gianlu.commonutils.permissions.AskPermission;
import com.gianlu.commonutils.ui.Toaster;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;

import javax.security.auth.x500.X500Principal;

public class CertificateInputView extends LinearLayout {
    public static final int CODE_PICK_CERT = 13;
    private static final String TAG = CertificateInputView.class.getSimpleName();
    private final LinearLayout detailsContainer;
    private final CheckBox hostnameVerifier;
    private final SuperTextView detailsVersion;
    private final SuperTextView detailsSerialNumber;
    private final SuperTextView detailsSigAlgName;
    private final SuperTextView detailsSigAlgOid;
    private final SuperTextView detailsIssuerName;
    private final SuperTextView detailsIssuerAns;
    private final SuperTextView detailsSubjectAns;
    private final SuperTextView detailsSubjectName;
    private X509Certificate lastLoadedCertificate;
    private ActivityProvider activityProvider;

    public CertificateInputView(Context context) {
        this(context, null, 0);
    }

    public CertificateInputView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CertificateInputView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        View.inflate(context, R.layout.view_certificate_input, this);

        detailsContainer = findViewById(R.id.certificateInputView_certificateDetailsContainer);
        detailsSigAlgName = findViewById(R.id.certificateInputView_certificateDetails_sigAlgName);
        detailsSigAlgOid = findViewById(R.id.certificateInputView_certificateDetails_sigAlgOid);
        detailsIssuerName = findViewById(R.id.certificateInputView_certificateDetails_issuerName);
        detailsIssuerAns = findViewById(R.id.certificateInputView_certificateDetails_issuerAns);
        detailsSubjectName = findViewById(R.id.certificateInputView_certificateDetails_subjectName);
        detailsSubjectAns = findViewById(R.id.certificateInputView_certificateDetails_subjectAns);
        detailsVersion = findViewById(R.id.certificateInputView_certificateDetails_version);
        detailsSerialNumber = findViewById(R.id.certificateInputView_certificateDetails_serialNumber);
        hostnameVerifier = findViewById(R.id.certificateInputView_hostnameVerifier);

        ImageButton removeCertificateFile = findViewById(R.id.certificateInputView_removeCertificateFile);
        removeCertificateFile.setOnClickListener(v -> loadCertificateUri(null));

        ImageButton pickCertificateFile = findViewById(R.id.certificateInputView_pickCertificateFile);
        pickCertificateFile.setOnClickListener(v -> showPicker());
    }

    @NonNull
    public static Bundle stateFromDirectDownload(@NonNull MultiProfile.DirectDownload dd) {
        if (!dd.serverSsl) throw new IllegalArgumentException();

        Bundle bundle = new Bundle();
        bundle.putBoolean("hostnameVerifier", dd.hostnameVerifier);
        bundle.putSerializable("certificate", dd.certificate);
        return bundle;
    }

    @NonNull
    public static Bundle stateFromProfile(@NonNull MultiProfile.UserProfile profile) {
        if (!profile.serverSsl) throw new IllegalArgumentException();

        Bundle bundle = new Bundle();
        bundle.putBoolean("hostnameVerifier", profile.hostnameVerifier);
        bundle.putSerializable("certificate", profile.certificate);
        return bundle;
    }

    void attachActivity(@NonNull ActivityProvider activityProvider) {
        this.activityProvider = activityProvider;
    }

    void detachActivity() {
        this.activityProvider = null;
    }

    void showPicker() {
        if (activityProvider == null) return;

        final FragmentActivity activity = activityProvider.getActivity();
        if (activity == null) return;

        AskPermission.ask(activity, Manifest.permission.READ_EXTERNAL_STORAGE, new AskPermission.Listener() {
            @Override
            public void permissionGranted(@NonNull String permission) {
                if (activityProvider instanceof Fragment) {
                    try {
                        ((Fragment) activityProvider).startActivityForResult(
                                Intent.createChooser(new Intent(Intent.ACTION_GET_CONTENT).setType("*/*")
                                        .addCategory(Intent.CATEGORY_OPENABLE), "Select the certificate"), CODE_PICK_CERT);
                    } catch (ActivityNotFoundException ignored) {
                    }
                }
            }

            @Override
            public void permissionDenied(@NonNull String permission) {
                Toaster.with(activity).message(R.string.readPermissionDenied).show();
            }

            @Override
            public void askRationale(@NonNull AlertDialog.Builder builder) {
                builder.setTitle(R.string.readExternalStorageRequest_title)
                        .setMessage(R.string.readExternalStorageRequest_certMessage);
            }
        });
    }

    void showCertificateDetails(@NonNull X509Certificate certificate) {
        lastLoadedCertificate = certificate;

        detailsContainer.setVisibility(View.VISIBLE);

        detailsVersion.setHtml(R.string.versionLabel, String.valueOf(certificate.getVersion()));
        detailsSerialNumber.setHtml(R.string.serialNumber, Utils.toHexString(certificate.getSerialNumber().toByteArray()));

        detailsSigAlgName.setHtml(R.string.name, certificate.getSigAlgName());
        detailsSigAlgOid.setHtml(R.string.oid, certificate.getSigAlgOID());

        detailsIssuerName.setHtml(R.string.name, certificate.getIssuerX500Principal().getName(X500Principal.RFC1779));

        try {
            Collection<List<?>> ians = certificate.getIssuerAlternativeNames();
            if (ians == null) {
                detailsIssuerAns.setVisibility(View.GONE);
            } else {
                detailsIssuerAns.setVisibility(View.VISIBLE);
                List<CertUtils.GeneralName> names = CertUtils.parseGeneralNames(ians);
                detailsIssuerAns.setHtml(R.string.alternativeNames, CommonUtils.join(names, ", "));
            }
        } catch (CertificateParsingException ex) {
            detailsIssuerAns.setVisibility(View.GONE);
            Log.e(TAG, "Failed parsing certificate.", ex);
        }

        detailsSubjectName.setHtml(R.string.name, certificate.getSubjectX500Principal().getName(X500Principal.RFC1779));

        try {
            Collection<List<?>> sans = certificate.getSubjectAlternativeNames();
            if (sans == null) {
                detailsSubjectAns.setVisibility(View.GONE);
            } else {
                detailsSubjectAns.setVisibility(View.VISIBLE);
                List<CertUtils.GeneralName> names = CertUtils.parseGeneralNames(sans);
                detailsSubjectAns.setHtml(R.string.alternativeNames, CommonUtils.join(names, ", "));
            }
        } catch (CertificateParsingException ex) {
            detailsSubjectAns.setVisibility(View.GONE);
            Log.e(TAG, "Failed parsing certificate.", ex);
        }
    }

    void loadCertificateUri(@Nullable Uri path) {
        if (path == null) {
            lastLoadedCertificate = null;
            detailsContainer.setVisibility(View.GONE);
            return;
        }

        Context context = getContext();
        if (context == null) return;

        X509Certificate certificate;
        try {
            InputStream in = context.getContentResolver().openInputStream(path);
            if (in != null) {
                certificate = CertUtils.loadCertificateFromStream(in);
            } else {
                Toaster.with(context).message(R.string.invalidCertificate).show();
                return;
            }
        } catch (FileNotFoundException | CertificateException ex) {
            Log.e(TAG, "Failed reading certificate.", ex);
            Toaster.with(context).message(R.string.invalidCertificate).show();
            return;
        }

        showCertificateDetails(certificate);
    }

    @NonNull
    public Bundle saveState() {
        Bundle bundle = new Bundle();
        bundle.putBoolean("hostnameVerifier", hostnameVerifier.isChecked());
        bundle.putSerializable("certificate", lastLoadedCertificate);
        return bundle;
    }

    public void restore(@Nullable Bundle bundle, boolean show) {
        if (bundle == null) {
            hostnameVerifier.setChecked(false);
            loadCertificateUri(null);
        } else {
            hostnameVerifier.setChecked(bundle.getBoolean("hostnameVerifier", false));
            lastLoadedCertificate = (X509Certificate) bundle.getSerializable("certificate");
            if (lastLoadedCertificate != null && show)
                showCertificateDetails(lastLoadedCertificate);
        }
    }

    public interface ActivityProvider {
        @Nullable
        FragmentActivity getActivity();
    }
}
