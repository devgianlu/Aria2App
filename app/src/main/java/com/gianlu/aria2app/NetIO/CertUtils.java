package com.gianlu.aria2app.NetIO;

import android.util.Base64;

import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.Logging;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class CertUtils {
    @Nullable
    public static X509Certificate decodeCertificate(@NonNull String base64) {
        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(Base64.decode(base64, Base64.NO_WRAP)));
        } catch (CertificateException ex) {
            Logging.log(ex);
            return null;
        }
    }

    public static List<GeneralName> parseGeneralNames(Collection<List<?>> generalNames) {
        List<GeneralName> generalNamesParsed = new ArrayList<>();
        for (List<?> name : generalNames) generalNamesParsed.add(new GeneralName(name));
        return generalNamesParsed;
    }

    @Nullable
    public static String encodeCertificate(@NonNull X509Certificate certificate) {
        try {
            return Base64.encodeToString(certificate.getEncoded(), Base64.NO_WRAP);
        } catch (CertificateEncodingException ex) {
            Logging.log(ex);
            return null;
        }
    }

    @NonNull
    public static X509Certificate loadCertificateFromStream(@NonNull InputStream in) throws CertificateException {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) factory.generateCertificate(in);
    }

    @Nullable
    public static X509Certificate loadCertificateFromFile(@NonNull String path) {
        try {
            return loadCertificateFromStream(new FileInputStream(path));
        } catch (FileNotFoundException | CertificateException ex) {
            Logging.log(ex);
            return null;
        }
    }

    public static class GeneralName {
        public final Type type;
        public final String val;

        public GeneralName(List<?> pair) {
            type = Type.parse((Integer) pair.get(0));

            Object valObj = pair.get(1);
            Class<?> clazz = valObj.getClass();
            if (clazz.isArray() && clazz.getComponentType() == byte.class) {
                val = Utils.toHexString((byte[]) valObj);
            } else {
                val = valObj.toString();
            }
        }

        @NonNull
        @Override
        public String toString() {
            return val + " (" + type.name() + ")";
        }

        public enum Type {
            OTHER_NAME,
            RFC822_NAME,
            DNS_NAME,
            X400_ADDRESS,
            DIRECTORY_NAME,
            EDIT_PARTY_NAME,
            URI,
            IP_ADDRESS,
            REGISTERED_ID;

            public static Type parse(Integer i) {
                switch (i) {
                    default:
                    case 0:
                        return OTHER_NAME;
                    case 1:
                        return RFC822_NAME;
                    case 2:
                        return DNS_NAME;
                    case 3:
                        return X400_ADDRESS;
                    case 4:
                        return DIRECTORY_NAME;
                    case 5:
                        return EDIT_PARTY_NAME;
                    case 6:
                        return URI;
                    case 7:
                        return IP_ADDRESS;
                    case 8:
                        return REGISTERED_ID;
                }
            }
        }
    }
}
