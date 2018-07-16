package com.gianlu.aria2app.Activities.AddDownload;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Objects;

public abstract class AddBase64Bundle extends AddDownloadBundle implements Serializable {
    public final String base64;
    public final String filename;
    private final String fileUri;

    public AddBase64Bundle(@NonNull String base64, @NonNull String filename, @NonNull Uri fileUri, @Nullable Integer position, @Nullable HashMap<String, String> options) {
        super(position, options);
        this.base64 = base64;
        this.filename = filename;
        this.fileUri = fileUri.toString();
    }

    AddBase64Bundle(@NonNull Context context, @NonNull Uri fileUri, @Nullable Integer position, @Nullable HashMap<String, String> options) throws CannotReadException {
        super(position, options);
        this.base64 = readBase64(context, fileUri);
        this.filename = extractFilename(context, fileUri);
        this.fileUri = fileUri.toString();
    }

    @NonNull
    public static String readBase64(@NonNull Context context, @NonNull Uri uri) throws CannotReadException {
        try (InputStream in = context.getContentResolver().openInputStream(uri); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (in == null) throw new CannotReadException(new NullPointerException("in is null!"));

            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
            return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
        } catch (IOException | SecurityException | OutOfMemoryError ex) {
            System.gc();
            throw new CannotReadException(ex);
        }
    }

    @NonNull
    public static String extractFilename(@NonNull Context context, @NonNull Uri uri) throws CannotReadException {
        String name;
        long size;
        if (Objects.equals(uri.getScheme(), "file")) {
            File file = new File(uri.getPath());
            name = file.getName();
            size = file.length();
        } else {
            try (Cursor cursor = context.getContentResolver().query(uri, new String[]{MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.SIZE}, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst() && cursor.getColumnCount() > 0) {
                    name = cursor.getString(0);
                    size = cursor.getLong(1);
                } else {
                    throw new CannotReadException("Failed query: " + cursor);
                }
            } catch (Exception ex) {
                throw new CannotReadException(ex);
            }
        }

        if (size > 26214400 /* 25MB */)
            throw new CannotReadException("File is too big: " + size);

        return name;
    }

    @NonNull
    public Uri fileUri() {
        return Uri.parse(fileUri);
    }


}
