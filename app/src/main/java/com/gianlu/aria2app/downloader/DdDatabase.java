package com.gianlu.aria2app.downloader;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

public final class DdDatabase extends SQLiteOpenHelper {
    DdDatabase(@Nullable Context context) {
        super(context, "direct_download", null, 1);
    }

    @Override
    public void onCreate(@NotNull SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS downloads (id INTEGER PRIMARY KEY UNIQUE, type TEXT NOT NULL, remotePath TEXT NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public int addDownload(@NonNull Type type, @NonNull String remotePath) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues(2);
            values.put("type", type.name());
            values.put("remotePath", remotePath);
            long id = db.insert("downloads", null, values);
            if (id != -1) db.setTransactionSuccessful();

            return (int) id;
        } finally {
            db.endTransaction();
        }
    }

    @NonNull
    public List<Download> getDownloads(@NonNull Type type) {
        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.query("downloads", null, "type=?", new String[]{type.name()}, null, null, null)) {
            List<Download> list = new LinkedList<>();
            while (cursor.moveToNext()) list.add(new Download(cursor));
            return list;
        } finally {
            db.endTransaction();
        }
    }

    public void removeDownload(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("downloads", "id=?", new String[]{String.valueOf(id)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public enum Type {
        FTP, SFTP, SMB
    }

    public static class Download {
        public final String path;

        Download(@NonNull Cursor cursor) {
            path = cursor.getString(cursor.getColumnIndex("remotePath"));
        }
    }
}
