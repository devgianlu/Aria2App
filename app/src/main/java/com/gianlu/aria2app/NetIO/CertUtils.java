package com.gianlu.aria2app.NetIO;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;

import com.gianlu.aria2app.BuildConfig;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class CertUtils {
    @Nullable
    public static X509Certificate decodeCertificate(@Nullable String base64) {
        if (base64 == null) return null;

        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(Base64.decode(base64, Base64.NO_WRAP)));
        } catch (CertificateException ex) {
            if (BuildConfig.DEBUG) ex.printStackTrace();
            return null;
        }
    }

    @Nullable
    public static String encodeCertificate(@Nullable X509Certificate certificate) {
        if (certificate == null) return null;

        try {
            return Base64.encodeToString(certificate.getEncoded(), Base64.NO_WRAP);
        } catch (CertificateEncodingException ex) {
            if (BuildConfig.DEBUG) ex.printStackTrace();
            return null;
        }
    }

    @NonNull
    public static X509Certificate loadCertificateFromStream(@NonNull InputStream in) throws CertificateException {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) factory.generateCertificate(in);
    }

    @Nullable
    public static X509Certificate loadCertificateFromFile(@Nullable String path) {
        if (path == null) return null;

        try {
            return loadCertificateFromStream(new FileInputStream(path));
        } catch (FileNotFoundException | CertificateException ex) {
            if (BuildConfig.DEBUG) ex.printStackTrace();
            return null;
        }
    }
}
