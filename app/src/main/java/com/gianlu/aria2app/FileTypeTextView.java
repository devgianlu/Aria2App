package com.gianlu.aria2app;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;

import com.gianlu.commonutils.FontsManager;

import java.util.Objects;

public class FileTypeTextView extends AppCompatTextView {
    public FileTypeTextView(Context context) {
        this(context, null, 0);
    }

    public FileTypeTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FileTypeTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setTypeface(FontsManager.get().get(context, FontsManager.ROBOTO_BLACK));
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(this, 8, 36, 1, TypedValue.COMPLEX_UNIT_SP);
        setGravity(Gravity.CENTER);

        if (isInEditMode()) setExtension("XML");
    }

    public void setFilename(@NonNull String filename) {
        String[] split = filename.split("\\.");
        if (split.length > 0) setExtension(split[split.length - 1]);
        else setExtension(null);
    }

    public void setExtension(@Nullable String ext) {
        if (Objects.equals(getText().toString(), ext)) return;
        if (ext == null) {
            ext = "???";
        } else {
            ext = ext.trim();
            if (ext.length() > 4) ext = ext.substring(0, 4);
        }

        setText(ext.toUpperCase());
    }
}
